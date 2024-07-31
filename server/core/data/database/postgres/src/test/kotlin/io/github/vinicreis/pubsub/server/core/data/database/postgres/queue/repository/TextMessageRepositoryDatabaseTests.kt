package io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.TextMessages
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.asDomainTextMessage
import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.fixture.DatabaseFixture
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.QueueRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.TextMessageRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import io.github.vinicreis.pubsub.server.core.test.extension.asTextMessage
import io.github.vinicreis.pubsub.server.core.test.extension.randomSlice
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.TextMessageFixture
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.fail
import java.util.logging.Logger
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@TestMethodOrder(MethodOrderer.DisplayName::class)
class TextMessageRepositoryDatabaseTests {
    @Test
    fun `01 - Should fail to add an invalid message to a valid queue`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)

        if (queueRepository.add(queue) !is QueueRepository.Result.Add.Success) {
            fail("Failed to add queue to repository")
        }

        val message = "    ".asTextMessage

        when (val result = sut.add(queue, listOf(message))) {
            TextMessageRepository.Result.Add.Success -> fail("Should not add a message to a non-existent queue")
            is TextMessageRepository.Result.Add.Error -> {
                assertInstanceOf(IllegalArgumentException::class.java, result.e)
                assertNotNull(result.e.message)
                assertTrue(result.e.message!!.contains("content"))
                assertTrue(result.e.message!!.contains("not be blank"))
            }
        }
    }

    @Test
    fun `02 - Should add a valid message to a valid queue successfully`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)
        val message = TextMessageFixture.any().asTextMessage

        when (val result = sut.add(queue, listOf(message))) {
            is TextMessageRepository.Result.Add.Error -> fail("Should not fail to add message", result.e)
            TextMessageRepository.Result.Add.Success -> Unit
        }

        when (val result = sut.subscribe(queue)) {
            is TextMessageRepository.Result.Subscribe.Error -> fail("Should not fail to poll message")
            is TextMessageRepository.Result.Subscribe.Success -> result.messages.first().also { receivedMessage ->
                assertEquals(message, receivedMessage)
            }
        }
    }

    @Test
    fun `03 - Should add multiple messages to a valid queue successfully`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)
        val messages = TextMessageFixture.EXAMPLES.randomSlice(5).map { it.asTextMessage }

        when (val result = sut.add(queue, messages)) {
            is TextMessageRepository.Result.Add.Error -> fail(
                "Should not add a message to a non-existent queue",
                result.e
            )

            TextMessageRepository.Result.Add.Success -> Unit
        }
    }

    @Test
    fun `04 - Should fail when message does not match constraints`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)
        val tooLongMessage = "aaaaa".repeat(10_000)

        when (val result = sut.add(queue, listOf(tooLongMessage.asTextMessage))) {
            is TextMessageRepository.Result.Add.Success -> fail("Should not poll a message longer than limit")
            is TextMessageRepository.Result.Add.Error -> {
                assertInstanceOf(IllegalArgumentException::class.java, result.e)
                assertNotNull(result.e.message)
                assertTrue(result.e.message!!.contains("can not be longer than"))
                assertTrue(result.e.message!!.contains(TextMessage.MAX_CONTENT_LENGTH.toString()))
            }
        }

        val blankMessage = "   "

        when (val result = sut.add(queue, listOf(blankMessage.asTextMessage))) {
            is TextMessageRepository.Result.Add.Success -> fail("Should not poll a message longer than limit")
            is TextMessageRepository.Result.Add.Error -> {
                assertNotNull(result.e.message)
                assertTrue(result.e.message!!.contains("can not be blank"))
            }
        }
    }

    @Test
    fun `05 - Should add message on database if no subscriber exists for queue`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)
        val message = TextMessageFixture.any().asTextMessage

        when (val result = sut.add(queue, listOf(message))) {
            is TextMessageRepository.Result.Add.Error -> fail("Should not fail to add message", result.e)
            TextMessageRepository.Result.Add.Success -> Unit
        }

        val foundMessages = transaction {
            TextMessages.selectAll().where { TextMessages.id eq message.id }.map { it.asDomainTextMessage }
        }

        assertEquals(1, foundTextMessages.size)
        assertEquals(message.id, foundTextMessages.last().id)
        assertEquals(message.content, foundTextMessages.last().content)
    }

    @Test
    fun `06 - Should subscribe to a valid queue successfully`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)

        when (sut.subscribe(queue)) {
            is TextMessageRepository.Result.Subscribe.Error -> fail("Should not fail by generic error")
            is TextMessageRepository.Result.Subscribe.Success -> Unit
        }
    }

    @Test
    fun `07 - Should receive messages from a subscribed queue in order`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)
        val receivedTextMessages = mutableListOf<TextMessage>()
        val pendingMessages = transaction {
            TextMessages.selectAll()
                .where { TextMessages.queueId eq queue.id }
                .orderBy(TextMessages.createdAt, SortOrder.ASC)
                .map { it.asDomainTextMessage }
        }

        when (val result = sut.subscribe(queue)) {
            is TextMessageRepository.Result.Subscribe.Error -> fail("Should fail to subscribe to queue $queue")
            is TextMessageRepository.Result.Subscribe.Success -> {
                backgroundScope.launch {
                    result.messages
                        .takeWhile { receivedTextTextMessages.size < pendingTextMessages.size }
                        .toList(receivedTextMessages)
                }
            }
        }

        assertEquals(pendingMessages, receivedTextMessages)
    }

    @Test
    fun `08 - Should delete message from database after sending`() {
        transaction { TextMessages.selectAll().count() }.also {
            assertEquals(0, it)
        }
    }

    @Test
    fun `09 - Should remove existing queue successfully closing all subscribers`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)
        val subscribers: List<Flow<TextMessage>> = buildList {
            repeat(Random.nextInt(400, 500)) {
                when (val result = sut.subscribe(queue)) {
                    is TextMessageRepository.Result.Subscribe.Error -> fail("Should not fail by generic error")
                    is TextMessageRepository.Result.Subscribe.Success -> add(result.messages)
                }
            }
        }

        val jobs = subscribers.map { subscriber ->
            backgroundScope.launch { subscriber.collect { } }
        }

        when (sut.remove(queue)) {
            is TextMessageRepository.Result.Remove.Error -> fail("Should not fail by generic error")
            is TextMessageRepository.Result.Remove.Success -> Unit
        }

        assertTrue { jobs.all { it.isCompleted } }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()
        private lateinit var queueRepository: QueueRepository
        private lateinit var sut: TextMessageRepositoryDatabase
        private val validQueueId = QueueFixture.id()

        @JvmStatic
        @BeforeAll
        fun setup() {
            DatabaseFixture.up()

            queueRepository = QueueRepositoryDatabase()
            sut = TextMessageRepositoryDatabase(
                coroutineContext = testDispatcher,
                logger = Logger.getLogger(TextMessageRepositoryDatabaseTests::class.simpleName)
            )
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            DatabaseFixture.down()
        }
    }
}

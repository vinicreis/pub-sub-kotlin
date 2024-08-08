package io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.asDomainTextMessage
import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.fixture.DatabaseFixture
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.EventRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.QueueRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.TextMessageRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.Events
import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.Queues
import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.TextMessages
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueAddedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueRemovedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.test.extension.asTextMessage
import io.github.vinicreis.pubsub.server.core.test.extension.randomSlice
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.TextMessageFixture
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestMethodOrder(MethodOrderer.DisplayName::class)
class TextMessageRepositoryDatabaseTests {
    @Test
    fun `01 - Should fail to add an invalid message to a valid queue`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)

        if (queueRepository.add(queue) !is QueueRepository.Result.Add.Success) {
            fail("Failed to add queue to repository")
        }

        val message = "    ".asTextMessage(queue)

        when (val result = sut.add(queue, listOf(message))) {
            TextMessageRepository.Result.Add.Success -> fail("Should not add a message to a non-existent queue")
            is TextMessageRepository.Result.Add.Error -> {
                assertInstanceOf(IllegalArgumentException::class.java, result.e)
                assertNotNull(result.e.message)
                assertTrue(result.e.message!!.contains("content"))
                assertTrue(result.e.message!!.contains("not be blank"))
            }
        }

        when (val event = eventsRepository.consume(queue.id)) {
            null,
            is TextMessageReceivedEvent,
            is QueueRemovedEvent -> fail("Should not fail to poll message")
            is QueueAddedEvent -> {
                assertEquals(queue, event.queue)
                transaction { Events.selectAll().where { Events.id eq event.id }.count() }.also {
                    assertEquals(0, it)
                }
            }
        }
    }

    @Test
    fun `02 - Should add a valid message to a valid queue successfully`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)
        val message = TextMessageFixture.any().asTextMessage(queue)

        when (val result = sut.add(queue, listOf(message))) {
            is TextMessageRepository.Result.Add.Error -> fail("Should not fail to add message", result.e)
            TextMessageRepository.Result.Add.Success -> Unit
        }

        when (val event = eventsRepository.consume(queue.id)) {
            null,
            is QueueAddedEvent,
            is QueueRemovedEvent -> fail("Should not fail to poll message")
            is TextMessageReceivedEvent -> {
                assertEquals(message.content, event.textMessage.content)
                transaction { Events.selectAll().where { Events.id eq event.id }.count() }.also {
                    assertEquals(0, it)
                }
                transaction { TextMessages.selectAll().where { TextMessages.id eq event.textMessage.id }.count() }.also {
                    assertEquals(0, it)
                }
            }
        }
    }

    @Test
    fun `03 - Should add multiple messages to a valid queue successfully`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)
        val messages = TextMessageFixture.EXAMPLES.randomSlice(5).map { it.asTextMessage(queue) }

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

        when (val result = sut.add(queue, listOf(tooLongMessage.asTextMessage(queue)))) {
            is TextMessageRepository.Result.Add.Success -> fail("Should not poll a message longer than limit")
            is TextMessageRepository.Result.Add.Error -> {
                assertInstanceOf(IllegalArgumentException::class.java, result.e)
                assertNotNull(result.e.message)
                assertTrue(result.e.message!!.contains("can not be longer than"))
                assertTrue(result.e.message!!.contains(TextMessage.MAX_CONTENT_LENGTH.toString()))
            }
        }

        val blankMessage = "   "

        when (val result = sut.add(queue, listOf(blankMessage.asTextMessage(queue)))) {
            is TextMessageRepository.Result.Add.Success -> fail("Should not poll a message longer than limit")
            is TextMessageRepository.Result.Add.Error -> {
                assertNotNull(result.e.message)
                assertTrue(result.e.message!!.contains("can not be blank"))
            }
        }
    }

    @Test
    fun `05 - Should subscribe to a valid queue successfully`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)

        when (val event = eventsRepository.consume(queue.id)) {
            null,
            is QueueAddedEvent,
            is QueueRemovedEvent -> fail("Should not fail to poll message")
            is TextMessageReceivedEvent -> {
                transaction { Events.selectAll().where { Events.id eq event.id }.count() }.also {
                    assertEquals(0, it)
                }
                transaction { TextMessages.selectAll().where { TextMessages.id eq event.textMessage.id }.count() }.also {
                    assertEquals(0, it)
                }
            }
        }
    }

    @Test
    fun `06 - Should receive messages from a subscribed queue in order`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(id = validQueueId)
        val receivedTextMessages = mutableListOf<TextMessage>()
        val pendingMessages = transaction {
            (TextMessages leftJoin Queues)
                .select(TextMessages.columns + Queues.columns)
                .where { TextMessages.queueId eq queue.id }
                .groupBy(TextMessages.id, Queues.id)
                .orderBy(TextMessages.createdAt, SortOrder.ASC)
                .map { it.asDomainTextMessage }
        }

        var lastEvent: TextMessageReceivedEvent?
        do {
            lastEvent = eventsRepository.consume(queue.id) as? TextMessageReceivedEvent
            lastEvent?.also { receivedTextMessages.add(it.textMessage) }
        } while (lastEvent != null)

        assertEquals(pendingMessages.map { it.id }, receivedTextMessages.map { it.id })
    }

    @Test
    fun `07 - Should delete message from database after sending`() {
        transaction { TextMessages.selectAll().count() }.also {
            assertEquals(0, it)
        }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()
        private lateinit var eventsRepository: EventsRepository
        private lateinit var queueRepository: QueueRepository
        private lateinit var sut: TextMessageRepositoryDatabase
        private val validQueueId = QueueFixture.id()

        @JvmStatic
        @BeforeAll
        fun setup() {
            DatabaseFixture.up()

            eventsRepository = EventRepositoryDatabase(testDispatcher)
            queueRepository = QueueRepositoryDatabase(testDispatcher, eventsRepository)
            sut = TextMessageRepositoryDatabase(
                coroutineContext = testDispatcher,
                eventsRepository = eventsRepository,
            )
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            DatabaseFixture.down()
        }
    }
}

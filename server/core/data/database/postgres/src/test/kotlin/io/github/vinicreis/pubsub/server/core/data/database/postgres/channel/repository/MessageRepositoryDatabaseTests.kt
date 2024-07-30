package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity.Messages
import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.fixture.DatabaseFixture
import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.mapper.asDomainMessage
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.core.test.extension.asMessage
import io.github.vinicreis.pubsub.server.core.test.extension.randomSlice
import io.github.vinicreis.pubsub.server.core.test.fixture.ChannelFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.MessageFixture
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
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
class MessageRepositoryDatabaseTests {
    @Test
    fun `01 - Should fail to add a message to a non-existent channel`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(code = "non-existent-channel")
        val message = MessageFixture.any().asMessage

        when (val result = sut.add(channel, message)) {
            is MessageRepository.Result.Add.Error -> fail("Should not fail with generic error", result.e)
            MessageRepository.Result.Add.Success -> fail("Should not add a message to a non-existent channel")
            MessageRepository.Result.Add.QueueNotFound -> Unit
        }
    }

    @Test
    fun `02 - Should fail to add an invalid message to a valid channel`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(id = validChannelId)

        if (channelRepository.add(channel) !is ChannelRepository.Result.Add.Success) {
            fail("Failed to add channel to repository")
        }

        val message = "    ".asMessage

        when (val result = sut.add(channel, message)) {
            MessageRepository.Result.Add.Success -> fail("Should not add a message to a non-existent channel")
            MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
            is MessageRepository.Result.Add.Error -> {
                assertInstanceOf(IllegalArgumentException::class.java, result.e)
                assertNotNull(result.e.message)
                assertTrue(result.e.message!!.contains("content"))
                assertTrue(result.e.message!!.contains("not be blank"))
            }
        }
    }

    @Test
    fun `03 - Should add a valid message to a valid channel successfully`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(id = validChannelId)
        val message = MessageFixture.any().asMessage

        when (val result = sut.add(channel, message)) {
            is MessageRepository.Result.Add.Error -> fail("Should not fail to add message", result.e)
            MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
            MessageRepository.Result.Add.Success -> Unit
        }

        when (val result = sut.subscribe(channel)) {
            MessageRepository.Result.Subscribe.QueueNotFound -> fail("Should not fail with queue not found")
            is MessageRepository.Result.Subscribe.Error -> fail("Should not fail to poll message")
            is MessageRepository.Result.Subscribe.Success -> result.messages.first().also { receivedMessage ->
                assertEquals(message, receivedMessage)
            }
        }
    }

    @Test
    fun `04 - Should add multiple messages to a valid channel successfully`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(id = validChannelId)
        val messages = MessageFixture.EXAMPLES.randomSlice(5).map { it.asMessage }

        when (val result = sut.addAll(channel, messages)) {
            is MessageRepository.Result.Add.Error -> fail(
                "Should not add a message to a non-existent channel",
                result.e
            )

            MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
            MessageRepository.Result.Add.Success -> Unit
        }
    }

    @Test
    fun `05 - Should fail when message does not match constraints`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(id = validChannelId)
        val tooLongMessage = "aaaaa".repeat(10_000)

        when (val result = sut.add(channel, tooLongMessage.asMessage)) {
            MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
            is MessageRepository.Result.Add.Success -> fail("Should not poll a message longer than limit")
            is MessageRepository.Result.Add.Error -> {
                assertInstanceOf(IllegalArgumentException::class.java, result.e)
                assertNotNull(result.e.message)
                assertTrue(result.e.message!!.contains("can not be longer than"))
                assertTrue(result.e.message!!.contains(Message.MAX_CONTENT_LENGTH.toString()))
            }
        }

        val blankMessage = "   "

        when (val result = sut.add(channel, blankMessage.asMessage)) {
            MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
            is MessageRepository.Result.Add.Success -> fail("Should not poll a message longer than limit")
            is MessageRepository.Result.Add.Error -> {
                assertNotNull(result.e.message)
                assertTrue(result.e.message!!.contains("can not be blank"))
            }
        }
    }

    @Test
    fun `06 - Should add message on database if no subscriber exists for channel`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(id = validChannelId)
        val message = MessageFixture.any().asMessage

        when (val result = sut.add(channel, message)) {
            is MessageRepository.Result.Add.Error -> fail("Should not fail to add message", result.e)
            MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
            MessageRepository.Result.Add.Success -> Unit
        }

        val foundMessages = transaction {
            Messages.selectAll().where { Messages.id eq message.id }.map { it.asDomainMessage }
        }

        assertEquals(1, foundMessages.size)
        assertEquals(message.id, foundMessages.last().id)
        assertEquals(message.content, foundMessages.last().content)
    }

    @Test
    fun `07 - Should fail to subscribe to a non-existent channel`() = runTest(testDispatcher) {
        val invalidChannel = ChannelFixture.instance()

        when (sut.subscribe(invalidChannel)) {
            is MessageRepository.Result.Subscribe.Error -> fail("Should not fail by generic error")
            is MessageRepository.Result.Subscribe.Success -> fail("Should not succeed to remove non-existing channel")
            MessageRepository.Result.Subscribe.QueueNotFound -> Unit
        }
    }

    @Test
    fun `08 - Should subscribe to a valid channel successfully`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(id = validChannelId)

        when (sut.subscribe(channel)) {
            is MessageRepository.Result.Subscribe.Error -> fail("Should not fail by generic error")
            MessageRepository.Result.Subscribe.QueueNotFound -> fail("Should find valid channel $validChannelId")
            is MessageRepository.Result.Subscribe.Success -> Unit
        }
    }

    @Test
    fun `09 - Should receive messages from a subscribed channel in order`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(id = validChannelId)
        val receivedMessages = mutableListOf<Message>()
        val pendingMessages = transaction {
            Messages.selectAll()
                .where { Messages.channel eq channel.id }
                .orderBy(Messages.createdAt, SortOrder.ASC)
                .map { it.asDomainMessage }
        }

        when (val result = sut.subscribe(channel)) {
            is MessageRepository.Result.Subscribe.Error -> fail("Should fail to subscribe to channel $channel")
            MessageRepository.Result.Subscribe.QueueNotFound -> fail("The channel $validChannelId is valid and should've been found")
            is MessageRepository.Result.Subscribe.Success -> {
                backgroundScope.launch {
                    result.messages
                        .takeWhile { receivedMessages.size < pendingMessages.size }
                        .toList(receivedMessages)
                }
            }
        }

        assertEquals(pendingMessages, receivedMessages)
    }

    @Test
    fun `10 - Should delete message from database after sending`() {
        transaction { Messages.selectAll().count() }.also {
            assertEquals(0, it)
        }
    }

    @Test
    fun `11 - Should fail to remove a non-existing channel`() = runTest(testDispatcher) {
        val invalidChannel = ChannelFixture.instance()

        when (sut.remove(invalidChannel)) {
            is MessageRepository.Result.Remove.Error -> fail("Should not fail by generic error")
            is MessageRepository.Result.Remove.Success -> fail("Should not succeed to remove non-existing channel")
            MessageRepository.Result.Remove.QueueNotFound -> Unit
        }
    }

    @Test
    fun `12 - Should remove existing channel successfully closing all subscribers`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(id = validChannelId)
        val subscribers: List<Flow<Message>> = buildList {
            repeat(Random.nextInt(400, 500)) {
                when (val result = sut.subscribe(channel)) {
                    is MessageRepository.Result.Subscribe.Error -> fail("Should not fail by generic error")
                    MessageRepository.Result.Subscribe.QueueNotFound -> fail("Should find valid channel $validChannelId")
                    is MessageRepository.Result.Subscribe.Success -> add(result.messages)
                }
            }
        }

        val jobs = subscribers.map { subscriber ->
            backgroundScope.launch { subscriber.collect { } }
        }

        when (sut.remove(channel)) {
            is MessageRepository.Result.Remove.Error -> fail("Should not fail by generic error")
            MessageRepository.Result.Remove.QueueNotFound -> fail("The channel $validChannelId is valid and should've been found")
            is MessageRepository.Result.Remove.Success -> Unit
        }

        assertTrue { jobs.all { it.isCompleted } }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()
        private lateinit var channelRepository: ChannelRepository
        private lateinit var sut: MessageRepositoryDatabase
        private val validChannelId = ChannelFixture.id()

        @JvmStatic
        @BeforeAll
        fun setup() {
            DatabaseFixture.up()

            channelRepository = ChannelRepositoryDatabase()
            sut = MessageRepositoryDatabase(
                channelRepository = channelRepository,
                coroutineContext = testDispatcher,
                logger = Logger.getLogger(MessageRepositoryDatabaseTests::class.simpleName)
            )
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            DatabaseFixture.down()
        }
    }
}

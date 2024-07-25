package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity.Messages
import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.fixture.DatabaseFixture
import io.github.vinicreis.pubsub.server.core.test.extension.asMessage
import io.github.vinicreis.pubsub.server.core.test.fixture.ChannelFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.MessageFixture
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.fail
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@TestMethodOrder(MethodOrderer.DisplayName::class)
class MessageRepositoryDatabaseTests {
    @Nested
    @DisplayName("1 - Add operation tests")
    @TestMethodOrder(MethodOrderer.MethodName::class)
    inner class AddTests {
        @Test
        fun `1 - Should fail to add a message to a non-existent channel`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance(code = "non-existent-channel")
            val message = MessageFixture.any().asMessage

            when (sut.add(channel, message)) {
                is MessageRepository.Result.Add.Error -> fail("Should not fail with generic error")
                MessageRepository.Result.Add.Success -> fail("Should not add a message to a non-existent channel")
                MessageRepository.Result.Add.QueueNotFound -> Unit
            }
        }

        @Test
        fun `2 - Should fail to add an invalid message to a valid channel`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance(code = "channel-1")

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
        fun `3 - Should add a valid message to a valid channel successfully`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance(code = "channel-1")

            if (channelRepository.add(channel) !is ChannelRepository.Result.Add.Success) {
                fail("Failed to add channel to repository")
            }

            val message = MessageFixture.any().asMessage

            when (val result = sut.add(channel, message)) {
                MessageRepository.Result.Add.Success -> fail("Should not add a message to a non-existent channel")
                MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
                is MessageRepository.Result.Add.Error -> {
                    assertInstanceOf(IllegalArgumentException::class.java, result.e)
                    assertNotNull(result.e.message)
                    assertTrue(result.e.message!!.contains("id"))
                    assertTrue(result.e.message!!.contains("not be blank"))
                }
            }

            when (val result = sut.poll(channel)) {
                MessageRepository.Result.Poll.QueueNotFound -> fail("Should not fail with queue not found")
                is MessageRepository.Result.Poll.Error -> fail("Should not add a message to a non-existent channel")
                is MessageRepository.Result.Poll.Success -> assertEquals(message, result.message)
            }
        }

        @Test
        fun `4 - Should add multiple messages to a valid channel successfully`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance(code = "channel-1")
            val messages = MessageFixture.EXAMPLES.take(5).map { it.asMessage }

            when (val result = sut.addAll(channel, messages)) {
                MessageRepository.Result.Add.Success -> fail("Should not add a message to a non-existent channel")
                MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
                is MessageRepository.Result.Add.Error -> {
                    assertInstanceOf(IllegalArgumentException::class.java, result.e)
                    assertNotNull(result.e.message)
                    assertTrue(result.e.message!!.contains("id"))
                    assertTrue(result.e.message!!.contains("not be blank"))
                }
            }
        }

        @Test
        fun `5 - Should fail when message fails to be added on database`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance(code = "channel-1")
            val tooLongMessage = "aaaaa".repeat(10_000)

            when (val result = sut.add(channel, tooLongMessage.asMessage)) {
                MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
                is MessageRepository.Result.Add.Success -> fail("Should not poll a message longer than limit")
                is MessageRepository.Result.Add.Error -> {
                    assertNotNull(result.e.message)
                    assertEquals(GENERIC_ERROR_MESSAGE, result.e.message)
                }
            }
        }

        @Test
        fun `6 - Should add message on database if no subscriber exists for channel`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance(code = "channel-1")
            val tooLongMessage = "aaaaa".repeat(10_000)

            when (val result = sut.add(channel, tooLongMessage.asMessage)) {
                MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail with queue not found")
                is MessageRepository.Result.Add.Success -> fail("Should not poll a message longer than limit")
                is MessageRepository.Result.Add.Error -> {
                    assertNotNull(result.e.message)
                    assertEquals(GENERIC_ERROR_MESSAGE, result.e.message)
                }
            }
        }
    }

    @Nested
    @DisplayName("2 - Poll operation tests")
    @TestMethodOrder(MethodOrderer.MethodName::class)
    inner class PollTests {
        @Test
        fun `1 - Should poll a message from a valid channel successfully`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance(code = "channel-1")
            val messages = MessageFixture.EXAMPLES.take(5).map { it.asMessage }

            messages.forEach { message ->
                when (val result = sut.poll(channel)) {
                    MessageRepository.Result.Poll.QueueNotFound -> fail("Should not fail with queue not found")
                    is MessageRepository.Result.Poll.Error -> fail("Should not add a message to a non-existent channel")
                    is MessageRepository.Result.Poll.Success -> {
                        assertEquals(message, result.message)
                        Messages.selectAll().where {
                            Messages.content eq message.content and
                                (Messages.channel eq channel.id)
                        }.any()
                    }
                }
            }
        }

        @Test
        fun `2 - Should wait for message to be added`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance(code = "channel-1")
            val message = MessageFixture.any().asMessage
            val waitTime = Random.nextLong(30_000L)

            backgroundScope.launch {
                delay(waitTime.milliseconds)

                sut.add(channel, message).also { result ->
                    assertInstanceOf(MessageRepository.Result.Add.Success::class.java, result)
                }
            }

            when (val result = sut.poll(channel)) {
                MessageRepository.Result.Poll.QueueNotFound -> fail("Should not fail with queue not found")
                is MessageRepository.Result.Poll.Error -> fail("Should not add a message to a non-existent channel")
                is MessageRepository.Result.Poll.Success -> assertEquals(message, result.message)
            }

            assertEquals(waitTime, currentTime)
        }

        @Test
        fun `3 - Should fail by timeout if no message is added`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance(code = "channel-1")

            when (val result = sut.poll(channel)) {
                MessageRepository.Result.Poll.QueueNotFound -> fail("Should not fail with queue not found")
                is MessageRepository.Result.Poll.Success -> fail("Should not poll a message from an empty channel")
                is MessageRepository.Result.Poll.Error -> {
                    assertNotNull(result.e.message)
                    assertTrue(result.e.message!!.contains("timeout"))
                }
            }

            assertEquals(30L, currentTime)
        }
    }

    @Nested
    @DisplayName("3 - Subscribe operation tests")
    @TestMethodOrder(MethodOrderer.MethodName::class)
    inner class SubscribeTests {
        @Test
        fun `1 - Should subscribe to a valid channel successfully`() {

        }

        @Test
        fun `2 - Should fail to subscribe to a non-existent channel`() {

        }

        @Test
        fun `3 - Should receive messages from a subscribed channel in order`() {

        }
    }

    @Nested
    @DisplayName("4 - Remove channel queue tests")
    @TestMethodOrder(MethodOrderer.MethodName::class)
    inner class RemoveTests {
        @Test
        fun `1 - Should fail to remove a non-existing channel`() {

        }

        @Test
        fun `2 - Should return generic error message when the removal fails`() {

        }

        @Test
        fun `3 - Should remove existing channel successfully closing all subscribers`() {

        }
    }

    companion object {
        private const val GENERIC_ERROR_MESSAGE = "Failed to add message on queue"
        private val testDispatcher = UnconfinedTestDispatcher()
        private lateinit var channelRepository: ChannelRepository
        private lateinit var sut: MessageRepositoryDatabase

        @JvmStatic
        @BeforeAll
        fun setup() {
            DatabaseFixture.up()

            channelRepository = ChannelRepositoryDatabase()
            sut = MessageRepositoryDatabase(
                channelRepository = channelRepository,
                coroutineContext = testDispatcher
            )
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            DatabaseFixture.down()
        }
    }
}

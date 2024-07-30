package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.fixture.DatabaseFixture
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.test.extension.asMessage
import io.github.vinicreis.pubsub.server.core.test.extension.randomSlice
import io.github.vinicreis.pubsub.server.core.test.fixture.ChannelFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.ChannelFixture.id
import io.github.vinicreis.pubsub.server.core.test.fixture.MessageFixture
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@TestMethodOrder(value = MethodOrderer.MethodName::class)
class ChannelRepositoryDatabaseTests {
    @Test
    fun `1 - Should add channel successfully`() = runTest(testDispatcher) {
        sut.add(validChannel).also {
            assertInstanceOf(ChannelRepository.Result.Add.Success::class.java, it)
        }

        sut.getAll().also {
            when (it) {
                is ChannelRepository.Result.GetAll.Error -> fail("Failed to get channel $validChannel from database")
                is ChannelRepository.Result.GetAll.Success -> {
                    assertEquals(1, it.channels.size)
                    assertEquals(validChannel, it.channels.first())
                }
            }
        }
    }

    @Test
    fun `2 - Should not add channel with empty or blank id`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(
            id = id(),
            code = " ",
            name = "Test Channel 2",
            type = Channel.Type.MULTIPLE
        )

        sut.add(channel).also {
            when (it) {
                is ChannelRepository.Result.Add.Success -> fail("Channel $channel with empty id should NOT added")
                ChannelRepository.Result.Add.AlreadyFound -> fail("Channel $channel should not exist on database")
                is ChannelRepository.Result.Add.Error -> {
                    assertInstanceOf(IllegalArgumentException::class.java, it.e)
                    assertTrue(it.e.message?.contains("id") == true)
                }
            }
        }
    }

    @Test
    fun `3 - Should not add channel with empty or blank name`() = runTest(testDispatcher) {
        val channel = ChannelFixture.instance(
            id = id(),
            code = "channel",
            name = " ",
            type = Channel.Type.MULTIPLE
        )

        sut.add(channel).also {
            when (it) {
                is ChannelRepository.Result.Add.Success -> fail("Channel $channel with empty name should NOT added")
                ChannelRepository.Result.Add.AlreadyFound -> fail("Channel $channel should not exist on database")
                is ChannelRepository.Result.Add.Error -> {
                    assertInstanceOf(IllegalArgumentException::class.java, it.e)
                    assertTrue(it.e.message?.contains("name") == true)
                }
            }
        }
    }

    @Test
    fun `4 - Should not allow existing channel to be added`() = runTest(testDispatcher) {
        sut.add(validChannel).also {
            when (it) {
                is ChannelRepository.Result.Add.Success -> fail("Existing channel $validChannel should NOT added")
                is ChannelRepository.Result.Add.Error -> fail("Add should not fail with generic error")
                ChannelRepository.Result.Add.AlreadyFound -> Unit
            }
        }
    }

    @Test
    fun `5 - Should get the correct channel`() = runTest(testDispatcher) {
        sut.getById(validChannel.id).also {
            when (it) {
                is ChannelRepository.Result.GetById.Error -> fail("Failed to get channel $validChannel from database")
                ChannelRepository.Result.GetById.NotFound -> fail("Channel $validChannel should exist on database")
                is ChannelRepository.Result.GetById.Success -> assertEquals(validChannel, it.channel)
            }
        }
    }

    @Test
    fun `6 - Should count pending messages properly`() = runTest(testDispatcher) {
        val messageCount = Random.nextLong(1L, 100L)
        val messages = MessageFixture.EXAMPLES.randomSlice(messageCount.toInt()).map { it.asMessage }

        when (messageRepository.addAll(validChannel, messages)) {
            is MessageRepository.Result.Add.Error -> fail("Should not fail by generic error to add messages to valid channel")
            MessageRepository.Result.Add.QueueNotFound -> fail("Should not fail to find valid channel")
            MessageRepository.Result.Add.Success -> Unit
        }

        sut.getById(validChannel.id).also {
            when (it) {
                is ChannelRepository.Result.GetById.Error -> fail("Failed to get channel $validChannel from database")
                ChannelRepository.Result.GetById.NotFound -> fail("Channel $validChannel should exist on database")
                is ChannelRepository.Result.GetById.Success -> assertEquals(messageCount, it.channel.pendingMessagesCount)
            }
        }
    }

    @Test
    fun `7 - Should get all channels properly`() = runTest(testDispatcher) {
        sut.getAll().also {
            when (it) {
                is ChannelRepository.Result.GetAll.Error -> fail("Add should not fail with generic error")
                is ChannelRepository.Result.GetAll.Success -> assertEquals(1, it.channels.size)
            }
        }
    }

    @Test
    fun `8 - Should remove a existing channel successfully`() = runTest(testDispatcher) {
        when(messageRepository.remove(validChannel)) {
            is MessageRepository.Result.Remove.Error -> fail("Should not fail to remove messages from valid channel")
            MessageRepository.Result.Remove.QueueNotFound -> fail("Should not fail to find valid channel message queue")
            MessageRepository.Result.Remove.Success -> Unit
        }

        sut.removeByCode(validChannel.code).also {
            when (it) {
                is ChannelRepository.Result.Remove.Error -> fail("Remove should not fail with generic error")
                ChannelRepository.Result.Remove.NotFound -> fail("Channel with code \"channel-1\" should exist on database")
                is ChannelRepository.Result.Remove.Success -> assertEquals(validChannel, it.channel)
            }
        }

        sut.getAll().also {
            when (it) {
                is ChannelRepository.Result.GetAll.Error -> fail("Add should not fail with generic error")
                is ChannelRepository.Result.GetAll.Success -> assertTrue(it.channels.isEmpty())
            }
        }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()
        private lateinit var sut: ChannelRepositoryDatabase
        private lateinit var messageRepository: MessageRepositoryDatabase
        private val validChannel = ChannelFixture.instance()

        @BeforeAll
        @JvmStatic
        fun setup() {
            DatabaseFixture.up()

            sut = ChannelRepositoryDatabase()
            messageRepository = MessageRepositoryDatabase(sut, testDispatcher)
        }

        @AfterEach
        fun tearDown() {
            DatabaseFixture.down()
        }
    }
}

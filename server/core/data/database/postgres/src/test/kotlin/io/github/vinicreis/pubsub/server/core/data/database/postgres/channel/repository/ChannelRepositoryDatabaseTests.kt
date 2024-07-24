package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.fixture.DatabaseFixture
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestMethodOrder(value = MethodOrderer.MethodName::class)
class ChannelRepositoryDatabaseTests {
    @Test
    fun `1 - Should add channel successfully`() = runTest(testDispatcher) {
        val channel = Channel("channel-1", "Test Channel 1", Channel.Type.MULTIPLE)

        sut.add(channel).also {
            assertInstanceOf(ChannelRepository.Result.Add.Success::class.java, it)
        }

        sut.getAll().also {
            when (it) {
                is ChannelRepository.Result.GetAll.Error -> fail("Failed to get channel $channel from database")
                is ChannelRepository.Result.GetAll.Success -> {
                    assertEquals(1, it.channels.size)
                    assertEquals(channel, it.channels.first())
                }
            }
        }
    }

    @Test
    fun `2 - Should not add channel with empty or blank id`() = runTest(testDispatcher) {
        val channel = Channel(" ", "Test Channel 2", Channel.Type.MULTIPLE)

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
        val channel = Channel("channel", " ", Channel.Type.MULTIPLE)

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
        val channel = Channel("channel-1", "Test", Channel.Type.MULTIPLE)

        sut.add(channel).also {
            when (it) {
                is ChannelRepository.Result.Add.Success -> fail("Existing channel $channel should NOT added")
                is ChannelRepository.Result.Add.Error -> fail("Add should not fail with generic error")
                ChannelRepository.Result.Add.AlreadyFound -> Unit
            }
        }
    }

    @Test
    fun `5 - Should get the correct channel by id`() = runTest(testDispatcher) {
        val channel = Channel("channel-2", "Test", Channel.Type.SIMPLE)

        sut.add(channel).also {
            when (it) {
                is ChannelRepository.Result.Add.Error -> fail("Add should not fail with generic error")
                ChannelRepository.Result.Add.AlreadyFound -> fail("Channel $channel should NOT exist on database")
                is ChannelRepository.Result.Add.Success -> Unit
            }
        }

        sut.getById(channel.id).also {
            when (it) {
                is ChannelRepository.Result.GetById.Error -> fail("Failed to get channel $channel from database")
                ChannelRepository.Result.GetById.NotFound -> fail("Channel $channel should exist on database")
                is ChannelRepository.Result.GetById.Success -> assertEquals(channel, it.channel)
            }
        }
    }

    @Test
    fun `6 - Should get all channels properly`() = runTest(testDispatcher) {
        sut.getAll().also {
            when (it) {
                is ChannelRepository.Result.GetAll.Error -> fail("Add should not fail with generic error")
                is ChannelRepository.Result.GetAll.Success -> assertEquals(2, it.channels.size)
            }
        }
    }

    @Test
    fun `7 - Should remove a existing channel successfully`() = runTest(testDispatcher) {
        val removedChannel = Channel("channel-1", "Test Channel 1", Channel.Type.MULTIPLE)
        val removedChannel2 = Channel("channel-2", "Test", Channel.Type.SIMPLE)

        sut.removeById(removedChannel.id).also {
            when (it) {
                is ChannelRepository.Result.Remove.Error -> fail("Remove should not fail with generic error")
                ChannelRepository.Result.Remove.NotFound -> fail("Channel with id \"channel-1\" should exist on database")
                is ChannelRepository.Result.Remove.Success -> assertEquals(removedChannel, it.channel)
            }
        }

        sut.getAll().also {
            when (it) {
                is ChannelRepository.Result.GetAll.Error -> fail("Add should not fail with generic error")
                is ChannelRepository.Result.GetAll.Success -> {
                    assertEquals(1, it.channels.size)
                    assertEquals("channel-2", it.channels.first().id)
                }
            }
        }

        sut.remove(removedChannel2).also {
            when (it) {
                is ChannelRepository.Result.Remove.Error -> fail("Remove should not fail with generic error")
                ChannelRepository.Result.Remove.NotFound -> fail("Channel $removedChannel2 should exist on database")
                is ChannelRepository.Result.Remove.Success -> assertEquals(removedChannel2, it.channel)
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

        @BeforeAll
        @JvmStatic
        fun setup() {
            DatabaseFixture.up()

            sut = ChannelRepositoryDatabase()
        }

        @AfterEach
        fun tearDown() {
            DatabaseFixture.down()
        }
    }
}

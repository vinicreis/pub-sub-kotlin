package io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.fixture.DatabaseFixture
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.EventRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.QueueRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.TextMessageRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueAddedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueRemovedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.test.extension.asTextMessage
import io.github.vinicreis.pubsub.server.core.test.extension.randomSlice
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture.id
import io.github.vinicreis.pubsub.server.core.test.fixture.TextMessageFixture
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
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
class QueueRepositoryDatabaseTests {
    @Test
    fun `1 - Should add queue successfully`() = runTest(testDispatcher) {
        sut.add(validQueue).also {
            assertInstanceOf(QueueRepository.Result.Add.Success::class.java, it)
        }

        sut.getAll().also {
            when (it) {
                is QueueRepository.Result.GetAll.Error -> fail("Failed to get queue $validQueue from database")
                is QueueRepository.Result.GetAll.Success -> {
                    assertEquals(1, it.queues.size)
                    assertEquals(validQueue, it.queues.first())
                }
            }
        }

        when (val event = eventsRepository.consume(validQueue.id)) {
            is QueueRemovedEvent,
            is TextMessageReceivedEvent,
            null -> fail("Should have emitted queue added event")

            is QueueAddedEvent -> {
                assertEquals(validQueue, event.queue)
            }
        }
    }

    @Test
    fun `2 - Should not add queue with empty or blank id`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(
            id = id(),
            code = " ",
            name = "Test Queue 2",
            type = Queue.Type.MULTIPLE
        )

        sut.add(queue).also {
            when (it) {
                is QueueRepository.Result.Add.Success -> fail("Queue $queue with empty id should NOT added")
                QueueRepository.Result.Add.AlreadyFound -> fail("Queue $queue should not exist on database")
                is QueueRepository.Result.Add.Error -> {
                    assertInstanceOf(IllegalArgumentException::class.java, it.e)
                    assertTrue(it.e.message?.contains("id") == true)
                }
            }
        }
    }

    @Test
    fun `3 - Should not add queue with empty or blank name`() = runTest(testDispatcher) {
        val queue = QueueFixture.instance(
            id = id(),
            code = "queue",
            name = " ",
            type = Queue.Type.MULTIPLE
        )

        sut.add(queue).also {
            when (it) {
                is QueueRepository.Result.Add.Success -> fail("Queue $queue with empty name should NOT added")
                QueueRepository.Result.Add.AlreadyFound -> fail("Queue $queue should not exist on database")
                is QueueRepository.Result.Add.Error -> {
                    assertInstanceOf(IllegalArgumentException::class.java, it.e)
                    assertTrue(it.e.message?.contains("name") == true)
                }
            }
        }
    }

    @Test
    fun `4 - Should not allow existing queue to be added`() = runTest(testDispatcher) {
        sut.add(validQueue).also {
            when (it) {
                is QueueRepository.Result.Add.Success -> fail("Existing queue $validQueue should NOT added")
                is QueueRepository.Result.Add.Error -> fail("Add should not fail with generic error")
                QueueRepository.Result.Add.AlreadyFound -> Unit
            }
        }
    }

    @Test
    fun `5 - Should get the correct queue`() = runTest(testDispatcher) {
        sut.getById(validQueue.id).also {
            when (it) {
                is QueueRepository.Result.GetById.Error -> fail("Failed to get queue $validQueue from database")
                QueueRepository.Result.GetById.NotFound -> fail("Queue $validQueue should exist on database")
                is QueueRepository.Result.GetById.Success -> assertEquals(validQueue, it.queue)
            }
        }
    }

    @Test
    fun `6 - Should count pending messages properly`() = runTest(testDispatcher) {
        val messageCount = Random.nextLong(1L, 100L)
        val messages = TextMessageFixture.EXAMPLES.randomSlice(messageCount.toInt()).map { it.asTextMessage(validQueue) }

        when (val result = messageRepository.add(validQueue, messages)) {
            is TextMessageRepository.Result.Add.Error -> fail("Should not fail by generic error to add messages to valid queue", result.e)
            TextMessageRepository.Result.Add.Success -> Unit
        }

        when (val result = sut.getById(validQueue.id)) {
            is QueueRepository.Result.GetById.Error -> fail("Failed to get queue $validQueue from database", result.e)
            QueueRepository.Result.GetById.NotFound -> fail("Queue $validQueue should exist on database")
            is QueueRepository.Result.GetById.Success -> assertEquals(messageCount, result.queue.pendingMessagesCount)

        }
    }

    @Test
    fun `7 - Should get all queues properly`() = runTest(testDispatcher) {
        sut.getAll().also {
            when (it) {
                is QueueRepository.Result.GetAll.Error -> fail("Add should not fail with generic error")
                is QueueRepository.Result.GetAll.Success -> assertEquals(1, it.queues.size)
            }
        }
    }

    @Test
    fun `8 - Should remove a existing queue successfully`() = runTest(testDispatcher) {
        when (messageRepository.removeAll(validQueue)) {
            is TextMessageRepository.Result.Remove.Error -> fail("Should not fail to remove messages from valid queue")
            TextMessageRepository.Result.Remove.Success -> Unit
        }

        sut.removeByCode(validQueue.code).also {
            when (it) {
                is QueueRepository.Result.Remove.Error -> fail("Remove should not fail with generic error")
                QueueRepository.Result.Remove.NotFound -> fail("Queue with code \"queue-1\" should exist on database")
                is QueueRepository.Result.Remove.Success -> assertEquals(validQueue, it.queue)
            }
        }

        sut.getAll().also {
            when (it) {
                is QueueRepository.Result.GetAll.Error -> fail("Add should not fail with generic error")
                is QueueRepository.Result.GetAll.Success -> assertTrue(it.queues.isEmpty())
            }
        }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()
        private lateinit var sut: QueueRepositoryDatabase
        private lateinit var eventsRepository: EventsRepository
        private lateinit var messageRepository: TextMessageRepositoryDatabase
        private val validQueue = QueueFixture.instance()

        @BeforeAll
        @JvmStatic
        fun setup() {
            DatabaseFixture.up()

            eventsRepository = EventRepositoryDatabase()
            sut = QueueRepositoryDatabase(eventsRepository = eventsRepository)
            messageRepository = TextMessageRepositoryDatabase(
                coroutineContext = testDispatcher,
                eventsRepository = eventsRepository
            )
        }

        @AfterEach
        fun tearDown() {
            DatabaseFixture.down()
        }
    }
}

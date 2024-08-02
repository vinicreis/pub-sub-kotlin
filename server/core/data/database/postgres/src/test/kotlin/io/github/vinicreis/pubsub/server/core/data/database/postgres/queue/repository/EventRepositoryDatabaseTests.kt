package io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Events
import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.TextMessages
import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.fixture.DatabaseFixture
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.EventRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.QueueRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.TextMessageRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueAddedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueRemovedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.test.extension.asTextMessage
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.TextMessageFixture
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.fail

@OptIn(ExperimentalCoroutinesApi::class)
@TestMethodOrder(value = MethodOrderer.MethodName::class)
class EventRepositoryDatabaseTests {
    @Test
    fun `01 - Should save a queue added event successfully when a queue is added`() = runTest(testDispatcher) {
        when (val result = queueRepository.add(validQueue)) {
            QueueRepository.Result.Add.AlreadyFound -> fail("Should not fail to add queue")
            is QueueRepository.Result.Add.Error -> fail("Should not fail to add queue", result.e)
            is QueueRepository.Result.Add.Success -> Unit
        }

        when (val event = sut.consume(validQueue.id)) {
            null -> fail("Should have emitted queue added event")
            is QueueRemovedEvent -> fail("Should not have emitted queue removed event")
            is TextMessageReceivedEvent -> fail("Should not have emitted text message received event")
            is QueueAddedEvent -> assertEquals(validQueue, event.queue)
        }
    }

    @Test
    fun `02 - Should delete queue added event from database when it is consumed`() = runTest(testDispatcher) {
        assertEquals(0, transaction { Events.selectAll().count() })
    }

    @Test
    fun `03 - Should save a text received event successfully when a text message is added`() = runTest(testDispatcher) {
        val queue = validQueue.copy(pendingMessagesCount = 1)
        val message = TextMessageFixture.any().asTextMessage(queue)

        when(val result = textMessageRepository.add(validQueue, listOf(message))) {
            is TextMessageRepository.Result.Add.Error -> fail("Should not fail to add text message", result.e)
            TextMessageRepository.Result.Add.Success -> Unit
        }

        assertEquals(1, transaction { TextMessages.selectAll().count() })

        when (val event = sut.consume(validQueue.id)) {
            null -> fail("Should have emitted text message received event")
            is QueueRemovedEvent -> fail("Should not have emitted queue removed event")
            is QueueAddedEvent -> fail("Should not have emitted queue added event")
            is TextMessageReceivedEvent -> assertEquals(message, event.textMessage)
        }
    }

    @Test
    fun `04 - Should delete text message received event and row from its database when it is consumed`() =
        runTest(testDispatcher) {
            assertEquals(0, transaction { Events.selectAll().count() })
            assertEquals(0, transaction { TextMessages.selectAll().count() })
        }

    @Test
    fun `05 - Should save a queue removed event successfully when its removed`() = runTest(testDispatcher) {
        when (val result = queueRepository.remove(validQueue)) {
            QueueRepository.Result.Remove.NotFound -> fail("Should not fail to find queue on remove")
            is QueueRepository.Result.Remove.Error -> fail("Should not fail to remove queue", result.e)
            is QueueRepository.Result.Remove.Success -> assertEquals(validQueue, result.queue)
        }

        when (val event = sut.consume(validQueue.id)) {
            null -> fail("Should have emitted queue removed event")
            is QueueAddedEvent -> fail("Should not have emitted queue added event")
            is TextMessageReceivedEvent -> fail("Should not have emitted text message received event")
            is QueueRemovedEvent -> assertEquals(validQueue.id, event.queueId)
        }
    }

    @Test
    fun `06 - Should delete queue removed event from database when it is consumed`() = runTest(testDispatcher) {
        assertEquals(0, transaction { Events.selectAll().count() })
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()
        private lateinit var sut: EventRepositoryDatabase
        private lateinit var queueRepository: QueueRepositoryDatabase
        private lateinit var textMessageRepository: TextMessageRepositoryDatabase
        private val validQueue = QueueFixture.instance()

        @BeforeAll
        @JvmStatic
        fun setup() {
            DatabaseFixture.up()

            sut = EventRepositoryDatabase(testDispatcher)
            queueRepository = QueueRepositoryDatabase(testDispatcher, sut)
            textMessageRepository = TextMessageRepositoryDatabase(testDispatcher, sut)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            DatabaseFixture.down()
        }
    }
}
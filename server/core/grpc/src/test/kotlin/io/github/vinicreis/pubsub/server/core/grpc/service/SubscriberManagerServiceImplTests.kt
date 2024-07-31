package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import io.github.vinicreis.pubsub.server.core.test.extension.asTextMessage
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.logging.Logger
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriberManagerServiceImplTests {
    private val eventsRepository = mockk<EventsRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val sut = SubscriberManagerImpl(
        eventsRepository = eventsRepository,
        coroutineContext = testDispatcher,
        logger = Logger.getLogger("SubscriberManagerImplTests")
    )

    @Test
    fun `Should close queue when some error happens to fetch message queue`() = runTest {
        coEvery { eventsRepository.consume() } returns
                TextMessageRepository.Result.Subscribe.Error(IllegalArgumentException("Error"))

        val textMessages = mutableListOf<TextMessage>()
        val queue = QueueFixture.instance()
        val subscriber = sut.subscribe(queue)

        launch { assertThrows<RuntimeException> { subscriber.toList(textMessages) } }
        advanceUntilIdle()

        coVerify(exactly = 2) { eventsRepository.consume() }
        assertTrue(textMessages.isEmpty())
    }

    @Test
    fun `Should return a valid queue flow for subscriber when the queue is found`() = runTest {
        val textMessageChannel = Channel<TextMessage>(Channel.UNLIMITED)
        val textMessages = mutableListOf<TextMessage>()
        val queue = QueueFixture.instance(type = Queue.Type.SIMPLE)
        val messagesEmitted = Random.nextInt(10)

        coEvery { eventsRepository.consume() } returns
                TextMessageRepository.Result.Subscribe.Success(textMessageChannel.receiveAsFlow())

        val subscriber = sut.subscribe(queue)

        backgroundScope.launch(testDispatcher) { subscriber.toList(textMessages) }
        launch {
            repeat(messagesEmitted) { i ->
                textMessageChannel.send("Message $i".asTextMessage)
            }
        }

        advanceUntilIdle()

        coVerify(exactly = 2) { eventsRepository.consume(queue) }
        assertEquals(messagesEmitted, textMessages.size)
    }

    @Test
    fun `Should send message to only one subscriber when some message is posted on a simple queue`() = runTest {
        val textMessageChannel = Channel<TextMessage>(Channel.UNLIMITED)
        val queue = QueueFixture.instance(type = Queue.Type.SIMPLE)
        val messagesEmitted = Random.nextInt(10)
        val messages1 = mutableListOf<TextMessage>()
        val messages2 = mutableListOf<TextMessage>()
        val messages3 = mutableListOf<TextMessage>()
        val messages4 = mutableListOf<TextMessage>()

        coEvery { eventsRepository.consume() } returns
                TextMessageRepository.Result.Subscribe.Success(textMessageChannel.receiveAsFlow())

        val subscriber1 = sut.subscribe(queue)
        val subscriber2 = sut.subscribe(queue)
        val subscriber3 = sut.subscribe(queue)
        val subscriber4 = sut.subscribe(queue)

        backgroundScope.launch(testDispatcher) { subscriber1.toList(messages1) }
        backgroundScope.launch(testDispatcher) { subscriber2.toList(messages2) }
        backgroundScope.launch(testDispatcher) { subscriber3.toList(messages3) }
        backgroundScope.launch(testDispatcher) { subscriber4.toList(messages4) }

        launch {
            repeat(messagesEmitted) { i ->
                textMessageChannel.send("Message $i".asTextMessage)
            }
        }

        advanceUntilIdle()

        coVerify(exactly = 5) { eventsRepository.consume(queue) }
        assertEquals(messagesEmitted, messages1.size + messages2.size + messages3.size + messages4.size)
    }

    @Test
    fun `Should send message to all subscribers when some message is posted on a multiple queue`() = runTest {
        val textMessageChannel = Channel<TextMessage>(Channel.UNLIMITED)
        val queue = QueueFixture.instance(type = Queue.Type.MULTIPLE)
        val messages1 = mutableListOf<TextMessage>()
        val messages2 = mutableListOf<TextMessage>()
        val messages3 = mutableListOf<TextMessage>()
        val messages4 = mutableListOf<TextMessage>()
        val messagesEmitted = buildList {
            repeat(Random.nextInt(10)) { i ->
                add("Message $i".asTextMessage)
            }
        }

        coEvery { eventsRepository.consume() } returns
                TextMessageRepository.Result.Subscribe.Success(textMessageChannel.receiveAsFlow())

        val subscriber1 = sut.subscribe(queue)
        val subscriber2 = sut.subscribe(queue)
        val subscriber3 = sut.subscribe(queue)
        val subscriber4 = sut.subscribe(queue)

        backgroundScope.launch(testDispatcher) { subscriber1.toList(messages1) }
        backgroundScope.launch(testDispatcher) { subscriber2.toList(messages2) }
        backgroundScope.launch(testDispatcher) { subscriber3.toList(messages3) }
        backgroundScope.launch(testDispatcher) { subscriber4.toList(messages4) }

        launch {
            messagesEmitted.forEach { message ->
                textMessageChannel.send(message)
            }
        }

        advanceUntilIdle()

        coVerify(exactly = 5) { eventsRepository.consume(queue) }
        assertEquals(messagesEmitted, messages1)
        assertEquals(messagesEmitted, messages2)
        assertEquals(messagesEmitted, messages3)
        assertEquals(messagesEmitted, messages4)
    }

    @Test
    fun `Should close queue if the queue is removed from message queue`() = runTest {
        val textMessageChannel = Channel<TextMessage>(Channel.UNLIMITED)
        val queue = QueueFixture.instance(type = Queue.Type.MULTIPLE)
        val messagesEmitted = Random.nextInt(10)
        val textMessages = mutableListOf<TextMessage>()

        coEvery { eventsRepository.consume() } returns
                TextMessageRepository.Result.Subscribe.Success(textMessageChannel.receiveAsFlow())

        val subscriber = sut.subscribe(queue)

        val listJob = launch(testDispatcher) { subscriber.toList(textMessages) }

        launch {
            repeat(messagesEmitted) { i ->
                textMessageChannel.send("Message $i".asTextMessage)
            }

            textMessageChannel.close(CancellationException("Queue removed!"))
            println("Queue removed!")
        }

        advanceUntilIdle()

        coVerify(exactly = 2) { eventsRepository.consume() }
        assertEquals(messagesEmitted, textMessages.size)
        println("Validating cancellation...")
        assertTrue(listJob.isCancelled)
    }
}

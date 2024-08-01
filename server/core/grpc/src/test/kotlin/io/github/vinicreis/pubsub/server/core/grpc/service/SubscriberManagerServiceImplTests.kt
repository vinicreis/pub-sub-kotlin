package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import io.github.vinicreis.pubsub.server.core.model.data.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.test.extension.asTextMessage
import io.github.vinicreis.pubsub.server.core.test.extension.randomSlice
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.TextMessageFixture
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriberManagerServiceImplTests {
    private val eventsRepository = mockk<EventsRepository>()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sut: SubscriberManagerImpl

    @Test
    fun `Should close queue when some error happens to fetch message queue`() = runTest(testDispatcher) {
        coEvery { eventsRepository.consume() } returns
                EventsRepository.Result.Consume.Fail(IllegalArgumentException("Error")) andThen
                EventsRepository.Result.Consume.None

        val textMessages = mutableListOf<TextMessage>()
        val queue = QueueFixture.instance()
        sut = SubscriberManagerImpl(
            eventsRepository = eventsRepository,
            coroutineContext = testDispatcher,
            logger = Logger.getLogger("SubscriberManagerImplTests"),
        )

//        backgroundScope.launch { sut.subscribe(queue).toList(textMessages) }
//        advanceTimeBy(1.seconds)

        assertEquals(30.seconds.inWholeMilliseconds, currentTime)
        coVerify(atLeast = 30) { eventsRepository.consume() }
        assertTrue(textMessages.isEmpty())
    }

    @Test
    fun `Should return a valid queue flow for subscriber when the queue is found`() = runTest {
        val textMessages = mutableListOf<TextMessage>()
        val queue = QueueFixture.instance(type = Queue.Type.SIMPLE)
        val messagesEmitted = TextMessageFixture.EXAMPLES.randomSlice(Random.nextInt(100)).map { it.asTextMessage }
        val events = messagesEmitted.map {
            EventsRepository.Result.Consume.Success(
                TextMessageReceivedEvent(
                    queue = queue,
                    textMessage = messagesEmitted.first()
                )
            )
        }

        (coEvery { eventsRepository.consume() } returns events.first()).apply {
            events.drop(1).forEach { event -> this andThen event }
        }

        val subscriber = sut.subscribe(queue)

        backgroundScope.launch(testDispatcher) { subscriber.toList(textMessages) }

        assertEquals(messagesEmitted, textMessages)
    }

    @Test
    fun `Should send message to only one subscriber when some message is posted on a simple queue`() = runTest {
        val queue = QueueFixture.instance(type = Queue.Type.SIMPLE)
        val messages1 = mutableListOf<TextMessage>()
        val messages2 = mutableListOf<TextMessage>()
        val messages3 = mutableListOf<TextMessage>()
        val messages4 = mutableListOf<TextMessage>()
        val messagesEmitted = TextMessageFixture.EXAMPLES.randomSlice(Random.nextInt(100)).map { it.asTextMessage }
        val events = messagesEmitted.map {
            EventsRepository.Result.Consume.Success(
                TextMessageReceivedEvent(
                    queue = queue,
                    textMessage = messagesEmitted.first()
                )
            )
        }

        (coEvery { eventsRepository.consume() } returns events.first()).apply {
            events.drop(1).forEach { event -> this andThen event }
        }

        val subscriber1 = sut.subscribe(queue)
        val subscriber2 = sut.subscribe(queue)
        val subscriber3 = sut.subscribe(queue)
        val subscriber4 = sut.subscribe(queue)

        backgroundScope.launch(testDispatcher) { subscriber1.toList(messages1) }
        backgroundScope.launch(testDispatcher) { subscriber2.toList(messages2) }
        backgroundScope.launch(testDispatcher) { subscriber3.toList(messages3) }
        backgroundScope.launch(testDispatcher) { subscriber4.toList(messages4) }

        assertEquals(messagesEmitted, messages1.size + messages2.size + messages3.size + messages4.size)
    }

    @Test
    fun `Should send message to all subscribers when some message is posted on a multiple queue`() = runTest {
        val queue = QueueFixture.instance(type = Queue.Type.MULTIPLE)
        val messages1 = mutableListOf<TextMessage>()
        val messages2 = mutableListOf<TextMessage>()
        val messages3 = mutableListOf<TextMessage>()
        val messages4 = mutableListOf<TextMessage>()
        val messagesEmitted = TextMessageFixture.EXAMPLES.randomSlice(Random.nextInt(100)).map { it.asTextMessage }
        val events = messagesEmitted.map {
            EventsRepository.Result.Consume.Success(
                TextMessageReceivedEvent(
                    queue = queue,
                    textMessage = messagesEmitted.first()
                )
            )
        }

        (coEvery { eventsRepository.consume() } returns events.first()).apply {
            events.drop(1).forEach { event -> this andThen event }
        }

        val subscriber1 = sut.subscribe(queue)
        val subscriber2 = sut.subscribe(queue)
        val subscriber3 = sut.subscribe(queue)
        val subscriber4 = sut.subscribe(queue)

        backgroundScope.launch(testDispatcher) { subscriber1.toList(messages1) }
        backgroundScope.launch(testDispatcher) { subscriber2.toList(messages2) }
        backgroundScope.launch(testDispatcher) { subscriber3.toList(messages3) }
        backgroundScope.launch(testDispatcher) { subscriber4.toList(messages4) }

        assertEquals(messagesEmitted, messages1)
        assertEquals(messagesEmitted, messages2)
        assertEquals(messagesEmitted, messages3)
        assertEquals(messagesEmitted, messages4)
    }

    @Test
    fun `Should close queue if the queue is removed from message queue`() = runTest {
        val queue = QueueFixture.instance(type = Queue.Type.MULTIPLE)
        val textMessages = mutableListOf<TextMessage>()
        val messagesEmitted = TextMessageFixture.EXAMPLES.randomSlice(Random.nextInt(100)).map { it.asTextMessage }
        val events = messagesEmitted.map {
            EventsRepository.Result.Consume.Success(
                TextMessageReceivedEvent(
                    queue = queue,
                    textMessage = messagesEmitted.first()
                )
            )
        }

        (coEvery { eventsRepository.consume() } returns events.first()).apply {
            events.drop(1).forEach { event -> this andThen event }
        }

        val subscriber = sut.subscribe(queue)
        val listJob = launch(testDispatcher) { subscriber.toList(textMessages) }

        assertEquals(messagesEmitted, textMessages.size)
        assertTrue(listJob.isCancelled)
    }
}

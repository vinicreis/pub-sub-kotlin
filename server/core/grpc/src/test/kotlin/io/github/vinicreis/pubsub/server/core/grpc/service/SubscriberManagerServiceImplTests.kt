package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Event
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.test.extension.asTextMessage
import io.github.vinicreis.pubsub.server.core.test.extension.randomSlice
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.TextMessageFixture
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
    private val queueRepository = mockk<QueueRepository>()
    private val textMessageRepository = mockk<TextMessageRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val logger = Logger.getLogger("SubscriberManagerImplTests")
    private lateinit var sut: SubscriberManagerImpl

    @Test
    fun `Should close queue when some error happens to fetch message queue`() = runTest(testDispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
//        val time = Random.nextInt(1, 60)
        val time = 2
        val queue = QueueFixture.instance()
        val events = mutableListOf<Event>()

        coEvery { queueRepository.getAll() } returns QueueRepository.Result.GetAll.Success(listOf(queue))
        coEvery { eventsRepository.consume(queue.id) } returns EventsRepository.Result.Consume.None

        sut = SubscriberManagerImpl(
            coroutineContext = testDispatcher,
            logger = logger,
            eventsRepository = eventsRepository,
            queueRepository = queueRepository,
            textMessageRepository = textMessageRepository,
        )

        launch(dispatcher) {
            sut.subscribe(queue)
                .takeWhile { events.size < time }
                .toList(events)
        }
        advanceTimeBy(time.seconds)

        assertEquals(time.seconds.inWholeMilliseconds, currentTime)
//        coVerify(exactly = time) { eventsRepository.consume(queue.id) }
//        assertEquals(time, events.size)
    }

    @Test
    fun `Should return a valid queue flow for subscriber when the queue is found`() = runTest {
        val textMessages = mutableListOf<Event>()
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

        (coEvery { eventsRepository.consume(queue.id) } returns events.first()).apply {
            events.drop(1).forEach { event -> this andThen event }
        }

        val subscriber = sut.subscribe(queue)

        backgroundScope.launch(testDispatcher) { subscriber.toList(textMessages) }

        assertEquals(messagesEmitted, textMessages)
    }

    @Test
    fun `Should send message to only one subscriber when some message is posted on a simple queue`() = runTest {
        val queue = QueueFixture.instance(type = Queue.Type.SIMPLE)
        val messages1 = mutableListOf<Event>()
        val messages2 = mutableListOf<Event>()
        val messages3 = mutableListOf<Event>()
        val messages4 = mutableListOf<Event>()
        val messagesEmitted = TextMessageFixture.EXAMPLES.randomSlice(Random.nextInt(100)).map { it.asTextMessage }
        val events = messagesEmitted.map {
            EventsRepository.Result.Consume.Success(
                TextMessageReceivedEvent(
                    queue = queue,
                    textMessage = messagesEmitted.first()
                )
            )
        }

        (coEvery { eventsRepository.consume(queue.id) } returns events.first()).apply {
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
        val messages1 = mutableListOf<Event>()
        val messages2 = mutableListOf<Event>()
        val messages3 = mutableListOf<Event>()
        val messages4 = mutableListOf<Event>()
        val messagesEmitted = TextMessageFixture.EXAMPLES.randomSlice(Random.nextInt(100)).map { it.asTextMessage }
        val events = messagesEmitted.map {
            EventsRepository.Result.Consume.Success(
                TextMessageReceivedEvent(
                    queue = queue,
                    textMessage = messagesEmitted.first()
                )
            )
        }

        (coEvery { eventsRepository.consume(queue.id) } returns events.first()).apply {
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
        val textMessages = mutableListOf<Event>()
        val messagesEmitted = TextMessageFixture.EXAMPLES.randomSlice(Random.nextInt(100)).map { it.asTextMessage }
        val events = messagesEmitted.map {
            EventsRepository.Result.Consume.Success(
                TextMessageReceivedEvent(
                    queue = queue,
                    textMessage = messagesEmitted.first()
                )
            )
        }

        (coEvery { eventsRepository.consume(queue.id) } returns events.first()).apply {
            events.drop(1).forEach { event -> this andThen event }
        }

        val subscriber = sut.subscribe(queue)
        val listJob = launch(testDispatcher) { subscriber.toList(textMessages) }

        assertEquals(messagesEmitted, textMessages.size)
        assertTrue(listJob.isCancelled)
    }
}

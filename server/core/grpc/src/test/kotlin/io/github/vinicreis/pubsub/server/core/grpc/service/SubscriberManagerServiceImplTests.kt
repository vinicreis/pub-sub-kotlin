package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.event.Event
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueRemovedEvent
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriberManagerServiceImplTests {
    private val eventsRepository = mockk<EventsRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val logger = Logger.getLogger("SubscriberManagerImplTests")
    private val sut: SubscriberManagerImpl = SubscriberManagerImpl(
        coroutineContext = testDispatcher,
        logger = logger,
        eventsRepository = eventsRepository,
    )

    @Test
    fun `Should emit all events while has only one subscriber on simple channel`() = runTest(testDispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val time = Random.nextInt(1, 60)
        val queue = QueueFixture.instance(type = Queue.Type.SIMPLE)
        val events = mutableListOf<Event>()

        coEvery { eventsRepository.consume(queue.id) } returns null

        launch(dispatcher) { sut.subscribe(queue).takeWhile { events.size < time }.toList(events) }
        advanceTimeBy((time * 2).seconds)

        assertEquals((time.seconds * 2).inWholeMilliseconds, currentTime)
        coVerify(exactly = time + 1) { eventsRepository.consume(queue.id) }
        assertEquals(time, events.size)
    }

    @Test
    fun `Should emit all events once with multiple subscribers on simple channel`() = runTest(testDispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val time = Random.nextInt(1, 60)
        val queue = QueueFixture.instance(type = Queue.Type.SIMPLE)
        val subscribersCount = Random.nextInt(50, 100)
        val events = mutableListOf<Event>()
        val subscribers = buildList {
            repeat(subscribersCount) { add(mutableListOf<Event>()) }
        }

        coEvery { eventsRepository.consume(queue.id) } returns null

        repeat(subscribersCount) { index ->
            launch(dispatcher) {
                sut.subscribe(queue)
                    .takeWhile { events.size < time }
                    .onEach { events.add(it) }
                    .toList(subscribers[index])
            }
        }
        advanceTimeBy((time * 2).seconds)

        assertEquals((time.seconds * 2).inWholeMilliseconds, currentTime)
        coVerify(atLeast = time) { eventsRepository.consume(queue.id) }
        assertEquals(events.size, subscribers.sumOf { it.size })
    }

    @Test
    fun `Should send message to all subscribers when some message is posted on a multiple queue`() =
        runTest(testDispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val time = Random.nextInt(1, 60)
            val queue = QueueFixture.instance(type = Queue.Type.MULTIPLE)
            val subscribersCount = Random.nextInt(50, 100)
            val subscribers = buildList {
                repeat(subscribersCount) { add(mutableListOf<Event>()) }
            }

            coEvery { eventsRepository.consume(queue.id) } returns null

            repeat(subscribersCount) { index ->
                launch(dispatcher) {
                    sut.subscribe(queue)
                        .takeWhile { subscribers[index].size < time }
                        .toList(subscribers[index])
                }
            }
            advanceTimeBy((time * 2).seconds)

            assertEquals((time.seconds * 2).inWholeMilliseconds, currentTime)
            coVerify(atLeast = time) { eventsRepository.consume(queue.id) }
            subscribers.forEach { subscriber -> assertEquals(time, subscriber.size) }
        }

    @Test
    fun `Should should stop collecting queue events when there are no subscribers left`() = runTest(testDispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val queue = QueueFixture.instance(type = Queue.Type.SIMPLE)
        val subscribersCount = Random.nextInt(50, 100)
        val time = 60.seconds

        coEvery { eventsRepository.consume(queue.id) } returns null

        val jobs = buildList {
            repeat(subscribersCount) {
                launch(dispatcher) { sut.subscribe(queue).collect { } }.also { add(it) }
            }
        }
        advanceTimeBy(time)
        jobs.forEach { it.cancel() }

        assertEquals(time.inWholeMilliseconds, currentTime)
        coVerify(exactly = time.inWholeSeconds.toInt()) { eventsRepository.consume(queue.id) }
    }

    @Test
    fun `Should close subscribers when queue is removed`() = runTest(testDispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val queue = QueueFixture.instance(type = Queue.Type.SIMPLE)
        val subscribersCount = Random.nextInt(5, 100)
        val secondsToClose = Random.nextInt(subscribersCount, 100)
        val events = buildList {
            repeat(secondsToClose) {
                add(null)
            }

            add(QueueRemovedEvent(queue = queue))
        }

        println(events)

        coEvery { eventsRepository.consume(queue.id) } returnsMany events

        repeat(subscribersCount) {
            launch(dispatcher) { sut.subscribe(queue).collect { } }
        }
        advanceUntilIdle()

        assertEquals((subscribersCount + secondsToClose - 1).seconds.inWholeMilliseconds, currentTime)
        coVerify(exactly = subscribersCount + secondsToClose) { eventsRepository.consume(queue.id) }
    }
}

package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.event.Event
import io.github.vinicreis.pubsub.server.core.model.data.event.HeartbeatEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueAddedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueRemovedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SubscriberManagerImpl(
    coroutineContext: CoroutineContext,
    private val checkInterval: Duration = 1.seconds,
    private val eventsRepository: EventsRepository,
    private val logger: Logger = Logger.getLogger(SubscriberManagerImpl::class.java.simpleName)
) : SubscriberManagerService {
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val subscribers = ConcurrentHashMap<UUID, MutableList<ProducerScope<Event>>>()
    private val jobs = ConcurrentHashMap<UUID, Job>()

    override fun subscribe(queue: Queue): Flow<Event> = channelFlow {
        subscribers.getOrPut(queue.id) { mutableListOf() }.add(this)

        queue.collect()
        yield()

        awaitClose {
            subscribers[queue.id]?.remove(this)
            subscribers[queue.id]?.ifEmpty { jobs[queue.id]?.cancel() }
        }
    }.catch { logger.severe("Something went wrong while collecting subscriber event") }

    private suspend fun Queue.withChosenSubscriber(block: suspend ProducerScope<Event>.() -> Unit) {
        when (type) {
            Queue.Type.SIMPLE -> subscribers[id]?.random()?.block()
            Queue.Type.MULTIPLE -> subscribers[id]?.forEach { it.block() }
        }
    }

    private suspend fun Queue.collect() {
        jobs.getOrPut(id) {
            coroutineScope.launch {
                while (true) {
                    val event = eventsRepository.consume(queueId = id)

                    withChosenSubscriber {
                        when (event) {
                            is TextMessageReceivedEvent -> send(event)
                            is QueueRemovedEvent -> close()
                            is QueueAddedEvent -> error("This queue should be added already!")
                            null -> send(HeartbeatEvent)
                        }
                    }

                    delay(checkInterval)
                }
            }
        }
    }
}

package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.event.Event
import io.github.vinicreis.pubsub.server.core.model.data.event.HeartbeatEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueAddedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueRemovedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.EventRepository
import kotlinx.coroutines.CancellationException
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

class SubscriberManagerServiceImpl(
    coroutineContext: CoroutineContext,
    private val checkInterval: Duration = 1.seconds,
    private val eventRepository: EventRepository,
    private val logger: Logger = Logger.getLogger(SubscriberManagerServiceImpl::class.java.simpleName)
) : SubscriberManagerService {
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val subscribers = ConcurrentHashMap<UUID, MutableList<ProducerScope<Event>>>()
    private val jobs = ConcurrentHashMap<UUID, Job>()

    override fun subscribe(queue: Queue): Flow<Event> = channelFlow {
        subscribers.getOrPut(queue.id) { mutableListOf() }.add(this)

        queue.launchCollection()
        yield()

        awaitClose {
            subscribers[queue.id]?.remove(this)
            subscribers[queue.id]?.ifEmpty {
                jobs.remove(queue.id)?.cancel()
            }
        }
    }.catch { logger.severe("Something went wrong while collecting subscriber event") }

    private suspend fun Queue.withChosenSubscriber(block: suspend ProducerScope<Event>.() -> Unit) {
        when (type) {
            Queue.Type.SIMPLE -> subscribers[id]?.random()?.block()
            Queue.Type.MULTIPLE -> subscribers[id]?.forEach { it.block() }
        }
    }

    private fun Queue.launchCollection() {
        jobs.getOrPut(id) {
            coroutineScope.launch {
                try {
                    logger.info("Starting collecting events for queue: $id")

                    while (true) {
                        val event = eventRepository.consume(queueId = id)

                        logger.fine("Event received from queue $id: $event")

                        withChosenSubscriber {
                            when (event) {
                                is TextMessageReceivedEvent -> send(event)
                                is QueueRemovedEvent -> close(CancellationException("Queue $id was removed"))
                                is QueueAddedEvent -> send(event)
                                null -> send(HeartbeatEvent)
                            }
                        }

                        delay(checkInterval)
                    }
                } catch (e: CancellationException) {
                    logger.info("Events from queue $id collection was cancelled")
                }
            }
        }
    }
}

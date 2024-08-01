package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Event
import io.github.vinicreis.pubsub.server.core.model.data.HeartbeatEvent
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SubscriberManagerImpl(
    private val coroutineContext: CoroutineContext,
    private val checkInterval: Duration = 1.seconds,
    private val queueRepository: QueueRepository,
    private val eventsRepository: EventsRepository,
    private val textMessageRepository: TextMessageRepository,
    private val logger: Logger = Logger.getLogger(SubscriberManagerImpl::class.java.simpleName)
) : SubscriberManagerService {
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val subscribers = mutableListOf<ProducerScope<Event>>()
    private val channels = ConcurrentHashMap<UUID, Channel<Event>>()
    private val jobs = ConcurrentHashMap<UUID, Job>()

    init {
        coroutineScope.launch {
            when (val result = queueRepository.getAll()) {
                is QueueRepository.Result.GetAll.Error -> TODO()
                is QueueRepository.Result.GetAll.Success -> result.queues.forEach { queue ->
                    channels[queue.id] = Channel(Channel.UNLIMITED)
                    jobs[queue.id] = queue.collect()
                }
            }
        }
    }

    override fun subscribe(queue: Queue): Flow<Event> = channelFlow {
        subscribers.add(this)

        channels[queue.id]!!.receiveAsFlow().collect { send(it) }

        awaitClose {
            subscribers.remove(this)
            subscribers.ifEmpty {
                jobs[queue.id]?.cancel()
            }
        }
    }.onStart { queue.collect() }

    private suspend fun Queue.collect(): Job = coroutineScope.launch {
        while (true) {
            println("Consuming events from queue $id")
            when (val result = eventsRepository.consume(id)) {
                EventsRepository.Result.Consume.None -> channels[id]?.send(HeartbeatEvent)
                is EventsRepository.Result.Consume.Fail -> channels[id]?.send(HeartbeatEvent)
                is EventsRepository.Result.Consume.Success -> channels[id]?.send(result.event)
            }

            delay(checkInterval)
        }
    }
}

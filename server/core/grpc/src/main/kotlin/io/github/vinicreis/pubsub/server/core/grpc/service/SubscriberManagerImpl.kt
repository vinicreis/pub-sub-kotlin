package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import io.github.vinicreis.pubsub.server.core.model.data.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
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
    private val subscribers = ConcurrentHashMap<UUID, ConcurrentHashMap<Int, FlowCollector<TextMessage>>>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)

    init {
        coroutineScope.launch { collectEvents() }
    }

    override fun subscribe(queue: Queue): Flow<TextMessage> {
        return flow { subscribers.getOrPut(queue.id) { ConcurrentHashMap() }[this.hashCode()] = this }
            .onCompletion { subscribers[queue.id]?.remove(this.hashCode()) }
    }

    private fun <E> Collection<E>.choose(): E? = takeIf { isNotEmpty() }?.elementAt(Random().nextInt(size))

    private suspend fun collectEvents() {
        while(true) {
            delay(checkInterval)

            when (val result = eventsRepository.consume()) {
                is EventsRepository.Result.Consume.None -> Unit

                is EventsRepository.Result.Consume.Fail -> {
                    logger.severe("Failed to consume event")
                    result.e.printStackTrace()
                }

                is EventsRepository.Result.Consume.Success -> when(val event = result.event) {
                    is TextMessageReceivedEvent -> when(event.queue.type) {
                        Queue.Type.SIMPLE -> subscribers[event.queue.id]?.values?.choose()?.emit(event.textMessage)
                        Queue.Type.MULTIPLE -> subscribers[event.queue.id]?.values?.forEach { producerScope ->
                            producerScope.emit(event.textMessage)
                        }
                    }
                }
            }
        }
    }
}

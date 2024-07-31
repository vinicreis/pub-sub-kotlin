package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class SubscriberManagerImpl(
    private val textMessageRepository: TextMessageRepository,
    coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger(SubscriberManagerImpl::class.java.simpleName)
) : SubscriberManagerService {
    private val subscribers = ConcurrentHashMap<Queue, ConcurrentHashMap<String, ProducerScope<TextMessage>>>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val jobs = ConcurrentHashMap<Queue, Job>()

    override fun subscribersCount(queue: Queue): Int = subscribers[queue]?.size ?: 0

    override fun subscribe(queue: Queue): Flow<TextMessage> = channelFlow {
        val subscriberId: String = UUID.randomUUID().toString()

        textMessageRepository.subscribe(queue).let { result ->
            when (result) {
                TextMessageRepository.Result.Subscribe.QueueNotFound ->
                    close(IllegalStateException("Queue ${queue.id} not found"))

                is TextMessageRepository.Result.Subscribe.Error ->
                    close(RuntimeException("Failed to get queue ${queue.id} messages queue", result.e))

                is TextMessageRepository.Result.Subscribe.Success -> {
                    subscribers.getOrPut(queue) { ConcurrentHashMap() }[subscriberId] = this
                }
            }
        }

        awaitClose {
            logger.info("Subscriber $subscriberId completed!")
            subscribers[queue]?.remove(subscriberId)
            jobs[queue]?.takeIf { subscribers[queue].isNullOrEmpty() }?.cancel(
                CancellationException("No more subscribers on queue ${queue.id}")
            )
        }
    }.onStart { queue.collectMessagesIfNotStarted() }

    private fun <E> Collection<E>.choose(): E? = takeIf { isNotEmpty() }?.elementAt(Random().nextInt(size))

    private fun Queue.collectMessagesIfNotStarted() {
        jobs.getOrPut(this@collectMessagesIfNotStarted) {
            coroutineScope.launch {
                when (val result = textMessageRepository.subscribe(this@collectMessagesIfNotStarted)) {
                    is TextMessageRepository.Result.Subscribe.Error ->
                        subscribers[this@collectMessagesIfNotStarted]?.values?.forEach {
                            it.close(CancellationException("Failed to get queue messages"))
                        }

                    TextMessageRepository.Result.Subscribe.QueueNotFound ->
                        subscribers[this@collectMessagesIfNotStarted]?.values?.forEach {
                            it.close(CancellationException("Queue not found on messages queue"))
                        }

                    is TextMessageRepository.Result.Subscribe.Success -> result.messages
                        .onEach { logger.fine("Received message: ${it.content}") }
                        .onCompletion { cause ->
                            cause?.let {
                                logger.severe("Queue $id message queue failed! Closing subscribers...")
                            } ?: logger.info("Queue $id message queue finished! Closing subscribers...")

                            subscribers[this@collectMessagesIfNotStarted]?.forEach { (_, producerScope) ->
                                cause?.let {
                                    producerScope.close(CancellationException("Message queue finished by error!", it))
                                } ?: producerScope.close()
                            }
                        }.collect { message ->
                            when (type) {
                                Queue.Type.SIMPLE -> subscribers[this@collectMessagesIfNotStarted]
                                    ?.values?.choose()?.send(message)
                                Queue.Type.MULTIPLE -> subscribers[this@collectMessagesIfNotStarted]
                                    ?.values?.forEach { producerScope -> producerScope.send(message) }
                            }
                        }
                }
            }
        }
    }
}

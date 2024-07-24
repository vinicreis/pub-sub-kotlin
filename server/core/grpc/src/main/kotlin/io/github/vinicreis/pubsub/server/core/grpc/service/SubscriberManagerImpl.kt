package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class SubscriberManagerImpl(
    private val messageRepository: MessageRepository,
    coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger(SubscriberManagerImpl::class.java.simpleName)
) : SubscriberManagerService {
    private val subscribers = ConcurrentHashMap<Channel, ConcurrentHashMap<String, ProducerScope<Message>>>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val jobs = ConcurrentHashMap<Channel, Job>()

    override fun subscribersCount(channel: Channel): Int = subscribers[channel]?.size ?: 0

    override fun subscribe(channel: Channel): Flow<Message> = channelFlow {
        val subscriberId: String = UUID.randomUUID().toString()

        messageRepository.subscribe(channel).let {
            when (it) {
                MessageRepository.Result.Subscribe.QueueNotFound ->
                    close(IllegalStateException("Channel ${channel.id} not found"))

                is MessageRepository.Result.Subscribe.Error ->
                    close(RuntimeException("Failed to get channel ${channel.id} messages queue", it.e))

                is MessageRepository.Result.Subscribe.Success -> {
                    subscribers.getOrPut(channel) { ConcurrentHashMap() }[subscriberId] = this
                }
            }
        }

        awaitClose {
            logger.info("Subscriber $subscriberId completed!")
            subscribers[channel]?.remove(subscriberId)
            jobs[channel]?.takeIf { subscribers[channel].isNullOrEmpty() }?.cancel(
                CancellationException("No more subscribers on channel ${channel.id}")
            )
        }
    }.onStart { channel.collectMessagesIfNotStarted() }

    private fun <E> Collection<E>.choose(): E? = takeIf { isNotEmpty() }?.elementAt(Random().nextInt(size))

    private fun Channel.collectMessagesIfNotStarted() {
        jobs.getOrPut(this@collectMessagesIfNotStarted) {
            coroutineScope.launch {
                when (val result = messageRepository.subscribe(this@collectMessagesIfNotStarted)) {
                    is MessageRepository.Result.Subscribe.Error ->
                        subscribers[this@collectMessagesIfNotStarted]?.values?.forEach {
                            it.close(CancellationException("Failed to get channel messages queue"))
                        }

                    MessageRepository.Result.Subscribe.QueueNotFound ->
                        subscribers[this@collectMessagesIfNotStarted]?.values?.forEach {
                            it.close(CancellationException("Channel not found on messages queue"))
                        }

                    is MessageRepository.Result.Subscribe.Success -> result.messages.receiveAsFlow()
                        .onEach { logger.fine("Received message: ${it.content}") }
                        .onCompletion { cause ->
                            cause?.let {
                                logger.severe("Channel $id message queue failed! Closing subscribers...")
                            } ?: logger.info("Channel $id message queue finished! Closing subscribers...")

                            subscribers[this@collectMessagesIfNotStarted]?.forEach { (_, producerScope) ->
                                cause?.let {
                                    producerScope.close(CancellationException("Message queue finished by error!", it))
                                } ?: producerScope.close()
                            }
                        }.collect { message ->
                            when (type) {
                                Channel.Type.SIMPLE -> subscribers[this@collectMessagesIfNotStarted]
                                    ?.values?.choose()?.send(message)
                                Channel.Type.MULTIPLE -> subscribers[this@collectMessagesIfNotStarted]
                                    ?.values?.forEach { producerScope -> producerScope.send(message) }
                            }
                        }
                }
            }
        }
    }
}

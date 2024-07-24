package io.github.vinicreis.pubsub.server.channel.infra.service

import io.github.vinicreis.domain.model.ResultOuterClass
import io.github.vinicreis.domain.server.channel.model.Subscription
import io.github.vinicreis.domain.server.channel.model.TextMessageOuterClass.TextMessage
import io.github.vinicreis.domain.server.channel.request.AddRequest
import io.github.vinicreis.domain.server.channel.request.ListRequest
import io.github.vinicreis.domain.server.channel.request.PeekRequest
import io.github.vinicreis.domain.server.channel.request.PublishMultipleRequest
import io.github.vinicreis.domain.server.channel.request.PublishSingleRequest
import io.github.vinicreis.domain.server.channel.request.RemoveByIdRequest
import io.github.vinicreis.domain.server.channel.request.SubscribeRequest
import io.github.vinicreis.domain.server.channel.response.AddResponse
import io.github.vinicreis.domain.server.channel.response.ListResponse
import io.github.vinicreis.domain.server.channel.response.PeekResponse
import io.github.vinicreis.domain.server.channel.response.PublishResponse
import io.github.vinicreis.domain.server.channel.response.RemoveByIdResponse
import io.github.vinicreis.domain.server.channel.response.SubscribeResponse
import io.github.vinicreis.domain.server.channel.response.addResponse
import io.github.vinicreis.domain.server.channel.response.listResponse
import io.github.vinicreis.domain.server.channel.response.peekResponse
import io.github.vinicreis.domain.server.channel.response.publishResponse
import io.github.vinicreis.domain.server.channel.response.removeByIdResponse
import io.github.vinicreis.domain.server.channel.response.subscribeResponse
import io.github.vinicreis.domain.server.channel.service.ChannelServiceGrpcKt
import io.github.vinicreis.pubsub.server.channel.domain.mapper.asDomain
import io.github.vinicreis.pubsub.server.channel.domain.mapper.asRemote
import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.channel.domain.repository.MessageRepository
import io.github.vinicreis.pubsub.server.channel.domain.service.ChannelService
import io.github.vinicreis.pubsub.server.channel.domain.service.SubscriberManager
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class ChannelServiceGRPC(
    private val port: Int,
    coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger(ChannelServiceGRPC::class.java.name),
    private val channelRepository: ChannelRepository,
    private val messageRepository: MessageRepository,
    private val subscriberManager: SubscriberManager,
) : ChannelService, ChannelServiceGrpcKt.ChannelServiceCoroutineImplBase(coroutineContext) {
    private val credentials = InsecureServerCredentials.create()
    private val server = Grpc.newServerBuilderForPort(port, credentials)
        .addService(this)
        .build()

    override fun start() {
        logger.info("Starting server...")
        server.start()
    }

    override fun blockUntilShutdown() {
        logger.info("Listening on port $port...")
        server.awaitTermination()
    }

    override suspend fun add(request: AddRequest): AddResponse {
        return try {
            channelRepository.add(Channel(request.id, request.name, request.type.asDomain)).let { result ->
                when (result) {
                    is ChannelRepository.Result.Add.Error -> addResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is ChannelRepository.Result.Add.AlreadyFound -> addResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.id} already exists"
                    }

                    is ChannelRepository.Result.Add.Success -> addResponse {
                        this.result = ResultOuterClass.Result.SUCCESS
                        this.channel = result.channel.asRemote
                    }
                }
            }
        } catch (t: Throwable) {
            logger.severe("Failed to process add request: $t")
            addResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    override suspend fun list(request: ListRequest): ListResponse {
        return try {
            channelRepository.getAll().let { result ->
                when (result) {
                    is ChannelRepository.Result.GetAll.Success -> listResponse {
                        this.result = ResultOuterClass.Result.SUCCESS
                        result.channels.forEach { channel -> channels.add(channel.asRemote) }
                    }

                    is ChannelRepository.Result.GetAll.Error -> listResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        this.message = result.e.message ?: "Something went wrong"
                    }
                }
            }
        } catch (t: Throwable) {
            logger.severe("Failed to process add request: $t")
            listResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    override suspend fun removeById(request: RemoveByIdRequest): RemoveByIdResponse {
        return try {
            channelRepository.removeById(request.id).let { result ->
                when (result) {
                    ChannelRepository.Result.Remove.NotFound -> removeByIdResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.id} not found"
                    }

                    is ChannelRepository.Result.Remove.Error -> removeByIdResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is ChannelRepository.Result.Remove.Success -> {
                        when(val messageCloseResult = messageRepository.remove(channel = result.channel)) {
                            is MessageRepository.Result.Remove.Error -> removeByIdResponse {
                                this.result = ResultOuterClass.Result.ERROR
                                message = messageCloseResult.e.message ?: "Something went wrong..."
                            }

                            MessageRepository.Result.Remove.QueueNotFound -> removeByIdResponse {
                                this.result = ResultOuterClass.Result.ERROR
                                message = "Channel ${request.id} message queue not found"
                            }

                            is MessageRepository.Result.Remove.Success -> removeByIdResponse {
                                this.result = ResultOuterClass.Result.SUCCESS
                                id = request.id
                                this.channel = result.channel.asRemote
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            logger.severe("Failed to proccess remove by id request: $t")
            removeByIdResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    override suspend fun publishSingle(request: PublishSingleRequest): PublishResponse {
        return try {
            channelRepository.getById(request.channelId).let { result ->
                when (result) {
                    ChannelRepository.Result.GetById.NotFound -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.channelId} not found"
                    }

                    is ChannelRepository.Result.GetById.Error -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is ChannelRepository.Result.GetById.Success -> {
                        messageRepository.add(result.channel, request.content.asDomain)

                        publishResponse {
                            this.result = ResultOuterClass.Result.SUCCESS
                            channel = result.channel.asRemote
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            logger.severe("Failed to process publish single request: $e")
            e.printStackTrace()

            publishResponse {
                result = ResultOuterClass.Result.ERROR
                message = e.message ?: "Something went wrong..."
            }
        }
    }

    override suspend fun publishMultiple(request: PublishMultipleRequest): PublishResponse {
        return try {
            channelRepository.getById(request.channelId).let { result ->
                when (result) {
                    ChannelRepository.Result.GetById.NotFound -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.channelId} not found"
                    }

                    is ChannelRepository.Result.GetById.Error -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is ChannelRepository.Result.GetById.Success -> {
                        messageRepository.addAll(result.channel, request.contentList.map(TextMessage::asDomain))

                        publishResponse {
                            this.result = ResultOuterClass.Result.SUCCESS
                            channel = result.channel.asRemote
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            logger.severe("Failed to process publish single request: $e")
            e.printStackTrace()

            publishResponse {
                result = ResultOuterClass.Result.ERROR
                message = e.message ?: "Something went wrong..."
            }
        }
    }

    override fun subscribe(request: SubscribeRequest): Flow<SubscribeResponse> {
        return channelFlow {
            subscribeResponse {
                status = Subscription.SubscriptionStatus.PROCESSING
            }.also { send(it) }

            channelRepository.getById(request.channelId).let { result ->
                when (result) {
                    ChannelRepository.Result.GetById.NotFound -> subscribeResponse {
                        status = Subscription.SubscriptionStatus.FINISHED
                        message = "Channel ${request.channelId} not found"
                    }.also { send(it); close() }

                    is ChannelRepository.Result.GetById.Error -> subscribeResponse {
                        status = Subscription.SubscriptionStatus.FINISHED
                        message = result.e.message ?: "Something went wrong..."
                    }.also { send(it); close() }

                    is ChannelRepository.Result.GetById.Success ->
                        subscriberManager.subscribe(result.channel)
                            .onStart {
                                subscribeResponse {
                                    status = Subscription.SubscriptionStatus.ACTIVE
                                    channel = result.channel.asRemote
                                }.also { send(it) }
                            }.onCompletion {
                                subscribeResponse {
                                    status = Subscription.SubscriptionStatus.FINISHED
                                    channel = result.channel.asRemote
                                }.also { send(it); close() }
                            }.collect { pendingMessage ->
                                subscribeResponse {
                                    status = Subscription.SubscriptionStatus.UPDATE
                                    channel = result.channel.asRemote
                                    content = pendingMessage.asRemote
                                }.also { send(it) }
                            }
                }
            }
        }
    }

    override suspend fun peek(request: PeekRequest): PeekResponse {
        return try {
            channelRepository.getById(request.channelId).let { result ->
                when (result) {
                    ChannelRepository.Result.GetById.NotFound -> peekResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.channelId} not found"
                    }

                    is ChannelRepository.Result.GetById.Error -> peekResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is ChannelRepository.Result.GetById.Success -> {
                        val timeout = request.timeoutSeconds.takeIf { it > 0 } ?: Long.MAX_VALUE

                        try {
                            withTimeout(timeout.seconds) {
                                subscriberManager.subscribe(result.channel).first().let { pendingMessage ->
                                    peekResponse {
                                        this.result = ResultOuterClass.Result.SUCCESS
                                        channel = result.channel.asRemote
                                        this.content = pendingMessage.asRemote
                                    }
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            peekResponse {
                                this.result = ResultOuterClass.Result.ERROR
                                channel = result.channel.asRemote
                                this.message = "Peek last message timeout"
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            logger.severe("Failed to process publish single request: $e")
            e.printStackTrace()

            peekResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }
}

package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.domain.server.core.model.data.ResultOuterClass
import io.github.vinicreis.domain.server.core.model.data.SubscriptionStatusOuterClass
import io.github.vinicreis.domain.server.core.model.data.TextMessageOuterClass
import io.github.vinicreis.domain.server.core.model.request.AddRequest
import io.github.vinicreis.domain.server.core.model.request.ListRequest
import io.github.vinicreis.domain.server.core.model.request.PeekRequest
import io.github.vinicreis.domain.server.core.model.request.PublishMultipleRequest
import io.github.vinicreis.domain.server.core.model.request.PublishSingleRequest
import io.github.vinicreis.domain.server.core.model.request.RemoveByIdRequest
import io.github.vinicreis.domain.server.core.model.request.SubscribeRequest
import io.github.vinicreis.domain.server.core.model.response.AddResponse
import io.github.vinicreis.domain.server.core.model.response.ListResponse
import io.github.vinicreis.domain.server.core.model.response.PeekResponse
import io.github.vinicreis.domain.server.core.model.response.PublishResponse
import io.github.vinicreis.domain.server.core.model.response.RemoveByIdResponse
import io.github.vinicreis.domain.server.core.model.response.SubscribeResponse
import io.github.vinicreis.domain.server.core.model.response.addResponse
import io.github.vinicreis.domain.server.core.model.response.listResponse
import io.github.vinicreis.domain.server.core.model.response.peekResponse
import io.github.vinicreis.domain.server.core.model.response.publishResponse
import io.github.vinicreis.domain.server.core.model.response.removeByIdResponse
import io.github.vinicreis.domain.server.core.model.response.subscribeResponse
import io.github.vinicreis.domain.server.core.service.ChannelServiceGrpcKt
import io.github.vinicreis.pubsub.server.core.extension.asUuid
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asDomain
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asRemote
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.service.ChannelService
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
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

class ChannelServiceGrpc(
    private val port: Int,
    coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger(ChannelServiceGrpc::class.java.name),
    private val channelRepository: ChannelRepository,
    private val messageRepository: MessageRepository,
    private val subscriberManagerService: SubscriberManagerService,
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
            channelRepository.add(request.channel.asDomain).let { result ->
                when (result) {
                    is ChannelRepository.Result.Add.Error -> addResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is ChannelRepository.Result.Add.AlreadyFound -> addResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.channel.asDomain} already exists"
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
            channelRepository.removeById(request.id.asUuid).let { result ->
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
                        when (val messageCloseResult = messageRepository.remove(channel = result.channel)) {
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

    private fun MessageRepository.Result.Add.toPublishResponse(channel: Channel) = when (this) {
        is MessageRepository.Result.Add.QueueNotFound -> publishResponse {
            this.result = ResultOuterClass.Result.ERROR
            message = "Channel ${channel.id} message queue not found"
        }

        is MessageRepository.Result.Add.Error -> publishResponse {
            this.result = ResultOuterClass.Result.ERROR
            message = e.message ?: "Something went wrong..."
        }

        MessageRepository.Result.Add.Success -> publishResponse {
            this.result = ResultOuterClass.Result.SUCCESS
            this.channel = channel.asRemote
        }
    }

    override suspend fun publishSingle(request: PublishSingleRequest): PublishResponse {
        return try {
            channelRepository.getById(request.channelId.asUuid).let { result ->
                when (result) {
                    ChannelRepository.Result.GetById.NotFound -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.channelId} not found"
                    }

                    is ChannelRepository.Result.GetById.Error -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is ChannelRepository.Result.GetById.Success ->
                        messageRepository.add(
                            channel = result.channel,
                            message = request.content.asDomain
                        ).toPublishResponse(result.channel)
                }
            }
        } catch (e: Throwable) {
            logger.severe("Failed to process publish single request: $e")
            e.printStackTrace()

            publishResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    override suspend fun publishMultiple(request: PublishMultipleRequest): PublishResponse {
        return try {
            channelRepository.getById(request.channelId.asUuid).let { result ->
                when (result) {
                    ChannelRepository.Result.GetById.NotFound -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.channelId} not found"
                    }

                    is ChannelRepository.Result.GetById.Error -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is ChannelRepository.Result.GetById.Success ->
                        messageRepository.addAll(
                            channel = result.channel,
                            messages = request.contentList.map(TextMessageOuterClass.TextMessage::asDomain)
                        ).toPublishResponse(result.channel)
                }
            }
        } catch (e: Throwable) {
            logger.severe("Failed to process publish single request: $e")
            e.printStackTrace()

            publishResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    override fun subscribe(request: SubscribeRequest): Flow<SubscribeResponse> {
        return channelFlow {
            subscribeResponse {
                status = SubscriptionStatusOuterClass.SubscriptionStatus.PROCESSING
            }.also { send(it) }

            channelRepository.getById(request.channelId.asUuid).let { result ->
                when (result) {
                    ChannelRepository.Result.GetById.NotFound -> subscribeResponse {
                        status = SubscriptionStatusOuterClass.SubscriptionStatus.FINISHED
                        message = "Channel ${request.channelId} not found"
                    }.also { send(it); close() }

                    is ChannelRepository.Result.GetById.Error -> subscribeResponse {
                        status = SubscriptionStatusOuterClass.SubscriptionStatus.FINISHED
                        message = result.e.message ?: "Something went wrong..."
                    }.also { send(it); close() }

                    is ChannelRepository.Result.GetById.Success ->
                        subscriberManagerService.subscribe(result.channel)
                            .onStart {
                                subscribeResponse {
                                    status = SubscriptionStatusOuterClass.SubscriptionStatus.ACTIVE
                                    channel = result.channel.asRemote
                                }.also { send(it) }
                            }.onCompletion {
                                subscribeResponse {
                                    status = SubscriptionStatusOuterClass.SubscriptionStatus.FINISHED
                                    channel = result.channel.asRemote
                                }.also { send(it); close() }
                            }.collect { pendingMessage ->
                                subscribeResponse {
                                    status = SubscriptionStatusOuterClass.SubscriptionStatus.UPDATE
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
            channelRepository.getById(request.channelId.asUuid).let { result ->
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
                                subscriberManagerService.subscribe(result.channel).first().let { pendingMessage ->
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

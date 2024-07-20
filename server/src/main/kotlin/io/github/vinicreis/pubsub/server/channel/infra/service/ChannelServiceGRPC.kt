package io.github.vinicreis.pubsub.server.channel.infra.service

import io.github.vinicreis.domain.model.ResultOuterClass
import io.github.vinicreis.domain.server.channel.model.ChannelOuterClass
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
import io.github.vinicreis.pubsub.server.channel.domain.model.Queue
import io.github.vinicreis.pubsub.server.channel.domain.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.channel.domain.service.ChannelService
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class ChannelServiceGRPC(
    private val port: Int,
    coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger("ChannelService"),
    private val channelRepository: ChannelRepository,
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
            when (request.type) {
                ChannelOuterClass.Channel.Type.UNRECOGNIZED, null -> error("Unknown channel type")

                ChannelOuterClass.Channel.Type.SIMPLE -> Queue.Simple(
                    id = request.id,
                    name = request.name ?: request.id,
                )

                ChannelOuterClass.Channel.Type.MULTIPLE -> Queue.Multiple(
                    id = request.id,
                    name = request.name ?: request.id,
                )
            }.let { channel ->
                when (val result = channelRepository.add(channel)) {
                    is ChannelRepository.Result.Add.Error -> addResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.message
                    }

                    is ChannelRepository.Result.Add.AlreadyFound -> addResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Channel ${request.id} already exists"
                        this.channel = result.queue.asRemote
                    }

                    is ChannelRepository.Result.Add.Success -> addResponse {
                        this.result = ResultOuterClass.Result.SUCCESS
                        this.channel = result.queue.asRemote
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
                        result.queues.forEach { channel -> channels.add(channel.asRemote) }
                    }

                    is ChannelRepository.Result.GetAll.Error -> listResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        this.message = result.message
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
                        message = result.message
                    }

                    is ChannelRepository.Result.Remove.Success -> removeByIdResponse {
                        this.result = ResultOuterClass.Result.SUCCESS
                        id = request.id
                        this.channel = result.queue.asRemote
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
        return channelRepository.getById(request.channelId).let { result ->
            when (result) {
                ChannelRepository.Result.GetById.NotFound -> publishResponse {
                    this.result = ResultOuterClass.Result.ERROR
                    message = "Channel ${request.channelId} not found"
                }

                is ChannelRepository.Result.GetById.Error -> publishResponse {
                    this.result = ResultOuterClass.Result.ERROR
                    message = result.message
                }

                is ChannelRepository.Result.GetById.Success -> when (result.queue) {
                    is Queue.Simple -> TODO("Not implemented")

                    is Queue.Multiple -> {
                        result.queue.post(request.content.asDomain)

                        publishResponse {
                            this.result = ResultOuterClass.Result.SUCCESS
                            channel = result.queue.asRemote
                        }
                    }
                }
            }
        }
    }

    override suspend fun publishMultiple(request: PublishMultipleRequest): PublishResponse {
        return channelRepository.getById(request.channelId).let { result ->
            when (result) {
                ChannelRepository.Result.GetById.NotFound -> publishResponse {
                    this.result = ResultOuterClass.Result.ERROR
                    message = "Channel ${request.channelId} not found"
                }

                is ChannelRepository.Result.GetById.Error -> publishResponse {
                    this.result = ResultOuterClass.Result.ERROR
                    message = result.message
                }

                is ChannelRepository.Result.GetById.Success -> when (result.queue) {
                    is Queue.Simple -> TODO("Not implemented")

                    is Queue.Multiple -> {
                        request.contentList.map(TextMessage::asDomain).forEach { result.queue.post(it) }

                        publishResponse {
                            this.result = ResultOuterClass.Result.SUCCESS
                            channel = result.queue.asRemote
                        }
                    }
                }
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
                        message = result.message
                    }.also { send(it); close() }

                    is ChannelRepository.Result.GetById.Success -> when(result.queue) {
                        is Queue.Simple -> TODO("Not implemented")

                        is Queue.Multiple -> {
                            result.queue.messages
                                .onStart {
                                    subscribeResponse {
                                        status = Subscription.SubscriptionStatus.ACTIVE
                                        channel = result.queue.asRemote
                                    }.also { send(it) }
                                }.onCompletion {
                                    subscribeResponse {
                                        status = Subscription.SubscriptionStatus.FINISHED
                                        channel = result.queue.asRemote
                                    }.also { send(it); close() }
                                }.collect { message ->
                                    subscribeResponse {
                                        status = Subscription.SubscriptionStatus.UPDATE
                                        channel = result.queue.asRemote
                                        content = message.asRemote
                                    }.also { send(it) }
                                }
                        }
                    }
                }
            }
        }
    }

    override suspend fun peek(request: PeekRequest): PeekResponse {
        return channelRepository.getById(request.channelId).let { result ->
            when (result) {
                ChannelRepository.Result.GetById.NotFound -> peekResponse {
                    this.result = ResultOuterClass.Result.ERROR
                    message = "Channel ${request.channelId} not found"
                }

                is ChannelRepository.Result.GetById.Error -> peekResponse {
                    this.result = ResultOuterClass.Result.ERROR
                    message = result.message
                }

                is ChannelRepository.Result.GetById.Success -> when(result.queue) {
                    is Queue.Simple -> TODO("Not implemented")

                    is Queue.Multiple -> {
                        val timeout = request.timeoutSeconds.takeIf { it > 0 } ?: Long.MAX_VALUE

                        try {
                            withTimeout(timeout.seconds) {
                                result.queue.nextMessage.last().let { message ->
                                    peekResponse {
                                        this.result = ResultOuterClass.Result.SUCCESS
                                        channel = result.queue.asRemote
                                        this.content = message.asRemote
                                    }
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            peekResponse {
                                this.result = ResultOuterClass.Result.ERROR
                                channel = result.queue.asRemote
                                this.message = "Peek last message timeout"
                            }
                        }
                    }
                }
            }
        }
    }
}

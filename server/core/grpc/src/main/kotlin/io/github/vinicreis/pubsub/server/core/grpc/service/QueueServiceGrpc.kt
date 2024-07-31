package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.domain.server.core.model.data.ResultOuterClass
import io.github.vinicreis.domain.server.core.model.data.SubscriptionEventOuterClass.SubscriptionEvent
import io.github.vinicreis.domain.server.core.model.data.TextMessageOuterClass
import io.github.vinicreis.domain.server.core.model.request.ListRequestOuterClass.ListRequest
import io.github.vinicreis.domain.server.core.model.request.PollRequestOuterClass.PollRequest
import io.github.vinicreis.domain.server.core.model.request.PostMultipleRequestOuterClass.PostMultipleRequest
import io.github.vinicreis.domain.server.core.model.request.PostSingleRequestOuterClass.PostSingleRequest
import io.github.vinicreis.domain.server.core.model.request.PublishRequestOuterClass.PublishRequest
import io.github.vinicreis.domain.server.core.model.request.RemoveRequestOuterClass.RemoveRequest
import io.github.vinicreis.domain.server.core.model.request.SubscribeRequestOuterClass.SubscribeRequest
import io.github.vinicreis.domain.server.core.model.response.ListResponseOuterClass.ListResponse
import io.github.vinicreis.domain.server.core.model.response.PollResponseOuterClass.PollResponse
import io.github.vinicreis.domain.server.core.model.response.PostResponseOuterClass.PostResponse
import io.github.vinicreis.domain.server.core.model.response.PublishResponseOuterClass.PublishResponse
import io.github.vinicreis.domain.server.core.model.response.RemoveResponseOuterClass.RemoveResponse
import io.github.vinicreis.domain.server.core.model.response.SubscribeResponseOuterClass.SubscribeResponse
import io.github.vinicreis.domain.server.core.model.response.listResponse
import io.github.vinicreis.domain.server.core.model.response.pollResponse
import io.github.vinicreis.domain.server.core.model.response.postResponse
import io.github.vinicreis.domain.server.core.model.response.publishResponse
import io.github.vinicreis.domain.server.core.model.response.removeResponse
import io.github.vinicreis.domain.server.core.model.response.subscribeResponse
import io.github.vinicreis.domain.server.core.service.QueueServiceGrpcKt
import io.github.vinicreis.pubsub.server.core.extension.asUuid
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asDomain
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asRemote
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.service.QueueService
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
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

class QueueServiceGrpc(
    private val port: Int,
    coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger(QueueServiceGrpc::class.java.name),
    private val queueRepository: QueueRepository,
    private val textMessageRepository: TextMessageRepository,
    private val subscriberManagerService: SubscriberManagerService,
) : QueueService, QueueServiceGrpcKt.QueueServiceCoroutineImplBase(coroutineContext) {
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

    override suspend fun publish(request: PublishRequest): PublishResponse {
        return try {
            queueRepository.add(request.queue.asDomain).let { result ->
                when (result) {
                    is QueueRepository.Result.Add.Error -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is QueueRepository.Result.Add.AlreadyFound -> publishResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Queue ${request.queue.asDomain} already exists"
                    }

                    is QueueRepository.Result.Add.Success -> publishResponse {
                        this.result = ResultOuterClass.Result.SUCCESS
                        this.queue = result.queue.asRemote
                    }
                }
            }
        } catch (t: Throwable) {
            logger.severe("Failed to process add request: $t")
            publishResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    override suspend fun list(request: ListRequest): ListResponse {
        return try {
            queueRepository.getAll().let { result ->
                when (result) {
                    is QueueRepository.Result.GetAll.Success -> listResponse {
                        this.result = ResultOuterClass.Result.SUCCESS
                        result.queues.forEach { queue -> queues.add(queue.asRemote) }
                    }

                    is QueueRepository.Result.GetAll.Error -> listResponse {
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

    override suspend fun remove(request: RemoveRequest): RemoveResponse {
        return try {
            queueRepository.removeById(request.id.asUuid).let { result ->
                when (result) {
                    QueueRepository.Result.Remove.NotFound -> removeResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Queue ${request.id} not found"
                    }

                    is QueueRepository.Result.Remove.Error -> removeResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is QueueRepository.Result.Remove.Success -> {
                        when (val messageCloseResult = textMessageRepository.remove(queue = result.queue)) {
                            is TextMessageRepository.Result.Remove.Error -> removeResponse {
                                this.result = ResultOuterClass.Result.ERROR
                                message = messageCloseResult.e.message ?: "Something went wrong..."
                            }

                            is TextMessageRepository.Result.Remove.Success -> removeResponse {
                                this.result = ResultOuterClass.Result.SUCCESS
                                this.queue = result.queue.asRemote
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            logger.severe("Failed to proccess remove by id request: $t")
            removeResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    private fun TextMessageRepository.Result.Add.toPostResponse(queue: Queue) = when (this) {
        is TextMessageRepository.Result.Add.Error -> postResponse {
            this.result = ResultOuterClass.Result.ERROR
            message = e.message ?: "Something went wrong..."
        }

        TextMessageRepository.Result.Add.Success -> postResponse {
            this.result = ResultOuterClass.Result.SUCCESS
            this.queue = queue.asRemote
        }
    }

    override suspend fun postSingle(request: PostSingleRequest): PostResponse {
        return try {
            queueRepository.getById(request.queueId.asUuid).let { result ->
                when (result) {
                    QueueRepository.Result.GetById.NotFound -> postResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Queue ${request.queueId} not found"
                    }

                    is QueueRepository.Result.GetById.Error -> postResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is QueueRepository.Result.GetById.Success ->
                        textMessageRepository.add(
                            queue = result.queue,
                            textMessages = listOf(request.content.asDomain)
                        ).toPostResponse(result.queue)
                }
            }
        } catch (e: Throwable) {
            logger.severe("Failed to process publish single request: $e")
            e.printStackTrace()

            postResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    override suspend fun postMultiple(request: PostMultipleRequest): PostResponse {
        return try {
            queueRepository.getById(request.queueId.asUuid).let { result ->
                when (result) {
                    QueueRepository.Result.GetById.NotFound -> postResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Queue ${request.queueId} not found"
                    }

                    is QueueRepository.Result.GetById.Error -> postResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is QueueRepository.Result.GetById.Success ->
                        textMessageRepository.add(
                            queue = result.queue,
                            textMessages = request.contentList.map(TextMessageOuterClass.TextMessage::asDomain)
                        ).toPostResponse(result.queue)
                }
            }
        } catch (e: Throwable) {
            logger.severe("Failed to process publish single request: $e")
            e.printStackTrace()

            postResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }

    override fun subscribe(request: SubscribeRequest): Flow<SubscribeResponse> {
        return channelFlow {
            subscribeResponse {
                event = SubscriptionEvent.PROCESSING
            }.also { send(it) }

            queueRepository.getById(request.queueId.asUuid).let { result ->
                when (result) {
                    QueueRepository.Result.GetById.NotFound -> subscribeResponse {
                        event = SubscriptionEvent.FINISHED
                        message = "Queue ${request.queueId} not found"
                    }.also { send(it); close() }

                    is QueueRepository.Result.GetById.Error -> subscribeResponse {
                        event = SubscriptionEvent.FINISHED
                        message = result.e.message ?: "Something went wrong..."
                    }.also { send(it); close() }

                    is QueueRepository.Result.GetById.Success ->
                        subscriberManagerService.subscribe(result.queue)
                            .onStart {
                                subscribeResponse {
                                    event = SubscriptionEvent.ACTIVE
                                    queue = result.queue.asRemote
                                }.also { send(it) }
                            }.onCompletion {
                                subscribeResponse {
                                    event = SubscriptionEvent.FINISHED
                                    queue = result.queue.asRemote
                                }.also { send(it); close() }
                            }.collect { pendingMessage ->
                                subscribeResponse {
                                    event = SubscriptionEvent.UPDATE
                                    queue = result.queue.asRemote
                                    content = pendingMessage.asRemote
                                }.also { send(it) }
                            }
                }
            }
        }
    }

    override suspend fun poll(request: PollRequest): PollResponse {
        return try {
            queueRepository.getById(request.queueId.asUuid).let { result ->
                when (result) {
                    QueueRepository.Result.GetById.NotFound -> pollResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = "Queue ${request.queueId} not found"
                    }

                    is QueueRepository.Result.GetById.Error -> pollResponse {
                        this.result = ResultOuterClass.Result.ERROR
                        message = result.e.message ?: "Something went wrong..."
                    }

                    is QueueRepository.Result.GetById.Success -> {
                        val timeout = request.timeoutSeconds.takeIf { it > 0 } ?: Long.MAX_VALUE

                        try {
                            withTimeout(timeout.seconds) {
                                subscriberManagerService.subscribe(result.queue).first().let { pendingMessage ->
                                    pollResponse {
                                        this.result = ResultOuterClass.Result.SUCCESS
                                        this.queue = result.queue.asRemote
                                        this.content = pendingMessage.asRemote
                                    }
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            pollResponse {
                                this.result = ResultOuterClass.Result.ERROR
                                this.queue = result.queue.asRemote
                                this.message = "Peek last message timeout"
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            logger.severe("Failed to process publish single request: $e")
            e.printStackTrace()

            pollResponse {
                result = ResultOuterClass.Result.ERROR
                message = "Something went wrong..."
            }
        }
    }
}

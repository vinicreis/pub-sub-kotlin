package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.domain.server.core.model.data.ResultOuterClass
import io.github.vinicreis.domain.server.core.model.data.SubscriptionEventOuterClass.SubscriptionEvent
import io.github.vinicreis.domain.server.core.model.request.ListRequestOuterClass.ListRequest
import io.github.vinicreis.domain.server.core.model.request.PollRequestOuterClass.PollRequest
import io.github.vinicreis.domain.server.core.model.request.PostRequestOuterClass.PostRequest
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
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.service.QueueService
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

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
                        when (val messageCloseResult = textMessageRepository.removeAll(queue = result.queue)) {
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

    override suspend fun post(request: PostRequest): PostResponse {
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
                            textMessages = request.contentList.map { it.asDomain(queue = result.queue) }
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

    context(ProducerScope<SubscribeResponse>)
    private fun Flow<TextMessageReceivedEvent>.notifyStart(queue: Queue): Flow<TextMessageReceivedEvent> = onStart {
        subscribeResponse {
            event = SubscriptionEvent.ACTIVE
            this.queue = queue.asRemote
        }.also { send(it) }
    }

    context(ProducerScope<SubscribeResponse>)
    private fun Flow<TextMessageReceivedEvent>.notifyFinished(
        queue: Queue,
    ): Flow<TextMessageReceivedEvent> = onCompletion { cause ->
        subscribeResponse {
            event = SubscriptionEvent.FINISHED
            this.queue = queue.asRemote
            message = cause?.message ?: "Subscription on queue ${queue.id} has closed"
        }.also { send(it) }

        close(cause)
    }

    context(ProducerScope<SubscribeResponse>)
    private fun Flow<TextMessageReceivedEvent>.catchAndClose(
        queue: Queue,
    ): Flow<TextMessageReceivedEvent> = catch { t ->
        subscribeResponse {
            event = SubscriptionEvent.FINISHED
            this.queue = queue.asRemote
            message = t.message ?: "Failed while processing subscription"
        }.also { send(it) }

        close(CancellationException("Failed while processing subscription", t))
    }

    context(ProducerScope<SubscribeResponse>)
    private suspend fun Flow<TextMessageReceivedEvent>.collectUpdating(queue: Queue) {
        collect { event ->
            subscribeResponse {
                this.event = SubscriptionEvent.UPDATE
                this.queue = queue.asRemote
                content = event.textMessage.asRemote
            }.also { send(it) }
        }
    }

    override fun subscribe(request: SubscribeRequest): Flow<SubscribeResponse> = channelFlow {
        subscribeResponse {
            event = SubscriptionEvent.PROCESSING
        }.also { send(it) }

        queueRepository.getById(request.queueId.asUuid).let { result ->
            when (result) {
                QueueRepository.Result.GetById.NotFound -> subscribeResponse {
                    event = SubscriptionEvent.FINISHED
                    message = "Queue ${request.queueId} not found"
                }.also { send(it); close(CancellationException("Queue ${request.queueId} not found")) }

                is QueueRepository.Result.GetById.Error -> subscribeResponse {
                    event = SubscriptionEvent.FINISHED
                    message = result.e.message ?: "Something went wrong..."
                }.also { send(it); close(CancellationException("Failed to fetch queue ${request.queueId}")) }

                is QueueRepository.Result.GetById.Success -> {
                    subscriberManagerService.subscribe(result.queue)
                        .filterIsInstance<TextMessageReceivedEvent>()
                        .notifyStart(result.queue)
                        .notifyFinished(result.queue)
                        .catchAndClose(result.queue)
                        .collectUpdating(result.queue)
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

                    is QueueRepository.Result.GetById.Success -> subscriberManagerService.subscribe(result.queue)
                        .filterIsInstance<TextMessageReceivedEvent>()
                        .first()
                        .let { event ->
                            pollResponse {
                                this.result = ResultOuterClass.Result.SUCCESS
                                this.queue = result.queue.asRemote
                                this.content = event.textMessage.asRemote
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

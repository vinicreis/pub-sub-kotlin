package io.github.vinicreis.pubsub.client.core.grpc.service

import io.github.vinicreis.domain.server.core.model.request.ListRequestOuterClass.ListRequest
import io.github.vinicreis.domain.server.core.model.request.pollRequest
import io.github.vinicreis.domain.server.core.model.request.postMultipleRequest
import io.github.vinicreis.domain.server.core.model.request.postSingleRequest
import io.github.vinicreis.domain.server.core.model.request.publishRequest
import io.github.vinicreis.domain.server.core.model.request.removeRequest
import io.github.vinicreis.domain.server.core.model.request.subscribeRequest
import io.github.vinicreis.domain.server.core.service.QueueServiceGrpcKt
import io.github.vinicreis.pubsub.client.core.grpc.mapper.asDomain
import io.github.vinicreis.pubsub.client.core.grpc.mapper.asRemote
import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.model.ServerInfo
import io.github.vinicreis.pubsub.client.core.model.SubscriptionEvent
import io.github.vinicreis.pubsub.client.core.model.TextMessage
import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient
import io.grpc.ManagedChannelBuilder
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class QueueServiceClientGrpc(
    override val serverInfo: ServerInfo,
    private val coroutineContext: CoroutineContext,
) : QueueServiceClient {
    private val channel = ManagedChannelBuilder
        .forAddress(serverInfo.address, serverInfo.port)
        .usePlaintext()
        .build()

    private val healthCheck by lazy {
        HealthGrpc.newStub(channel)
    }

    private val server by lazy {
        QueueServiceGrpcKt.QueueServiceCoroutineStub(channel)
    }

    override fun watch(): Flow<QueueServiceClient.Response.Health> = callbackFlow {
        val request = HealthCheckRequest.getDefaultInstance()
        val observer = object : StreamObserver<HealthCheckResponse> {
            override fun onNext(value: HealthCheckResponse) {
                when (value.status) {
                    HealthCheckResponse.ServingStatus.SERVING -> trySend(QueueServiceClient.Response.Health.Healthy)
                    HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN,
                    HealthCheckResponse.ServingStatus.UNRECOGNIZED,
                    HealthCheckResponse.ServingStatus.NOT_SERVING,
                    HealthCheckResponse.ServingStatus.UNKNOWN,
                    null -> trySend(QueueServiceClient.Response.Health.NotHealthy)
                }
            }

            override fun onError(t: Throwable) {
                close(t)
            }

            override fun onCompleted() {
                close()
            }
        }

        healthCheck.watch(request, observer)

        awaitClose {  }
    }

    override suspend fun list(): QueueServiceClient.Response.ListAll = withContext(coroutineContext) {
        server.list(ListRequest.getDefaultInstance()).asDomain
    }

    override suspend fun publish(queue: Queue) = withContext(coroutineContext) {
        server.publish(request = publishRequest { this.queue = queue.asRemote }).asDomain
    }

    override suspend fun post(queueId: String, vararg textMessage: TextMessage) = withContext(coroutineContext) {
        when (textMessage.size) {
            0 -> error("Message list cannot be empty")
            1 -> server.postSingle(
                request = postSingleRequest {
                    this.queueId = queueId
                    this.content = textMessage.first().asRemote
                }
            )

            else -> server.postMultiple(
                request = postMultipleRequest {
                    this.queueId = queueId
                    this.content.addAll(
                        textMessage.map(TextMessage::asRemote)
                    )
                }
            )
        }.asDomain
    }

    override fun subscribe(queueId: String): Flow<SubscriptionEvent> =
        server.subscribe(subscribeRequest { this.queueId = queueId }).map { it.asDomain }

    override suspend fun poll(
        queueId: String,
        timeoutSeconds: Long?
    ): QueueServiceClient.Response.Poll = withContext(coroutineContext) {
        server.poll(
            request = pollRequest {
                this.queueId = queueId
                timeoutSeconds?.also { this.timeoutSeconds = timeoutSeconds }
            }
        ).asDomain
    }

    override suspend fun remove(queueId: String): QueueServiceClient.Response.Remove =
        withContext(coroutineContext) {
            server.remove(
                request = removeRequest { this.id = queueId }
            ).asDomain
        }
}

package io.github.vinicreis.pubsub.client.core.grpc.service

import io.github.vinicreis.domain.server.core.model.request.ListRequestOuterClass.ListRequest
import io.github.vinicreis.domain.server.core.model.request.pollRequest
import io.github.vinicreis.domain.server.core.model.request.postRequest
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class QueueServiceClientGrpc(
    override val serverInfo: ServerInfo,
    private val coroutineContext: CoroutineContext,
) : QueueServiceClient {
    private val server by lazy {
        QueueServiceGrpcKt.QueueServiceCoroutineStub(
            channel = ManagedChannelBuilder
                .forAddress(serverInfo.address, serverInfo.port)
                .usePlaintext()
                .build()
        )
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

            else -> server.post(
                request = postRequest {
                    this.queueId = queueId
                    this.content.addAll(
                        textMessage.map(TextMessage::asRemote)
                    )
                }
            )
        }.asDomain
    }

    private fun QueueServiceGrpcKt.QueueServiceCoroutineStub.withTimeout(timeoutSeconds: Long?) = let {
        timeoutSeconds?.let { seconds -> it.withDeadlineAfter(seconds, TimeUnit.SECONDS) } ?: it
    }

    override fun subscribe(queueId: String, timeoutSeconds: Long?): Flow<SubscriptionEvent> =
        server
            .withTimeout(timeoutSeconds)
            .subscribe(request = subscribeRequest { this.queueId = queueId })
            .map { it.asDomain }

    override suspend fun poll(
        queueId: String,
        timeoutSeconds: Long?
    ): QueueServiceClient.Response.Poll = withContext(coroutineContext) {
        server
            .withTimeout(timeoutSeconds)
            .poll(request = pollRequest { this.queueId = queueId })
            .asDomain
    }

    override suspend fun remove(queueId: String): QueueServiceClient.Response.Remove =
        withContext(coroutineContext) {
            server.remove(
                request = removeRequest { this.id = queueId }
            ).asDomain
        }
}

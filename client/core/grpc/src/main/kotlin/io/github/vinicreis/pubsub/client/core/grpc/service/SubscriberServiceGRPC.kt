package io.github.vinicreis.pubsub.client.core.grpc.service

import io.github.vinicreis.domain.server.core.model.request.ListRequest
import io.github.vinicreis.domain.server.core.model.request.addRequest
import io.github.vinicreis.domain.server.core.model.request.peekRequest
import io.github.vinicreis.domain.server.core.model.request.publishMultipleRequest
import io.github.vinicreis.domain.server.core.model.request.publishSingleRequest
import io.github.vinicreis.domain.server.core.model.request.subscribeRequest
import io.github.vinicreis.domain.server.core.service.ChannelServiceGrpcKt
import io.github.vinicreis.pubsub.client.core.grpc.mapper.asDomain
import io.github.vinicreis.pubsub.client.core.grpc.mapper.asRemote
import io.github.vinicreis.pubsub.client.core.model.Channel
import io.github.vinicreis.pubsub.client.core.model.Message
import io.github.vinicreis.pubsub.client.core.model.ServerInfo
import io.github.vinicreis.pubsub.client.core.model.SubscriptionEvent
import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class SubscriberServiceGRPC(
    override val serverInfo: ServerInfo,
    private val coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger(SubscriberServiceGRPC::class.java.name),
) : SubscriberServiceClient {
    private val server by lazy {
        logger.info("Connecting to server $serverInfo...")
        ChannelServiceGrpcKt.ChannelServiceCoroutineStub(
            channel = ManagedChannelBuilder
                .forAddress(serverInfo.address, serverInfo.port)
                .usePlaintext()
                .build()
        )
    }

    override suspend fun list(): SubscriberServiceClient.Response.ListAll = withContext(coroutineContext) {
        server.list(ListRequest.getDefaultInstance()).asDomain
    }

    override suspend fun publish(channel: Channel) = withContext(coroutineContext) {
        server.add(request = addRequest { this.channel = channel.asRemote }).asDomain
    }

    override suspend fun post(channelId: String, vararg message: Message) = withContext(coroutineContext) {
        when (message.size) {
            0 -> error("Message list cannot be empty")
            1 -> server.publishSingle(
                request = publishSingleRequest {
                    this.channelId = channelId
                    this.content = message.first().asRemote
                }
            )

            else -> server.publishMultiple(
                request = publishMultipleRequest {
                    this.channelId = channelId
                    this.content.addAll(
                        message.map(Message::asRemote)
                    )
                }
            )
        }.asDomain
    }

    override fun subscribe(channelId: String): Flow<SubscriptionEvent> =
        server.subscribe(subscribeRequest { this.channelId = channelId }).map { it.asDomain }

    override suspend fun peek(
        channelId: String,
        timeoutSeconds: Long?
    ): SubscriberServiceClient.Response.Peek = withContext(coroutineContext) {
        server.peek(
            request = peekRequest {
                this.channelId = channelId
                timeoutSeconds?.also { this.timeoutSeconds = timeoutSeconds }
            }
        ).asDomain
    }
}

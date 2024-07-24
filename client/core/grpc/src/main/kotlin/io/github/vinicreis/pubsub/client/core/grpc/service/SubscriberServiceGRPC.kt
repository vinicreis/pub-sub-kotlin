package io.github.vinicreis.pubsub.client.core.grpc.service

import io.github.vinicreis.domain.server.core.model.data.channel
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
import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class SubscriberServiceGRPC(
    override val serverInfo: ServerInfo,
    private val coroutineContext: CoroutineContext
) : SubscriberServiceClient {
    private val server by lazy {
        ChannelServiceGrpcKt.ChannelServiceCoroutineStub(
            channel = ManagedChannelBuilder
                .forAddress(serverInfo.address, serverInfo.port)
                .usePlaintext()
                .build()
        )
    }

    override suspend fun list(): SubscriberServiceClient.Response = withContext(coroutineContext) {
        server.list(ListRequest.getDefaultInstance()).asDomain
            .also { println("Available channels:") }
            .forEach { println("\tChannel: ${it.type} - ${it.name} - ${it.id}") }

        SubscriberServiceClient.Response.Success
    }

    override suspend fun publish(channel: Channel) = withContext(coroutineContext) {
        server.add(
            request = addRequest {
                this.channel = channel {
                    id = channel.id
                    name = channel.name
                    type = channel.type.asRemote
                }
            }
        ).asDomain
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

    override suspend fun subscribe(channelId: String): SubscriberServiceClient.Response {
        return server.subscribe(
            request = subscribeRequest {
                this.channelId = channelId
            }
        ).map { response -> Message(response.content.content.ifEmpty { "${response.status}: ${response.message}" }) }
            .let { SubscriberServiceClient.Response.Subscribed(it) }
    }

    suspend fun peek(channelId: String): String {
        return server.peek(
            request = peekRequest {
                this.channelId = channelId
            }
        ).let { response -> response.content.content.ifEmpty { "${response.result}: ${response.message}" } }
    }

    override suspend fun unsubscribe(channelId: String): SubscriberServiceClient.Response {
        return SubscriberServiceClient.Response.Success
    }
}

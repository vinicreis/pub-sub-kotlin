package io.github.vinicreis.pubsub.client.subscriber.infra

import io.github.vinicreis.domain.server.channel.request.ListRequest
import io.github.vinicreis.domain.server.channel.request.addRequest
import io.github.vinicreis.domain.server.channel.request.publishMultipleRequest
import io.github.vinicreis.domain.server.channel.request.publishSingleRequest
import io.github.vinicreis.domain.server.channel.request.subscribeRequest
import io.github.vinicreis.domain.server.channel.service.ChannelServiceGrpcKt
import io.github.vinicreis.pubsub.client.subscriber.domain.mapper.asDomain
import io.github.vinicreis.pubsub.client.subscriber.domain.mapper.asRemote
import io.github.vinicreis.pubsub.client.subscriber.domain.mapper.asRemove
import io.github.vinicreis.pubsub.client.subscriber.domain.model.Channel
import io.github.vinicreis.pubsub.client.subscriber.domain.model.Message
import io.github.vinicreis.pubsub.client.subscriber.domain.model.ServerInfo
import io.github.vinicreis.pubsub.client.subscriber.domain.service.SubscriberServiceClient
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
                this.id = channel.id
                this.name = channel.name
                this.type = channel.type.asRemove
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

    override suspend fun subscribe(channelId: String, type: Channel.Type): SubscriberServiceClient.Response {
        return server.subscribe(
            request = subscribeRequest {
                this.channelId = channelId
            }
        ).map { response -> Message(response.content.content.ifEmpty { "${response.status}: ${response.message}" }) }
            .let { SubscriberServiceClient.Response.Subscribed(it) }
    }

    override suspend fun unsubscribe(channelId: String): SubscriberServiceClient.Response {
        TODO("Not yet implemented")
    }
}

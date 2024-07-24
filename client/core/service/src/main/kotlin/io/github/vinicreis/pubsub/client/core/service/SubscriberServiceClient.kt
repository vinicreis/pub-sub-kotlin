package io.github.vinicreis.pubsub.client.core.service

import io.github.vinicreis.pubsub.client.core.model.Channel
import io.github.vinicreis.pubsub.client.core.model.Message
import io.github.vinicreis.pubsub.client.core.model.ServerInfo
import kotlinx.coroutines.flow.Flow

interface SubscriberServiceClient {
    val serverInfo: ServerInfo

    sealed interface Response {
        data object Invalid : Response
        data class Error(val message: String) : Response
        data object Success : Response
        data class Subscribed(val messages: Flow<Message>) : Response
    }

    suspend fun list(): Response
    suspend fun publish(channel: Channel): Response
    suspend fun post(channelId: String, vararg message: Message): Response
    suspend fun subscribe(channelId: String): Response
    suspend fun unsubscribe(channelId: String): Response
}

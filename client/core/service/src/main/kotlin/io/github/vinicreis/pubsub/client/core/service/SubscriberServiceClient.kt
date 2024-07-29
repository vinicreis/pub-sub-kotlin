package io.github.vinicreis.pubsub.client.core.service

import io.github.vinicreis.pubsub.client.core.model.Channel
import io.github.vinicreis.pubsub.client.core.model.Message
import io.github.vinicreis.pubsub.client.core.model.ServerInfo
import io.github.vinicreis.pubsub.client.core.model.SubscriptionEvent
import kotlinx.coroutines.flow.Flow

interface SubscriberServiceClient {
    val serverInfo: ServerInfo

    sealed interface Response {
        sealed interface ListAll : Response {
            data class Fail(val message: String?) : ListAll
            data class Success(val channels: List<Channel>) : ListAll
        }

        sealed interface Publish : Response {
            data class Fail(val message: String?) : Publish
            data class Success(val channel: Channel) : Publish
        }

        sealed interface Post : Response {
            data class Fail(val message: String?) : Post
            data class Success(val channel: Channel) : Post
        }

        sealed interface Peek : Response {
            data class Fail(val message: String?) : Peek
            data class Success(val channel: Channel, val message: Message) : Peek
        }
    }

    suspend fun list(): Response.ListAll
    suspend fun publish(channel: Channel): Response.Publish
    suspend fun post(channelId: String, vararg message: Message): Response.Post
    suspend fun peek(channelId: String, timeoutSeconds: Long? = null): Response.Peek
    fun subscribe(channelId: String): Flow<SubscriptionEvent>
}

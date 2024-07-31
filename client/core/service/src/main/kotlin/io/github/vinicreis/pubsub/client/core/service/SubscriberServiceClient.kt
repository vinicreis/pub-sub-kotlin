package io.github.vinicreis.pubsub.client.core.service

import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.model.ServerInfo
import io.github.vinicreis.pubsub.client.core.model.SubscriptionEvent
import io.github.vinicreis.pubsub.client.core.model.TextMessage
import kotlinx.coroutines.flow.Flow

interface SubscriberServiceClient {
    val serverInfo: ServerInfo

    sealed interface Response {
        sealed interface ListAll : Response {
            data class Fail(val message: String?) : ListAll
            data class Success(val queues: List<Queue>) : ListAll
        }

        sealed interface Publish : Response {
            data class Fail(val message: String?) : Publish
            data class Success(val queue: Queue) : Publish
        }

        sealed interface Post : Response {
            data class Fail(val message: String?) : Post
            data class Success(val queue: Queue) : Post
        }

        sealed interface Poll : Response {
            data class Fail(val message: String?) : Poll
            data class Success(val queue: Queue, val textMessage: TextMessage) : Poll
        }

        sealed interface Remove : Response {
            data class Fail(val message: String?) : Remove
            data class Success(val queue: Queue) : Remove
        }
    }

    suspend fun list(): Response.ListAll
    suspend fun publish(queue: Queue): Response.Publish
    suspend fun post(queueId: String, vararg textMessage: TextMessage): Response.Post
    suspend fun poll(queueId: String, timeoutSeconds: Long? = null): Response.Poll
    fun subscribe(queueId: String): Flow<SubscriptionEvent>
    suspend fun remove(queueId: String): Response.Remove
}

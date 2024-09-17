package io.github.vinicreis.pubsub.server.core.service

import io.github.vinicreis.pubsub.server.model.request.ListRequestOuterClass.ListRequest
import io.github.vinicreis.pubsub.server.model.request.PostRequestOuterClass.PostRequest
import io.github.vinicreis.pubsub.server.model.request.PublishRequestOuterClass.PublishRequest
import io.github.vinicreis.pubsub.server.model.request.RemoveRequestOuterClass.RemoveRequest
import io.github.vinicreis.pubsub.server.model.request.SubscribeRequestOuterClass
import io.github.vinicreis.pubsub.server.model.response.ListResponseOuterClass.ListResponse
import io.github.vinicreis.pubsub.server.model.response.PostResponseOuterClass.PostResponse
import io.github.vinicreis.pubsub.server.model.response.PublishResponseOuterClass.PublishResponse
import io.github.vinicreis.pubsub.server.model.response.RemoveResponseOuterClass.RemoveResponse
import io.github.vinicreis.pubsub.server.model.response.SubscribeResponseOuterClass
import kotlinx.coroutines.flow.Flow

interface QueueService {
    fun start()
    fun blockUntilShutdown()
    suspend fun publish(request: PublishRequest): PublishResponse
    suspend fun list(request: ListRequest): ListResponse
    suspend fun remove(request: RemoveRequest): RemoveResponse
    suspend fun post(request: PostRequest): PostResponse
    fun subscribe(request: SubscribeRequestOuterClass.SubscribeRequest): Flow<SubscribeResponseOuterClass.SubscribeResponse>
}

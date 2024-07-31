package io.github.vinicreis.pubsub.server.core.service

import io.github.vinicreis.domain.server.core.model.request.ListRequestOuterClass.ListRequest
import io.github.vinicreis.domain.server.core.model.request.PostMultipleRequestOuterClass.PostMultipleRequest
import io.github.vinicreis.domain.server.core.model.request.PostSingleRequestOuterClass.PostSingleRequest
import io.github.vinicreis.domain.server.core.model.request.PublishRequestOuterClass.PublishRequest
import io.github.vinicreis.domain.server.core.model.request.RemoveRequestOuterClass.RemoveRequest
import io.github.vinicreis.domain.server.core.model.request.SubscribeRequestOuterClass
import io.github.vinicreis.domain.server.core.model.response.ListResponseOuterClass.ListResponse
import io.github.vinicreis.domain.server.core.model.response.PostResponseOuterClass.PostResponse
import io.github.vinicreis.domain.server.core.model.response.PublishResponseOuterClass.PublishResponse
import io.github.vinicreis.domain.server.core.model.response.RemoveResponseOuterClass.RemoveResponse
import io.github.vinicreis.domain.server.core.model.response.SubscribeResponseOuterClass
import kotlinx.coroutines.flow.Flow

interface QueueService {
    fun start()
    fun blockUntilShutdown()
    suspend fun publish(request: PublishRequest): PublishResponse
    suspend fun list(request: ListRequest): ListResponse
    suspend fun remove(request: RemoveRequest): RemoveResponse
    suspend fun postSingle(request: PostSingleRequest): PostResponse
    suspend fun postMultiple(request: PostMultipleRequest): PostResponse
    fun subscribe(request: SubscribeRequestOuterClass.SubscribeRequest): Flow<SubscribeResponseOuterClass.SubscribeResponse>
}

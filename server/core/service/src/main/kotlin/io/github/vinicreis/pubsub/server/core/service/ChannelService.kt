package io.github.vinicreis.pubsub.server.core.service

import io.github.vinicreis.domain.server.core.model.request.AddRequest
import io.github.vinicreis.domain.server.core.model.request.ListRequest
import io.github.vinicreis.domain.server.core.model.request.PublishMultipleRequest
import io.github.vinicreis.domain.server.core.model.request.PublishSingleRequest
import io.github.vinicreis.domain.server.core.model.request.RemoveByIdRequest
import io.github.vinicreis.domain.server.core.model.request.SubscribeRequest
import io.github.vinicreis.domain.server.core.model.response.AddResponse
import io.github.vinicreis.domain.server.core.model.response.ListResponse
import io.github.vinicreis.domain.server.core.model.response.PublishResponse
import io.github.vinicreis.domain.server.core.model.response.RemoveByIdResponse
import io.github.vinicreis.domain.server.core.model.response.SubscribeResponse
import kotlinx.coroutines.flow.Flow

interface ChannelService {
    fun start()
    fun blockUntilShutdown()
    suspend fun add(request: AddRequest): AddResponse
    suspend fun list(request: ListRequest): ListResponse
    suspend fun removeById(request: RemoveByIdRequest): RemoveByIdResponse
    suspend fun publishSingle(request: PublishSingleRequest): PublishResponse
    suspend fun publishMultiple(request: PublishMultipleRequest): PublishResponse
    fun subscribe(request: SubscribeRequest): Flow<SubscribeResponse>
}

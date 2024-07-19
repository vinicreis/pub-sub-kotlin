package io.github.vinicreis.pubsub.server.service

import io.github.vinicreis.domain.channel.request.AddRequest
import io.github.vinicreis.domain.channel.request.ListRequest
import io.github.vinicreis.domain.channel.request.RemoveByIdRequest
import io.github.vinicreis.domain.channel.response.AddResponse
import io.github.vinicreis.domain.channel.response.ListResponse
import io.github.vinicreis.domain.channel.response.RemoveByIdResponse
import io.github.vinicreis.domain.channel.service.ChannelServiceGrpcKt
import kotlin.coroutines.CoroutineContext

class ChannelService(
    coroutineContext: CoroutineContext,
) : ChannelServiceGrpcKt.ChannelServiceCoroutineImplBase(
    coroutineContext
) {
    override suspend fun add(request: AddRequest): AddResponse {
        TODO("Not yet implemented")
    }

    override suspend fun list(request: ListRequest): ListResponse {
        TODO("Not yet implemented")
    }

    override suspend fun removeById(request: RemoveByIdRequest): RemoveByIdResponse {
        TODO("Not yet implemented")
    }
}

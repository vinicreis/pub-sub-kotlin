package io.github.vinicreis.pubsub.server.channel.domain.service

import io.github.vinicreis.domain.server.channel.request.AddRequest
import io.github.vinicreis.domain.server.channel.request.ListRequest
import io.github.vinicreis.domain.server.channel.request.PublishRequest
import io.github.vinicreis.domain.server.channel.request.RemoveByIdRequest
import io.github.vinicreis.domain.server.channel.request.SubscribeRequest
import io.github.vinicreis.domain.server.channel.response.AddResponse
import io.github.vinicreis.domain.server.channel.response.ListResponse
import io.github.vinicreis.domain.server.channel.response.PublishResponse
import io.github.vinicreis.domain.server.channel.response.RemoveByIdResponse
import io.github.vinicreis.domain.server.channel.response.SubscribeResponse
import kotlinx.coroutines.flow.Flow

interface ChannelService {
    suspend fun add(request: AddRequest): AddResponse
    suspend fun list(request: ListRequest): ListResponse
    suspend fun removeById(request: RemoveByIdRequest): RemoveByIdResponse
    suspend fun publish(request: PublishRequest): PublishResponse
    fun subscribe(request: SubscribeRequest): Flow<SubscribeResponse>
}
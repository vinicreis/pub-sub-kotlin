package io.github.vinicreis.pubsub.client.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.ResultOuterClass
import io.github.vinicreis.domain.server.core.model.response.AddResponse
import io.github.vinicreis.domain.server.core.model.response.ListResponse
import io.github.vinicreis.domain.server.core.model.response.PublishResponse
import io.github.vinicreis.pubsub.client.core.model.Channel
import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient
import io.github.vinicreis.domain.server.core.model.data.ChannelOuterClass.Channel as RemoteChannel

internal val ResultOuterClass.Result?.asDomain: SubscriberServiceClient.Response
    get() = when(this) {
        ResultOuterClass.Result.SUCCESS -> SubscriberServiceClient.Response.Success
        ResultOuterClass.Result.ERROR -> SubscriberServiceClient.Response.Error("Error")
        ResultOuterClass.Result.UNRECOGNIZED, null -> SubscriberServiceClient.Response.Invalid
    }

internal val ListResponse.asDomain: List<Channel>
    get() = when(result.asDomain) {
        is SubscriberServiceClient.Response.Error,
        SubscriberServiceClient.Response.Invalid,
        is SubscriberServiceClient.Response.Subscribed -> error("Failed!")
        SubscriberServiceClient.Response.Success -> channelsList.map(RemoteChannel::asDomain)
    }

internal val AddResponse.asDomain: SubscriberServiceClient.Response
    get() = result.asDomain

internal val PublishResponse.asDomain: SubscriberServiceClient.Response
    get() = result.asDomain

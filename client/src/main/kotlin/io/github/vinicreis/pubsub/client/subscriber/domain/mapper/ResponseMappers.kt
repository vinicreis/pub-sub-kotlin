package io.github.vinicreis.pubsub.client.subscriber.domain.mapper

import io.github.vinicreis.domain.server.channel.response.AddResponse
import io.github.vinicreis.domain.server.channel.response.ListResponse
import io.github.vinicreis.domain.server.channel.response.PublishResponse
import io.github.vinicreis.pubsub.client.subscriber.domain.model.Channel
import io.github.vinicreis.pubsub.client.subscriber.domain.service.SubscriberServiceClient
import io.github.vinicreis.domain.model.ResultOuterClass.Result as RemoteResult
import io.github.vinicreis.domain.server.channel.model.ChannelOuterClass.Channel as RemoteChannel

internal val RemoteResult?.asDomain: SubscriberServiceClient.Response
    get() = when(this) {
        RemoteResult.SUCCESS -> SubscriberServiceClient.Response.Success
        RemoteResult.ERROR -> SubscriberServiceClient.Response.Error("Error")
        RemoteResult.UNRECOGNIZED, null -> SubscriberServiceClient.Response.Invalid
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

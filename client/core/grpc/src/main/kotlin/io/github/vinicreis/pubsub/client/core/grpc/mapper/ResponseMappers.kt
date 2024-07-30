package io.github.vinicreis.pubsub.client.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.ResultOuterClass
import io.github.vinicreis.domain.server.core.model.data.SubscriptionStatusOuterClass.SubscriptionStatus
import io.github.vinicreis.domain.server.core.model.response.AddResponse
import io.github.vinicreis.domain.server.core.model.response.ListResponse
import io.github.vinicreis.domain.server.core.model.response.PeekResponse
import io.github.vinicreis.domain.server.core.model.response.PublishResponse
import io.github.vinicreis.domain.server.core.model.response.RemoveByIdResponse
import io.github.vinicreis.domain.server.core.model.response.SubscribeResponse
import io.github.vinicreis.pubsub.client.core.grpc.model.ServerResult
import io.github.vinicreis.pubsub.client.core.model.SubscriptionEvent
import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient

internal val ResultOuterClass.Result?.asDomain: ServerResult
    get() = when(this) {
        ResultOuterClass.Result.UNRECOGNIZED, null -> ServerResult.UNKNOWN
        ResultOuterClass.Result.ERROR -> ServerResult.ERROR
        ResultOuterClass.Result.SUCCESS -> ServerResult.SUCCESS
    }

internal val ListResponse.asDomain: SubscriberServiceClient.Response.ListAll
    get() = when(result.asDomain) {
        ServerResult.UNKNOWN -> SubscriberServiceClient.Response.ListAll.Fail("Unknown result received")
        ServerResult.ERROR -> SubscriberServiceClient.Response.ListAll.Fail(message.ifEmpty { null })
        ServerResult.SUCCESS -> SubscriberServiceClient.Response.ListAll.Success(channelsList.map { it.asDomain })
    }

internal val AddResponse.asDomain: SubscriberServiceClient.Response.Publish
    get() = when(result.asDomain) {
        ServerResult.UNKNOWN -> SubscriberServiceClient.Response.Publish.Fail("Unknown result received")
        ServerResult.ERROR -> SubscriberServiceClient.Response.Publish.Fail(message.ifEmpty { null })
        ServerResult.SUCCESS -> SubscriberServiceClient.Response.Publish.Success(
            channel = channel.asDomain
        )
    }

internal val PublishResponse.asDomain: SubscriberServiceClient.Response.Post
    get() = when(result.asDomain) {
        ServerResult.UNKNOWN -> SubscriberServiceClient.Response.Post.Fail("Unknown result received")
        ServerResult.ERROR -> SubscriberServiceClient.Response.Post.Fail(message.ifEmpty { null })
        ServerResult.SUCCESS -> SubscriberServiceClient.Response.Post.Success(
            channel = channel.asDomain
        )
    }

internal val SubscribeResponse.asDomain: SubscriptionEvent
    get() = when(status) {
        SubscriptionStatus.UNRECOGNIZED, null -> error("Unrecognized subscription status")
        SubscriptionStatus.PROCESSING -> SubscriptionEvent.Processing
        SubscriptionStatus.ACTIVE -> SubscriptionEvent.Active(channel.asDomain)
        SubscriptionStatus.UPDATE -> SubscriptionEvent.Update(channel.asDomain, content.asDomain)
        SubscriptionStatus.FINISHED -> SubscriptionEvent.Finished(message.ifEmpty { null })
    }

internal val PeekResponse.asDomain: SubscriberServiceClient.Response.Peek
    get() = when(result) {
        ResultOuterClass.Result.UNRECOGNIZED, null -> SubscriberServiceClient.Response.Peek.Fail("Unknown result received")
        ResultOuterClass.Result.ERROR -> SubscriberServiceClient.Response.Peek.Fail(message.ifEmpty { null })
        ResultOuterClass.Result.SUCCESS -> SubscriberServiceClient.Response.Peek.Success(
            channel = channel.asDomain,
            message = content.asDomain
        )
    }

internal val RemoveByIdResponse.oasDomain: SubscriberServiceClient.Response.Remove
    get() = when(result) {
        ResultOuterClass.Result.UNRECOGNIZED, null -> SubscriberServiceClient.Response.Remove.Fail("Unknown result received")
        ResultOuterClass.Result.ERROR -> SubscriberServiceClient.Response.Remove.Fail(message.ifEmpty { null })
        ResultOuterClass.Result.SUCCESS -> SubscriberServiceClient.Response.Remove.Success(
            channel = channel.asDomain,
        )
    }

package io.github.vinicreis.pubsub.client.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.ResultOuterClass
import io.github.vinicreis.domain.server.core.model.response.ListResponseOuterClass.ListResponse
import io.github.vinicreis.domain.server.core.model.response.PollResponseOuterClass.PollResponse
import io.github.vinicreis.domain.server.core.model.response.PostResponseOuterClass.PostResponse
import io.github.vinicreis.domain.server.core.model.response.PublishResponseOuterClass.PublishResponse
import io.github.vinicreis.domain.server.core.model.response.RemoveResponseOuterClass.RemoveResponse
import io.github.vinicreis.domain.server.core.model.response.SubscribeResponseOuterClass.SubscribeResponse
import io.github.vinicreis.pubsub.client.core.grpc.model.ServerResult
import io.github.vinicreis.pubsub.client.core.model.SubscriptionEvent
import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient
import io.github.vinicreis.domain.server.core.model.data.SubscriptionEventOuterClass.SubscriptionEvent as RemoteSubscriptionEvent

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
        ServerResult.SUCCESS -> SubscriberServiceClient.Response.ListAll.Success(queuesList.map { it.asDomain })
    }

internal val PublishResponse.asDomain: SubscriberServiceClient.Response.Publish
    get() = when(result.asDomain) {
        ServerResult.UNKNOWN -> SubscriberServiceClient.Response.Publish.Fail("Unknown result received")
        ServerResult.ERROR -> SubscriberServiceClient.Response.Publish.Fail(message.ifEmpty { null })
        ServerResult.SUCCESS -> SubscriberServiceClient.Response.Publish.Success(
            queue = queue.asDomain
        )
    }

internal val PostResponse.asDomain: SubscriberServiceClient.Response.Post
    get() = when(result.asDomain) {
        ServerResult.UNKNOWN -> SubscriberServiceClient.Response.Post.Fail("Unknown result received")
        ServerResult.ERROR -> SubscriberServiceClient.Response.Post.Fail(message.ifEmpty { null })
        ServerResult.SUCCESS -> SubscriberServiceClient.Response.Post.Success(
            queue = queue.asDomain
        )
    }

internal val SubscribeResponse.asDomain: SubscriptionEvent
    get() = when(event) {
        RemoteSubscriptionEvent.UNRECOGNIZED, null -> error("Unrecognized subscription status")
        RemoteSubscriptionEvent.PROCESSING -> SubscriptionEvent.Processing
        RemoteSubscriptionEvent.ACTIVE -> SubscriptionEvent.Active(queue.asDomain)
        RemoteSubscriptionEvent.UPDATE -> SubscriptionEvent.Update(queue.asDomain, content.asDomain)
        RemoteSubscriptionEvent.FINISHED -> SubscriptionEvent.Finished(message.ifEmpty { null })
    }

internal val PollResponse.asDomain: SubscriberServiceClient.Response.Poll
    get() = when(result) {
        ResultOuterClass.Result.UNRECOGNIZED, null -> SubscriberServiceClient.Response.Poll.Fail("Unknown result received")
        ResultOuterClass.Result.ERROR -> SubscriberServiceClient.Response.Poll.Fail(message.ifEmpty { null })
        ResultOuterClass.Result.SUCCESS -> SubscriberServiceClient.Response.Poll.Success(
            queue = queue.asDomain,
            textMessage = content.asDomain
        )
    }

internal val RemoveResponse.asDomain: SubscriberServiceClient.Response.Remove
    get() = when(result) {
        ResultOuterClass.Result.UNRECOGNIZED, null -> SubscriberServiceClient.Response.Remove.Fail("Unknown result received")
        ResultOuterClass.Result.ERROR -> SubscriberServiceClient.Response.Remove.Fail(message.ifEmpty { null })
        ResultOuterClass.Result.SUCCESS -> SubscriberServiceClient.Response.Remove.Success(
            queue = queue.asDomain,
        )
    }

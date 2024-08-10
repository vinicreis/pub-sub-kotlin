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
import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient
import io.github.vinicreis.domain.server.core.model.data.SubscriptionEventOuterClass.SubscriptionEvent as RemoteSubscriptionEvent

internal val ResultOuterClass.Result?.asDomain: ServerResult
    get() = when(this) {
        ResultOuterClass.Result.UNRECOGNIZED, null -> ServerResult.UNKNOWN
        ResultOuterClass.Result.ERROR -> ServerResult.ERROR
        ResultOuterClass.Result.SUCCESS -> ServerResult.SUCCESS
    }

internal val ListResponse.asDomain: QueueServiceClient.Response.ListAll
    get() = when(result.asDomain) {
        ServerResult.UNKNOWN -> QueueServiceClient.Response.ListAll.Fail("Unknown result received")
        ServerResult.ERROR -> QueueServiceClient.Response.ListAll.Fail(message.ifEmpty { null })
        ServerResult.SUCCESS -> QueueServiceClient.Response.ListAll.Success(queuesList.map { it.asDomain })
    }

internal val PublishResponse.asDomain: QueueServiceClient.Response.Publish
    get() = when(result.asDomain) {
        ServerResult.UNKNOWN -> QueueServiceClient.Response.Publish.Fail("Unknown result received")
        ServerResult.ERROR -> QueueServiceClient.Response.Publish.Fail(message.ifEmpty { null })
        ServerResult.SUCCESS -> QueueServiceClient.Response.Publish.Success(
            queue = queue.asDomain
        )
    }

internal val PostResponse.asDomain: QueueServiceClient.Response.Post
    get() = when(result.asDomain) {
        ServerResult.UNKNOWN -> QueueServiceClient.Response.Post.Fail("Unknown result received")
        ServerResult.ERROR -> QueueServiceClient.Response.Post.Fail(message.ifEmpty { null })
        ServerResult.SUCCESS -> QueueServiceClient.Response.Post.Success(
            queue = queue.asDomain
        )
    }

internal val SubscribeResponse.asDomain: SubscriptionEvent
    get() = when(event) {
        RemoteSubscriptionEvent.UNRECOGNIZED, null -> error("Unrecognized subscription status")
        RemoteSubscriptionEvent.PROCESSING -> SubscriptionEvent.Processing
        RemoteSubscriptionEvent.ACTIVE -> SubscriptionEvent.Active(queue.asDomain)
        RemoteSubscriptionEvent.UPDATE -> SubscriptionEvent.Update(queue.asDomain, content.asDomain)
        RemoteSubscriptionEvent.FINISHED -> SubscriptionEvent.Finished(queue.asDomain, message.ifEmpty { null })
    }

internal val PollResponse.asDomain: QueueServiceClient.Response.Poll
    get() = when(result) {
        ResultOuterClass.Result.UNRECOGNIZED, null -> QueueServiceClient.Response.Poll.Fail("Unknown result received")
        ResultOuterClass.Result.ERROR -> QueueServiceClient.Response.Poll.Fail(message.ifEmpty { null })
        ResultOuterClass.Result.SUCCESS -> QueueServiceClient.Response.Poll.Success(
            queue = queue.asDomain,
            textMessage = content.asDomain
        )
    }

internal val RemoveResponse.asDomain: QueueServiceClient.Response.Remove
    get() = when(result) {
        ResultOuterClass.Result.UNRECOGNIZED, null -> QueueServiceClient.Response.Remove.Fail("Unknown result received")
        ResultOuterClass.Result.ERROR -> QueueServiceClient.Response.Remove.Fail(message.ifEmpty { null })
        ResultOuterClass.Result.SUCCESS -> QueueServiceClient.Response.Remove.Success(
            queue = queue.asDomain,
        )
    }

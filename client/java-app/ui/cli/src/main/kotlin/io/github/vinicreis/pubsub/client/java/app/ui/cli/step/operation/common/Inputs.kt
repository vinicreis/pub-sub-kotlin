package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.common

import io.github.vinicreis.pubsub.client.core.model.Channel
import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient

suspend fun SubscriberServiceClient.withChannelList(
    block: suspend (List<Channel>) -> Unit
) {
    return when (val response = list()) {
        is SubscriberServiceClient.Response.ListAll.Fail ->
            throw RuntimeException("Failed to list channels: ${response.message}")

        is SubscriberServiceClient.Response.ListAll.Success -> block(response.channels)
    }
}

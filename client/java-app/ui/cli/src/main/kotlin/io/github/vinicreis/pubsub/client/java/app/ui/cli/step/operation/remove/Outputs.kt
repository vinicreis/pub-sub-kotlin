package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.remove

import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

fun SubscriberServiceClient.Response.Remove.print() {
    return when(this) {
        is SubscriberServiceClient.Response.Remove.Fail -> println("Failed to remove message: $message")
        is SubscriberServiceClient.Response.Remove.Success ->
            println(StringResource.Channel.Message.CHANNEL_REMOVED_SUCCESSFULLY)
    }
}

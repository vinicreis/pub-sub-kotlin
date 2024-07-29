package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish

import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list.print

fun SubscriberServiceClient.Response.Publish.print() {
    when(this) {
        is SubscriberServiceClient.Response.Publish.Fail -> println("Failed to publish channel: $message")
        is SubscriberServiceClient.Response.Publish.Success -> {
            println("Channel published successfully:")
            channel.print()
        }
    }
}

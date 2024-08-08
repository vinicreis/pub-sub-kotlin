package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish

import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list.print

fun QueueServiceClient.Response.Publish.print() {
    when(this) {
        is QueueServiceClient.Response.Publish.Fail -> println("Failed to publish queue: $message")
        is QueueServiceClient.Response.Publish.Success -> {
            println("Queue published successfully:")
            queue.print()
        }
    }
}

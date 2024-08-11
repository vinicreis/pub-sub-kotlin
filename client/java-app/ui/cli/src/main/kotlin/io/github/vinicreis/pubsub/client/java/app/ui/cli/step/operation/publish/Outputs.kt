package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish

import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.clear
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list.print

fun QueueServiceClient.Response.Publish.print() {
    when(this) {
        is QueueServiceClient.Response.Publish.Fail -> println("Failed to publish queue: $message")
        is QueueServiceClient.Response.Publish.Success -> {
            clear()
            println("Queue published successfully:")
            queue.print()
        }
    }
}

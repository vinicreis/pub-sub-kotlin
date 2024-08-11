package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.remove

import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.clear
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

fun QueueServiceClient.Response.Remove.print() {
    return when(this) {
        is QueueServiceClient.Response.Remove.Fail -> println("Failed to remove message: $message")
        is QueueServiceClient.Response.Remove.Success -> {
            clear()
            println(StringResource.Queue.Message.QUEUE_REMOVED_SUCCESSFULLY)
        }
    }
}

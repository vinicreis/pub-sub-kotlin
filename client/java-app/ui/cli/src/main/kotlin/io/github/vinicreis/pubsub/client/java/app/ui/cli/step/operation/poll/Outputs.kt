package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.poll

import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient

fun QueueServiceClient.Response.Poll.print() {
    return when(this) {
        is QueueServiceClient.Response.Poll.Fail -> println("Failed to poll message: $message")
        is QueueServiceClient.Response.Poll.Success -> println("Message polled successfully: ${textMessage.content}")
    }
}

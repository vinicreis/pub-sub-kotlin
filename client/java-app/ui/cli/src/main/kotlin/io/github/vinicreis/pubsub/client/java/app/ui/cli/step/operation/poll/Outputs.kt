package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.poll

import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient

fun SubscriberServiceClient.Response.Poll.print() {
    return when(this) {
        is SubscriberServiceClient.Response.Poll.Fail -> println("Failed to poll message: $message")
        is SubscriberServiceClient.Response.Poll.Success -> println("Message polled successfully: ${textMessage.content}")
    }
}

package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post

import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient

fun SubscriberServiceClient.Response.Post.print() {
    return when(this) {
        is SubscriberServiceClient.Response.Post.Fail -> println("Failed to post message: $message")
        is SubscriberServiceClient.Response.Post.Success -> println("Message posted successfully")
    }
}

package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post

import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient

fun QueueServiceClient.Response.Post.print() {
    return when(this) {
        is QueueServiceClient.Response.Post.Fail -> println("Failed to post message: $message")
        is QueueServiceClient.Response.Post.Success -> println("Message posted successfully")
    }
}

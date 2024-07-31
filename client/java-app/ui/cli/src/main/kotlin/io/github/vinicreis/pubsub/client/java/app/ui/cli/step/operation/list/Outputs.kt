package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list

import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

fun Queue.print() {
    println("Queue: $name")
    println("\tID: $id")
    println("\tCode: $code")
    println("\tHas $pendingMessagesCount pending messages")
}

fun SubscriberServiceClient.Response.ListAll.print() {
    when(this) {
        is SubscriberServiceClient.Response.ListAll.Fail -> println(StringResource.Operation.List.Error.GENERIC)
        is SubscriberServiceClient.Response.ListAll.Success -> queues.print(printElement = Queue::print)
    }
}

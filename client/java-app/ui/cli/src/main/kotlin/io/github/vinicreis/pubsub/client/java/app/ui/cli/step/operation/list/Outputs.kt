package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list

import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

fun Queue.print() {
    println("Queue: $name")
    println("\tID: $id")
    println("\tCode: $code")
    println("\tType: $type")
    println("\tHas $pendingMessagesCount pending messages")
}

fun QueueServiceClient.Response.ListAll.print() {
    when(this) {
        is QueueServiceClient.Response.ListAll.Fail -> println(StringResource.Operation.List.Error.GENERIC)
        is QueueServiceClient.Response.ListAll.Success -> {
            println("List queues: ")
            queues.print(printElement = Queue::print)
        }
    }
}

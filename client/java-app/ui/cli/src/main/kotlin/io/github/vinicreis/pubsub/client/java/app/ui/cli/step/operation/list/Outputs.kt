package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list

import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.clear
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

fun Queue.print() {
    println(name)
    println("\tID: $id")
    println("\tCode: $code")
    println("\tType: $type")
    println("\tHas $pendingMessagesCount pending messages")
}

fun QueueServiceClient.Response.ListAll.print() {
    when(this) {
        is QueueServiceClient.Response.ListAll.Fail -> println(StringResource.Operation.List.Error.GENERIC)
        is QueueServiceClient.Response.ListAll.Success -> {
            clear()
            println("Queues: ")
            queues.print(printElement = Queue::print)
        }
    }
}

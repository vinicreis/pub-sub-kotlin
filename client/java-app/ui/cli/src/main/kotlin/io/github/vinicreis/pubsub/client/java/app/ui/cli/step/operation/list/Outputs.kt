package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list

import io.github.vinicreis.pubsub.client.core.model.Channel
import io.github.vinicreis.pubsub.client.core.service.SubscriberServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

fun Channel.print() {
    println("Channel: $name")
    println("\tID: $id")
    println("\tCode: $code")
    println("\tHas $pendingMessagesCount pending messages")
}

fun SubscriberServiceClient.Response.ListAll.show() {
    when(this) {
        is SubscriberServiceClient.Response.ListAll.Fail -> println(StringResource.Operation.List.Error.GENERIC)
        is SubscriberServiceClient.Response.ListAll.Success -> channels.print(printElement = Channel::print)
    }
}

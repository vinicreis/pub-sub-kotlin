package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.subscribe

import io.github.vinicreis.pubsub.client.core.model.SubscriptionEvent
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource
import kotlinx.coroutines.flow.Flow

fun SubscriptionEvent.print() {
    when(this) {
        SubscriptionEvent.Processing -> println(StringResource.Channel.Message.PROCESSING_SUBSCRIPTION)
        is SubscriptionEvent.Active -> println(String.format(StringResource.Channel.Message.SUBSCRIPTION_ACTIVE, channel.name))
        is SubscriptionEvent.Update -> println(String.format(StringResource.Channel.Message.MESSAGE_RECEIVED, channel.name, message.content))
        is SubscriptionEvent.Finished -> println(String.format(StringResource.Channel.Message.SUBSCRIPTION_FINISHED, message))
    }
}

suspend fun Flow<SubscriptionEvent>.collectAndPrint() {
    collect { event -> event.print() }
}

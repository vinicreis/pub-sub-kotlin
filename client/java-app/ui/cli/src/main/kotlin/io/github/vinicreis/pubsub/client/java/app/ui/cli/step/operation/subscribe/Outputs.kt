package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.subscribe

import io.github.vinicreis.pubsub.client.core.model.SubscriptionEvent
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource
import kotlinx.coroutines.flow.Flow

fun SubscriptionEvent.print() {
    when(this) {
        SubscriptionEvent.Processing -> println(StringResource.Queue.Message.PROCESSING_SUBSCRIPTION)
        is SubscriptionEvent.Active -> println(String.format(StringResource.Queue.Message.SUBSCRIPTION_ACTIVE, queue.name))
        is SubscriptionEvent.Update -> println(String.format(StringResource.Queue.Message.MESSAGE_RECEIVED, queue.name, textMessage.content))
        is SubscriptionEvent.Finished -> println(String.format(StringResource.Queue.Message.SUBSCRIPTION_FINISHED, message))
    }
}

suspend fun Flow<SubscriptionEvent>.collectAndPrint() {
    collect { event -> event.print() }
}

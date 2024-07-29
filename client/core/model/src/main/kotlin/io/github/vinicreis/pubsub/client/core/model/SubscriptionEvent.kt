package io.github.vinicreis.pubsub.client.core.model

sealed interface SubscriptionEvent {
    data object Processing : SubscriptionEvent
    data class Active(val channel: Channel) : SubscriptionEvent
    data class Update(val channel: Channel, val message: Message) : SubscriptionEvent
    data class Finished(val message: String? = null) : SubscriptionEvent
}

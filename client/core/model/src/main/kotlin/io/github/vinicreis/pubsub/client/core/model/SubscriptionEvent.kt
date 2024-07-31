package io.github.vinicreis.pubsub.client.core.model

sealed interface SubscriptionEvent {
    data object Processing : SubscriptionEvent
    data class Active(val queue: Queue) : SubscriptionEvent
    data class Update(val queue: Queue, val textMessage: TextMessage) : SubscriptionEvent
    data class Finished(val message: String? = null) : SubscriptionEvent
}

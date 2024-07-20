package io.github.vinicreis.pubsub.client.subscriber.domain.model

data class Channel(
    val id: String,
    val name: String = id,
    val type: Type,
) {
    enum class Type {
        SIMPLE,
        MULTIPLE,
    }
}

package io.github.vinicreis.pubsub.server.core.model.data

data class Channel(
    val id: String,
    val name: String = id,
    val type: Type,
) {
    enum class Type {
        SIMPLE,
        MULTIPLE
    }
}

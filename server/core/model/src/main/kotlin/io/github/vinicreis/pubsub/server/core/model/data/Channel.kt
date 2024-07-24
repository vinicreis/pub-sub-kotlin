package io.github.vinicreis.pubsub.server.core.model.data

data class Channel(
    val id: String,
    val name: String = id,
    val type: Type,
    var pendingMessagesCount: Int = 0,
) {
    enum class Type {
        SIMPLE,
        MULTIPLE
    }
}

package io.github.vinicreis.pubsub.server.channel.domain.model

import java.util.UUID

data class Channel(
    val id: String,
    val type: Type,
    val name: String = id,
    val pendingMessages: MutableList<Message> = mutableListOf(),
    val subscribers: MutableList<Subscriber> = mutableListOf(),
) {
    enum class Type {
        SIMPLE,
        MULTIPLE,
    }
}

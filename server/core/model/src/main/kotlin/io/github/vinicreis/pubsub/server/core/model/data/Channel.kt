package io.github.vinicreis.pubsub.server.core.model.data

import java.util.*

data class Channel(
    val id: UUID,
    val code: String,
    val name: String = code,
    val type: Type,
    var pendingMessagesCount: Long = 0,
) {
    enum class Type {
        SIMPLE,
        MULTIPLE
    }
}

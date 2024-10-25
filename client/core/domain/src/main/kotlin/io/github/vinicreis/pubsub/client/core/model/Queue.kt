package io.github.vinicreis.pubsub.client.core.model

import java.util.*

data class Queue(
    val id: UUID,
    val code: String,
    val name: String = code,
    val type: Type,
    val pendingMessagesCount: Long,
) {
    enum class Type {
        SIMPLE,
        MULTIPLE;

        companion object {
            fun fromOrdinal(ordinal: Int) = Type.entries.firstOrNull { it.ordinal == ordinal }
        }
    }
}

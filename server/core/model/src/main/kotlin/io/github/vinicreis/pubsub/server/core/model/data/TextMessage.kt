package io.github.vinicreis.pubsub.server.core.model.data

import java.util.*

data class TextMessage(
    val id: UUID,
    val content: String,
) {
    companion object {
        const val MAX_CONTENT_LENGTH = 20_000
    }
}

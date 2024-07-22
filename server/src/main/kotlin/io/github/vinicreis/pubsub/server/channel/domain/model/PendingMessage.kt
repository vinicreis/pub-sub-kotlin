package io.github.vinicreis.pubsub.server.channel.domain.model

import java.util.*

data class PendingMessage(
    val id: String,
    val channel: Channel,
    val content: String,
    val createdAt: Date = Date()
)

package io.github.vinicreis.pubsub.server.core.model.data

import java.util.*

data class Message(
    val id: UUID = UUID.randomUUID(),
    val content: String,
)

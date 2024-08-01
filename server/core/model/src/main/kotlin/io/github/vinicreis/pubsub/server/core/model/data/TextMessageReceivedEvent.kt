package io.github.vinicreis.pubsub.server.core.model.data

import java.util.*

data class TextMessageReceivedEvent(
    val id: UUID = UUID.randomUUID(),
    val queue: Queue,
    val textMessage: TextMessage,
) : Event

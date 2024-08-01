package io.github.vinicreis.pubsub.server.core.model.data.event

import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.*

data class TextMessageReceivedEvent(
    override val id: UUID = UUID.randomUUID(),
    val textMessage: TextMessage,
    override val createdAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
) : Event.Saveable

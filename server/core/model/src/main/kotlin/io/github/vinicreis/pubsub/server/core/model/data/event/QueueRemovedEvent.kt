package io.github.vinicreis.pubsub.server.core.model.data.event

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.*

data class QueueRemovedEvent(
    override val id: UUID = UUID.randomUUID(),
    val queueId: UUID,
    override val createdAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
) : Event.Saveable

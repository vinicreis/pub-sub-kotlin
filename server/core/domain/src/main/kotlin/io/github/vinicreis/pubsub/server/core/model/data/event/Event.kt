package io.github.vinicreis.pubsub.server.core.model.data.event

import kotlinx.datetime.LocalDateTime
import java.util.*

sealed interface Event {
    sealed interface Saveable : Event {
        val id: UUID
        val createdAt: LocalDateTime
    }
}

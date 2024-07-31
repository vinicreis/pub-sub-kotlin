package io.github.vinicreis.pubsub.server.core.model.data

import java.util.*

sealed interface Event {
    val id: UUID
}

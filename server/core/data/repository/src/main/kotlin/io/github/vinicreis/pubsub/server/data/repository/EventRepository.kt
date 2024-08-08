package io.github.vinicreis.pubsub.server.data.repository

import io.github.vinicreis.pubsub.server.core.model.data.event.Event
import io.github.vinicreis.pubsub.server.data.model.Transaction
import java.util.*

interface EventRepository {
    context(T)
    fun <T : Transaction> notify(event: Event.Saveable)
    suspend fun consume(queueId: UUID): Event.Saveable?
}

package io.github.vinicreis.pubsub.server.data.repository

import io.github.vinicreis.pubsub.server.core.model.data.Event
import io.github.vinicreis.pubsub.server.data.model.Transaction
import java.util.*

interface EventsRepository {
    sealed interface Result {
        sealed interface Consume : Result {
            data object None : Consume
            data class Success(val event: Event) : Consume
            data class Fail(val e: Exception) : Consume
        }
    }

    context(T)
    fun <T : Transaction> notify(event: Event)
    suspend fun consume(queueId: UUID): Result.Consume
}

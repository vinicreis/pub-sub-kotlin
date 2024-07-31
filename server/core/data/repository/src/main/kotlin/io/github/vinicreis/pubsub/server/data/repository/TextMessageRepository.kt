package io.github.vinicreis.pubsub.server.data.repository

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage

interface TextMessageRepository {
    sealed interface Result {
        sealed interface Add : Result {
            data class Error(val e: Exception) : Add
            data object Success : Add
        }

        sealed interface Remove : Result {
            data class Error(val e: Exception) : Remove
            data object Success : Remove
        }
    }

    suspend fun add(queue: Queue, textMessages: List<TextMessage>): Result.Add
    suspend fun remove(queue: Queue): Result.Remove
}

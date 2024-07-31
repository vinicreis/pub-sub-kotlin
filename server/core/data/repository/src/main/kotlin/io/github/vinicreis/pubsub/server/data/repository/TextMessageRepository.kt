package io.github.vinicreis.pubsub.server.data.repository

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import kotlinx.coroutines.flow.Flow

interface TextMessageRepository {
    sealed interface Result {
        sealed interface Add : Result {
            data class Error(val e: Exception) : Add
            data object Success : Add
            data object QueueNotFound : Add
        }

        sealed interface Subscribe : Result {
            data class Error(val e: Exception) : Subscribe
            data class Success(val messages: Flow<TextMessage>) : Subscribe
            data object QueueNotFound : Subscribe
        }

        sealed interface Remove : Result {
            data class Error(val e: Exception) : Remove
            data object Success : Remove
            data object QueueNotFound : Remove
        }
    }

    suspend fun add(queue: Queue, textMessage: TextMessage): Result.Add
    suspend fun addAll(queue: Queue, textMessages: List<TextMessage>): Result.Add
    suspend fun remove(queue: Queue): Result.Remove
    suspend fun subscribe(queue: Queue): Result.Subscribe
}

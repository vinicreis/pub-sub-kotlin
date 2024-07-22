package io.github.vinicreis.pubsub.server.channel.domain.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.PendingMessage

interface PendingMessageRepository {
    sealed interface Result {
        sealed interface Add : Result {
            data object Success : Add
            data class Error(val e: Exception) : Add
        }

        sealed interface Poll : Result {
            data class Success(val message: PendingMessage) : Poll
            data object Empty : Poll
            data class Error(val e: Exception) : Poll
        }
    }

    suspend fun add(pendingMessage: PendingMessage): Result.Add
    suspend fun poll(): Result.Poll
}

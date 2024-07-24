package io.github.vinicreis.pubsub.server.data.repository

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import kotlinx.coroutines.channels.Channel as KotlinChannel

interface MessageRepository {
    sealed interface Result {
        sealed interface Add : Result {
            data class Error(val e: Exception) : Add
            data object Success : Add
        }

        sealed interface Poll : Result {
            data class Error(val e: Exception) : Poll
            data class Success(val message: Message) : Poll
            data object QueueNotFound : Poll
        }

        sealed interface Subscribe : Result {
            data class Error(val e: Exception) : Subscribe
            data class Success(val messages: KotlinChannel<Message>) : Subscribe
            data object QueueNotFound : Subscribe
        }

        sealed interface Remove : Result {
            data class Error(val e: Exception) : Remove
            data class Success(val messages: KotlinChannel<Message>) : Remove
            data object QueueNotFound : Remove
        }
    }

    suspend fun add(channel: Channel, message: Message): Result.Add
    suspend fun addAll(channel: Channel, messages: List<Message>): Result.Add
    suspend fun poll(channel: Channel): Result.Poll
    fun subscribe(channel: Channel): Result.Subscribe
    fun remove(channel: Channel): Result.Remove
}

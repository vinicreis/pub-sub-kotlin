package io.github.vinicreis.pubsub.server.channel.domain.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.model.Message
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
    }

    suspend fun add(channel: Channel, message: Message): Result.Add
    suspend fun addAll(channel: Channel, messages: List<Message>): Result.Add
    suspend fun poll(channel: Channel): Result.Poll
    fun subscribe(channel: Channel): Result.Subscribe
}

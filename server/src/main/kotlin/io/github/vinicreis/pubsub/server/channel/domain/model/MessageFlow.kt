package io.github.vinicreis.pubsub.server.channel.domain.model

import kotlinx.coroutines.flow.Flow

sealed interface MessageFlow {
    val messages: Flow<Message>
    suspend fun peek(): Message
    suspend fun post(vararg message: Message)
    fun close()
}

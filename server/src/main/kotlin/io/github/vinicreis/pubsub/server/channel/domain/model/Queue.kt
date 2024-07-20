package io.github.vinicreis.pubsub.server.channel.domain.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow

sealed class Queue(
    val id: String,
    val name: String = id,
) {
    class Simple(
        id: String,
        name: String = id,
    ) : Queue(id, name)

    class Multiple(
        id: String,
        name: String = id,
    ) : Queue(id, name) {
        private val mutableMessages = Channel<Message>(Channel.UNLIMITED)
        val messages: Flow<Message> = mutableMessages.receiveAsFlow()
        val nextMessage: Flow<Message> = mutableMessages.consumeAsFlow()

        suspend fun post(vararg message: Message) {
            message.forEach { mutableMessages.send(it) }
        }

        fun close() = mutableMessages.close()
    }
}

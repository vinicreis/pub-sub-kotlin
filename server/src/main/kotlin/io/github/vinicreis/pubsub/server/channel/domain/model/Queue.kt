package io.github.vinicreis.pubsub.server.channel.domain.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.*

sealed class Queue(
    val id: String,
    val name: String = id,
) {
    class Simple(
        id: String,
        name: String = id,
    ) : Queue(id, name) {
        private val subscribers = mutableMapOf<String, Channel<Message>>()

        fun subscribe(): Pair<String, Flow<Message>> {
            val subscriptionId = UUID.randomUUID().toString()

            return subscribers.getOrPut(subscriptionId) {
                Channel(Channel.UNLIMITED)
            }.receiveAsFlow().let { flow -> Pair(subscriptionId, flow) }
        }

        fun unsubscribe(subscriberId: String): Boolean {
            return subscribers.remove(subscriberId)?.close() ?: false
        }

        override suspend fun post(vararg message: Message) {

        }

        override fun close(): Boolean {
            subscribers.values.forEach { it.close() }
            subscribers.clear()

            return true
        }
    }

    class Multiple(
        id: String,
        name: String = id,
    ) : Queue(id, name) {
        private val mutableMessages = Channel<Message>(Channel.UNLIMITED)
        val messages: Flow<Message> = mutableMessages.receiveAsFlow()
        val nextMessage: Flow<Message> = mutableMessages.consumeAsFlow()

        override suspend fun post(vararg message: Message) {
            message.forEach { mutableMessages.send(it) }
        }

        override fun close() = mutableMessages.close()
    }

    abstract suspend fun post(vararg message: Message)
    abstract fun close(): Boolean
}

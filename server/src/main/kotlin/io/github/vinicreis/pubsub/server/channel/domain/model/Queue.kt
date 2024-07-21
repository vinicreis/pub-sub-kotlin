package io.github.vinicreis.pubsub.server.channel.domain.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.*
import kotlin.random.Random

sealed class Queue(
    val id: String,
    val name: String = id,
) {
    class Simple(
        id: String,
        name: String = id,
    ) : Queue(id, name) {
        private val subscribers = mutableMapOf<String, MutableSharedFlow<Message>>()

        fun subscribe(): Pair<String, Flow<Message>> {
            val subscriptionId = UUID.randomUUID().toString()

            return subscribers
                .getOrPut(subscriptionId) { MutableSharedFlow() }
                .let { flow -> Pair(subscriptionId, flow) }
        }

        fun unsubscribe(subscriberId: String) {
            subscribers.remove(subscriberId)
        }

        private fun <K, V> Map<K, V>.random(): Map.Entry<K, V> = entries.elementAt(Random.nextInt(size))

        override suspend fun post(vararg message: Message) {
            message.forEach {
                subscribers.random().also { (id, channel) ->
                    println("Posting message to subscription $id")

                    channel.emit(it)
                }
            }
        }

        override fun close() {
            subscribers.clear()
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

        override fun close() {
            mutableMessages.close()
        }
    }

    abstract suspend fun post(vararg message: Message)
    abstract fun close()
}

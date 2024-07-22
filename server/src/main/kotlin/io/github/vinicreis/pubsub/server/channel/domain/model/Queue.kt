package io.github.vinicreis.pubsub.server.channel.domain.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

sealed class Queue(
    val id: String,
    val name: String = id,
) {
    abstract val messages: Flow<Message>
    abstract suspend fun nextMessage(): Message

    class Simple(
        id: String,
        name: String = id,
    ) : Queue(id, name) {
        private val subscribers = ConcurrentHashMap<String, Channel<Message>>()
        private val queue = ConcurrentLinkedQueue<Message>()
        private val mutex = Mutex()

        override val messages: Flow<Message>
            get() = UUID.randomUUID().toString().let { uuid ->
                Channel<Message>(Channel.UNLIMITED).also { channel ->
                    subscribers[uuid] = channel
                }.receiveAsFlow()
                    .onStart { queue.poll()?.also { emit(it) } }
                    .onEach { message -> println("Message $message emitted to $uuid") }
                    .onCompletion { subscribers.remove(uuid) }
            }

        override suspend fun nextMessage(): Message = messages.first()

        private fun <K, V> Map<K, V>.choose(): V? =
            takeIf { isNotEmpty() }?.values?.elementAt(Random.nextInt(size))

        override suspend fun post(vararg message: Message) = mutex.withLock {
            message.forEach { message -> subscribers.choose()?.send(message) ?: queue.add(message) }
        }

        override fun close() {
            subscribers.forEach { (_, channel) -> channel.close() }
            subscribers.clear()
        }
    }

    class Multiple(
        id: String,
        name: String = id,
    ) : Queue(id, name) {
        private val mutableMessages = Channel<Message>(Channel.UNLIMITED)
        override val messages: Flow<Message> = mutableMessages.receiveAsFlow()

        override suspend fun nextMessage(): Message = messages.last()

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

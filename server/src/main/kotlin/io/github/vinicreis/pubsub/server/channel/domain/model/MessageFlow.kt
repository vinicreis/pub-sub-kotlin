package io.github.vinicreis.pubsub.server.channel.domain.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import java.util.*
import java.util.concurrent.ConcurrentHashMap

sealed class MessageFlow {
    abstract suspend fun peek(): Message
    abstract suspend fun post(vararg message: Message)

    protected val subscribers = ConcurrentHashMap<String, Channel<Message>>()
    protected val mutex = Mutex()

    val messages: Flow<Message>
        get() = UUID.randomUUID().toString().let { uuid ->
            Channel<Message>(Channel.UNLIMITED).also { channel ->
                subscribers[uuid] = channel
            }.receiveAsFlow()
                .onEach { message -> println("Message $message emitted to $uuid") }
                .onCompletion { subscribers.remove(uuid) }
        }

    fun close() {
        subscribers.forEach { (_, channel) -> channel.close() }
        subscribers.clear()
    }
}

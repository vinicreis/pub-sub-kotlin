package io.github.vinicreis.pubsub.server.channel.domain.model

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class SimpleMessageFlow : MessageFlow() {
    override suspend fun peek(): Message = messages.first()

    private fun <K, V> Map<K, V>.chooseOne(): V? =
        takeIf { isNotEmpty() }?.values?.elementAt(Random.nextInt(size))

    override suspend fun post(vararg message: Message) = mutex.withLock {
        message.forEach { message -> subscribers.chooseOne()?.send(message) }
    }
}

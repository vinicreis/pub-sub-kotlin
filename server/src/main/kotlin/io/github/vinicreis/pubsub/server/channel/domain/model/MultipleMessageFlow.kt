package io.github.vinicreis.pubsub.server.channel.domain.model

import kotlinx.coroutines.flow.last

class MultipleMessageFlow : MessageFlow() {
    override suspend fun peek(): Message = messages.last()

    override suspend fun post(vararg message: Message) {
        message.forEach {
            subscribers.values.forEach { subscriber ->
                subscriber.send(it)
            }
        }
    }
}

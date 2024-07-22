package io.github.vinicreis.pubsub.server.channel.domain.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel as KotlinChannel

class MultipleMessageFlow : MessageFlow {
    private val mutableMessages = KotlinChannel<Message>(KotlinChannel.UNLIMITED)
    override val messages: Flow<Message> = mutableMessages.receiveAsFlow()

    override suspend fun peek(): Message = messages.last()

    override suspend fun post(vararg message: Message) {
        message.forEach { mutableMessages.send(it) }
    }

    override fun close() {
        mutableMessages.close()
    }
}

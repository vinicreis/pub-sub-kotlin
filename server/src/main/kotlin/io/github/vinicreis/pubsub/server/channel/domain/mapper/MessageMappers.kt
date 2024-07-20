package io.github.vinicreis.pubsub.server.channel.domain.mapper

import io.github.vinicreis.domain.server.channel.model.textMessage
import io.github.vinicreis.pubsub.server.channel.domain.model.Message
import io.github.vinicreis.domain.server.channel.model.TextMessageOuterClass.TextMessage as RemoteTextMessage

internal val Message.asRemote: RemoteTextMessage
    get() = textMessage {
        this.content = this@asRemote.content
    }

internal val RemoteTextMessage.asDomain: Message
    get() = Message(
        content = this.content
    )

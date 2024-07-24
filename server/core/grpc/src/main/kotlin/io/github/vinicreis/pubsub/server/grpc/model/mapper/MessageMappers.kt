package io.github.vinicreis.pubsub.server.grpc.model.mapper

import io.github.vinicreis.domain.server.core.model.data.textMessage
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.domain.server.core.model.data.TextMessageOuterClass.TextMessage as RemoteTextMessage

internal val Message.asRemote: RemoteTextMessage
    get() = textMessage {
        this.content = this@asRemote.content
    }

internal val RemoteTextMessage.asDomain: Message
    get() = Message(
        content = this.content
    )

package io.github.vinicreis.pubsub.server.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.textMessage
import io.github.vinicreis.pubsub.server.core.model.data.Message
import java.util.*
import io.github.vinicreis.domain.server.core.model.data.TextMessageOuterClass.TextMessage as RemoteTextMessage

internal val Message.asRemote: RemoteTextMessage
    get() = textMessage {
        this.content = this@asRemote.content
    }

internal val RemoteTextMessage.asDomain: Message
    get() = Message(
        id = UUID.randomUUID(),
        content = this.content
    )

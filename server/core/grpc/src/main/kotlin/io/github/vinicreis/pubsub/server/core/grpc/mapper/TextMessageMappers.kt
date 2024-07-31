package io.github.vinicreis.pubsub.server.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.textMessage
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import java.util.*
import io.github.vinicreis.domain.server.core.model.data.TextMessageOuterClass.TextMessage as RemoteTextMessage

internal val TextMessage.asRemote: RemoteTextMessage
    get() = textMessage {
        this.content = this@asRemote.content
    }

internal val RemoteTextMessage.asDomain: TextMessage
    get() = TextMessage(
        id = UUID.randomUUID(),
        content = this.content
    )
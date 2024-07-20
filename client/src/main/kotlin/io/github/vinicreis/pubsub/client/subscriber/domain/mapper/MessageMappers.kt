package io.github.vinicreis.pubsub.client.subscriber.domain.mapper

import io.github.vinicreis.domain.server.channel.model.textMessage
import io.github.vinicreis.pubsub.client.subscriber.domain.model.Message
import io.github.vinicreis.domain.server.channel.model.TextMessageOuterClass.TextMessage as RemoteTextMessage

internal val Message.asRemote: RemoteTextMessage
    get() = textMessage {
        content = this@asRemote.content
    }

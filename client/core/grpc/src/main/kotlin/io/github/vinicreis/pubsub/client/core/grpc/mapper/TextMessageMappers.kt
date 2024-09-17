package io.github.vinicreis.pubsub.client.core.grpc.mapper

import io.github.vinicreis.pubsub.client.core.model.TextMessage
import io.github.vinicreis.pubsub.server.model.data.textMessage
import io.github.vinicreis.pubsub.server.model.data.TextMessageOuterClass.TextMessage as RemoteTextMessage

internal val TextMessage.asRemote: RemoteTextMessage
    get() = textMessage { content = this@asRemote.content }

internal val RemoteTextMessage.asDomain: TextMessage
    get() = TextMessage(content = content)

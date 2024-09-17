package io.github.vinicreis.pubsub.server.core.grpc.extension

import io.github.vinicreis.pubsub.server.model.data.TextMessageOuterClass.TextMessage
import io.github.vinicreis.pubsub.server.model.data.textMessage

val String.asRemoteMessage: TextMessage
    get() = textMessage { this.content = this@asRemoteMessage }

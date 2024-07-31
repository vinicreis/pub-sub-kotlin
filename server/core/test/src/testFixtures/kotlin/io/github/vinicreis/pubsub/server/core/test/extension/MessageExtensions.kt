package io.github.vinicreis.pubsub.server.core.test.extension

import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import java.util.*

val String.asTextMessage: TextMessage get() = TextMessage(
    id = UUID.randomUUID(),
    content = this
)

package io.github.vinicreis.pubsub.server.core.test.extension

import io.github.vinicreis.pubsub.server.core.model.data.Message
import java.util.*

val String.asMessage: Message get() = Message(
    id = UUID.randomUUID(),
    content = this
)

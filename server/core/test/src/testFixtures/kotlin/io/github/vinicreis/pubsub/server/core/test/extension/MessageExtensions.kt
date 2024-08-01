package io.github.vinicreis.pubsub.server.core.test.extension

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import java.util.*

fun String.asTextMessage(queue: Queue): TextMessage = TextMessage(
    id = UUID.randomUUID(),
    content = this,
    queue = queue,
)

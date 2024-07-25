package io.github.vinicreis.pubsub.server.core.test.extension

import io.github.vinicreis.pubsub.server.core.model.data.Message

val String.asMessage: Message get() = Message(this)

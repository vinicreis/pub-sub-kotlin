package io.github.vinicreis.pubsub.client.core.util.extension

import java.util.*

val String.asUUID: UUID get() = UUID.fromString(this)

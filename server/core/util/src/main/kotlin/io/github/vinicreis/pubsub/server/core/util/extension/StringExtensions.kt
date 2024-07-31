package io.github.vinicreis.pubsub.server.core.extension

import java.util.*

val String.asUuid: UUID get() = UUID.fromString(this)

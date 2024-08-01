package io.github.vinicreis.pubsub.server.core.model.data.event

data class ErrorEvent(val e: Exception) : Event

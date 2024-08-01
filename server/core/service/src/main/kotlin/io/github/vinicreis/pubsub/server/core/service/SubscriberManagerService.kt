package io.github.vinicreis.pubsub.server.core.service

import io.github.vinicreis.pubsub.server.core.model.data.Event
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import kotlinx.coroutines.flow.Flow

interface SubscriberManagerService {
    fun subscribe(queue: Queue): Flow<Event>
}

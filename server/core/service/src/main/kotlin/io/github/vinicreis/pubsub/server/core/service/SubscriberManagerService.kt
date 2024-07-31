package io.github.vinicreis.pubsub.server.core.service

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import kotlinx.coroutines.flow.Flow

interface SubscriberManagerService {
    fun subscribersCount(queue: Queue): Int
    fun subscribe(queue: Queue): Flow<TextMessage>
}

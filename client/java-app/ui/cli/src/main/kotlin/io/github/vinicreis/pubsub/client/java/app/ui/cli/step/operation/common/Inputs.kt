package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.common

import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient

suspend fun QueueServiceClient.withQueueList(
    block: suspend (List<Queue>) -> Unit
) {
    return when (val response = list()) {
        is QueueServiceClient.Response.ListAll.Fail ->
            throw RuntimeException("Failed to list queues: ${response.message}")

        is QueueServiceClient.Response.ListAll.Success -> block(response.queues)
    }
}

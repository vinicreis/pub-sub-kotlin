package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.common

import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.getInputOrNull
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

suspend fun QueueServiceClient.withQueueList(
    block: suspend (List<Queue>) -> Unit
) {
    return when (val response = list()) {
        is QueueServiceClient.Response.ListAll.Fail ->
            throw RuntimeException("Failed to list queues: ${response.message}")

        is QueueServiceClient.Response.ListAll.Success -> block(response.queues)
    }
}

fun getTimeout(): Long? = getInputOrNull(StringResource.Queue.Input.Message.ENTER_TIMEOUT_SECONDS)?.toLongOrNull()

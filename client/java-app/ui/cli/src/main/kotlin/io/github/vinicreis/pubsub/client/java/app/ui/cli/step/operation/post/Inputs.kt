package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post

import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.model.TextMessage
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.getInputOrNull
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.notNullable
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.selectOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

suspend fun List<Queue>.selectQueue(
    onQueueSelected: suspend (Queue) -> Unit
) {
    selectOption(
        message = StringResource.Queue.Input.Message.SELECT_AVAILABLE_QUEUES,
        options = this.map { it.name },
    )?.let { onQueueSelected(get(it)) } ?: error("Queue not selected")
}

suspend fun getMessage(): TextMessage {
    val content = getInputOrNull(StringResource.Message.Input.ENTER_CONTENT).notNullable {
        StringResource.Message.Validation.EMPTY_CONTENT
    }

    return TextMessage(content)
}

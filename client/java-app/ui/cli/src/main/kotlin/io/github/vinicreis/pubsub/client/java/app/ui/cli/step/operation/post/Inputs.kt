package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post

import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.model.TextMessage
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.getInputOrNull
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.selectOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

fun List<Queue>.selectQueue(): Queue {
    return selectOption(
        message = StringResource.Queue.Input.Message.SELECT_AVAILABLE_QUEUES,
        options = map { it.name },
    )?.let(::get) ?: error("Queue not selected")
}

fun getMessages(): List<TextMessage> = buildList {
    println(StringResource.Message.Input.ENTER_MULTIPLE_MESSAGES)

    do {
        val content = getInputOrNull(StringResource.Message.Input.ENTER_CONTENT)?.ifBlank { null }

        content?.also { add(TextMessage(it)) }
    } while(content != null)
}

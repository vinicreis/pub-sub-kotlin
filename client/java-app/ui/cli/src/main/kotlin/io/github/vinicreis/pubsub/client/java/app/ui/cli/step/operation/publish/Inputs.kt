package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish

import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.getInput
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.getInputOrNull
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.notNullable
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.selectOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource
import java.util.*

suspend fun getQueueData(): Queue {
    val code: String = getInputOrNull(message = StringResource.Queue.Input.Message.ENTER_CODE).notNullable {
        StringResource.Queue.Input.Validation.EMPTY_CODE
    }

    val name: String = getInput(StringResource.Queue.Input.Message.ENTER_NAME, code)

    val type: Queue.Type = selectOption(
        StringResource.Queue.Input.Message.SELECT_QUEUE_TYPE,
        Queue.Type.entries.map { StringResource.Queue.Type.name(it) }
    ).notNullable {
        StringResource.Queue.Input.Validation.INVALID_TYPE
    }.let { Queue.Type.fromOrdinal(it) ?: error(StringResource.Queue.Input.Validation.INVALID_TYPE) }

    return Queue(
        id = UUID.randomUUID(),
        code = code,
        name = name,
        type = type,
        pendingMessagesCount = 0
    )
}

package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post

import io.github.vinicreis.pubsub.client.core.model.Channel
import io.github.vinicreis.pubsub.client.core.model.Message
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.getInput
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.notNullable
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.selectOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

suspend fun List<Channel>.selectChannel(
    onChannelSelected: suspend (Channel) -> Unit
) {
    selectOption(
        message = StringResource.Channel.Input.Message.SELECT_AVAILABLE_CHANNELS,
        options = this.map { it.name },
    )?.let { onChannelSelected(get(it)) } ?: error("Channel not selected")
}

suspend fun getMessage(): Message {
    val content = getInput<String>(StringResource.Message.Input.ENTER_CONTENT).notNullable {
        StringResource.Message.Validation.EMPTY_CONTENT
    }

    return Message(content)
}

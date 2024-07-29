package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish

import io.github.vinicreis.pubsub.client.core.model.Channel
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.getInput
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.notNullable
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.selectOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource
import java.util.*

suspend fun getChannelData(): Channel {
    val code: String =
        getInput<String>(StringResource.Channel.Input.Message.ENTER_CODE)
            .notNullable { StringResource.Channel.Input.Validation.EMPTY_CODE }
    val name: String =
        getInput<String>(StringResource.Channel.Input.Message.ENTER_NAME, code)
    val type: Channel.Type =
        selectOption(
            StringResource.Channel.Input.Message.SELECT_CHANNEL_TYPE,
            Channel.Type.entries.map { StringResource.Channel.Type.name(it) }
        ).notNullable {
            StringResource.Channel.Input.Validation.INVALID_TYPE
        }.let { Channel.Type.fromOrdinal(it) ?: error(StringResource.Channel.Input.Validation.INVALID_TYPE) }

    return Channel(
        id = UUID.randomUUID(),
        code = code,
        name = name,
        type = type,
        pendingMessagesCount = 0
    )
}

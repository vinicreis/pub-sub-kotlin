package io.github.vinicreis.pubsub.server.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.channel
import io.github.vinicreis.pubsub.server.core.extension.asUuid
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import java.util.*
import io.github.vinicreis.domain.server.core.model.data.ChannelOuterClass.Channel as RemoteChannel

internal val RemoteChannel.Type.asDomain: Channel.Type
    get() = when(this) {
        RemoteChannel.Type.SIMPLE -> Channel.Type.SIMPLE
        RemoteChannel.Type.MULTIPLE -> Channel.Type.MULTIPLE
        RemoteChannel.Type.UNRECOGNIZED -> error("Unknown channel type received: $this")
    }

internal val RemoteChannel.asDomain: Channel
    get() = Channel(
        id = this.id.takeIf { it.isNotBlank() }?.asUuid ?: UUID.randomUUID(),
        code = this.code,
        type = this.type.asDomain,
        name = this.name,
        pendingMessagesCount = this.pendingMessagesCount
    )

internal val Channel.Type.asRemote: RemoteChannel.Type
    get() = when(this) {
        Channel.Type.SIMPLE -> RemoteChannel.Type.SIMPLE
        Channel.Type.MULTIPLE -> RemoteChannel.Type.MULTIPLE
    }

internal val Channel.asRemote: RemoteChannel
    get() = channel {
        id = this@asRemote.id.toString()
        code = this@asRemote.code
        type = this@asRemote.type.asRemote
        name = this@asRemote.name
        pendingMessagesCount = this@asRemote.pendingMessagesCount
    }

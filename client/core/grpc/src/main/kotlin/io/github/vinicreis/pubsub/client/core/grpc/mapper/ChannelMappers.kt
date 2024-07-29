package io.github.vinicreis.pubsub.client.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.channel
import io.github.vinicreis.pubsub.client.core.model.Channel
import io.github.vinicreis.pubsub.client.core.util.extension.asUUID
import io.github.vinicreis.domain.server.core.model.data.ChannelOuterClass.Channel as RemoteChannel

internal val Channel.Type.asRemote: RemoteChannel.Type
    get() = when (this) {
        Channel.Type.SIMPLE -> RemoteChannel.Type.SIMPLE
        Channel.Type.MULTIPLE -> RemoteChannel.Type.MULTIPLE
    }

internal val RemoteChannel.Type.asDomain: Channel.Type
    get() = when (this) {
        RemoteChannel.Type.SIMPLE -> Channel.Type.SIMPLE
        RemoteChannel.Type.MULTIPLE -> Channel.Type.MULTIPLE
        RemoteChannel.Type.UNRECOGNIZED -> error("Unrecognized channel type")
    }

internal val RemoteChannel.asDomain: Channel
    get() = Channel(
        id = id.asUUID,
        code = code,
        name = name,
        type = type.asDomain,
        pendingMessagesCount = pendingMessagesCount
    )

internal val Channel.asRemote: RemoteChannel
    get() = channel {
        this.id = this@asRemote.id.toString()
        this.code = this@asRemote.code
        this.name = this@asRemote.name
        this.type = this@asRemote.type.asRemote
    }

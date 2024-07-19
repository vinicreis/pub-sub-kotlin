package io.github.vinicreis.pubsub.server.channel.domain.mapper

import io.github.vinicreis.domain.server.channel.model.channel
import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.domain.server.channel.model.ChannelOuterClass.Channel as RemoteChannel

internal val Channel.Type.asRemote: RemoteChannel.Type
    get() = when (this) {
        Channel.Type.SIMPLE -> RemoteChannel.Type.SIMPLE
        Channel.Type.MULTIPLE -> RemoteChannel.Type.MULTIPLE
    }

internal val RemoteChannel.Type.asDomain: Channel.Type
    get() = when (this) {
        RemoteChannel.Type.SIMPLE -> Channel.Type.SIMPLE
        RemoteChannel.Type.MULTIPLE -> Channel.Type.MULTIPLE
        RemoteChannel.Type.UNRECOGNIZED -> error("Unrecognized channel type $this")
    }

internal val Channel.asRemote: RemoteChannel
    get() = channel {
        id = this@asRemote.id
        type = this@asRemote.type.asRemote
        name = this@asRemote.name
    }
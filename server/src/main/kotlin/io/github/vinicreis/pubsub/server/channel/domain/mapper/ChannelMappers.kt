package io.github.vinicreis.pubsub.server.channel.domain.mapper

import io.github.vinicreis.domain.server.channel.model.ChannelOuterClass
import io.github.vinicreis.domain.server.channel.model.channel
import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.domain.server.channel.model.ChannelOuterClass.Channel as RemoteChannel

internal val RemoteChannel.Type.asDomain: Channel.Type
    get() = when(this) {
        RemoteChannel.Type.SIMPLE -> Channel.Type.SIMPLE
        RemoteChannel.Type.MULTIPLE -> Channel.Type.MULTIPLE
        ChannelOuterClass.Channel.Type.UNRECOGNIZED -> error("Unknown channel type received: $this")
    }

private val Channel.Type.asRemote: RemoteChannel.Type
    get() = when(this) {
        Channel.Type.SIMPLE -> RemoteChannel.Type.SIMPLE
        Channel.Type.MULTIPLE -> RemoteChannel.Type.MULTIPLE
    }

internal val Channel.asRemote: RemoteChannel
    get() = channel {
        id = this@asRemote.id
        type = this@asRemote.type.asRemote
        name = this@asRemote.name
    }
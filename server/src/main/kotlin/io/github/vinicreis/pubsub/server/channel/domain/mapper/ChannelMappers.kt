package io.github.vinicreis.pubsub.server.channel.domain.mapper

import io.github.vinicreis.domain.server.channel.model.channel
import io.github.vinicreis.pubsub.server.channel.domain.model.Queue
import io.github.vinicreis.domain.server.channel.model.ChannelOuterClass.Channel as RemoteChannel

private val Queue.remoteType: RemoteChannel.Type
    get() = when(this) {
        is Queue.Simple -> RemoteChannel.Type.SIMPLE
        is Queue.Multiple -> RemoteChannel.Type.MULTIPLE
    }

internal val Queue.asRemote: RemoteChannel
    get() = channel {
        id = this@asRemote.id
        type = this@asRemote.remoteType
        name = this@asRemote.name
    }
package io.github.vinicreis.pubsub.client.subscriber.domain.mapper

import io.github.vinicreis.pubsub.client.subscriber.domain.model.Channel
import io.github.vinicreis.domain.server.channel.model.ChannelOuterClass.Channel as RemoteChannel

internal val Channel.Type.asRemove: RemoteChannel.Type
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
        id = id,
        name = name,
        type = when (type) {
            RemoteChannel.Type.SIMPLE -> Channel.Type.SIMPLE
            RemoteChannel.Type.MULTIPLE -> Channel.Type.MULTIPLE
            RemoteChannel.Type.UNRECOGNIZED -> error("Unrecognized channel type")
            null -> error("Null channel type")
        }
    )

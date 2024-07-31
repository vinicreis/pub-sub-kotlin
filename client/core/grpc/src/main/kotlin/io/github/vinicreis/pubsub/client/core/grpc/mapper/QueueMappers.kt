package io.github.vinicreis.pubsub.client.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.queue
import io.github.vinicreis.pubsub.client.core.model.Queue
import io.github.vinicreis.pubsub.client.core.util.extension.asUUID
import io.github.vinicreis.domain.server.core.model.data.QueueOuterClass.Queue as RemoteQueue

internal val Queue.Type.asRemote: RemoteQueue.Type
    get() = when (this) {
        Queue.Type.SIMPLE -> RemoteQueue.Type.SIMPLE
        Queue.Type.MULTIPLE -> RemoteQueue.Type.MULTIPLE
    }

internal val RemoteQueue.Type.asDomain: Queue.Type
    get() = when (this) {
        RemoteQueue.Type.SIMPLE -> Queue.Type.SIMPLE
        RemoteQueue.Type.MULTIPLE -> Queue.Type.MULTIPLE
        RemoteQueue.Type.UNRECOGNIZED -> error("Unrecognized queue type")
    }

internal val RemoteQueue.asDomain: Queue
    get() = Queue(
        id = id.asUUID,
        code = code,
        name = name,
        type = type.asDomain,
        pendingMessagesCount = pendingMessagesCount
    )

internal val Queue.asRemote: RemoteQueue
    get() = queue {
        this.id = this@asRemote.id.toString()
        this.code = this@asRemote.code
        this.name = this@asRemote.name
        this.type = this@asRemote.type.asRemote
    }

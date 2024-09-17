package io.github.vinicreis.pubsub.server.core.grpc.mapper

import io.github.vinicreis.pubsub.server.core.extension.asUuid
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.model.data.queue
import java.util.*
import io.github.vinicreis.pubsub.server.model.data.QueueOuterClass.Queue as RemoteQueue

internal val RemoteQueue.Type.asDomain: Queue.Type
    get() = when(this) {
        RemoteQueue.Type.SIMPLE -> Queue.Type.SIMPLE
        RemoteQueue.Type.MULTIPLE -> Queue.Type.MULTIPLE
        RemoteQueue.Type.UNRECOGNIZED -> error("Unknown queue type received: $this")
    }

internal val RemoteQueue.asDomain: Queue
    get() = Queue(
        id = this.id.takeIf { it.isNotBlank() }?.asUuid ?: UUID.randomUUID(),
        code = this.code,
        type = this.type.asDomain,
        name = this.name,
        pendingMessagesCount = this.pendingMessagesCount
    )

internal val Queue.Type.asRemote: RemoteQueue.Type
    get() = when(this) {
        Queue.Type.SIMPLE -> RemoteQueue.Type.SIMPLE
        Queue.Type.MULTIPLE -> RemoteQueue.Type.MULTIPLE
    }

internal val Queue.asRemote: RemoteQueue
    get() = queue {
        id = this@asRemote.id.toString()
        code = this@asRemote.code
        type = this@asRemote.type.asRemote
        name = this@asRemote.name
        pendingMessagesCount = this@asRemote.pendingMessagesCount
    }

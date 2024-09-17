package io.github.vinicreis.pubsub.server.core.grpc.mapper

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.model.data.queue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import io.github.vinicreis.pubsub.server.model.data.QueueOuterClass.Queue as RemoteQueue

class QueueMapperTests {
    @Test
    fun `Should map domain queue to remote queue properly`() {
        val remoteQueue: RemoteQueue = queue {
            id = UUID.randomUUID().toString()
            name = "queue-1"
            type = RemoteQueue.Type.SIMPLE
            pendingMessagesCount = 3
        }

        remoteQueue.asDomain.also { domainQueue ->
            assertEquals(remoteQueue.id, domainQueue.id.toString())
            assertEquals(remoteQueue.type.asDomain, domainQueue.type)
            assertEquals(remoteQueue.name, domainQueue.name)
            assertEquals(remoteQueue.pendingMessagesCount, domainQueue.pendingMessagesCount)
        }
    }

    @Test
    fun `Should throw exception when mapping unknown queue type`() {
        assertThrows<IllegalStateException> {
            RemoteQueue.Type.UNRECOGNIZED.asDomain
        }
    }

    @Test
    fun `Should map remote queue to domain queue properly`() {
        val domainQueue = Queue(
            id = QueueFixture.id(),
            code = "queue-3",
            name = "Queue 5",
            type = Queue.Type.MULTIPLE,
            pendingMessagesCount = 3,
        )

        domainQueue.asRemote.also { remoteQueue ->
            assertEquals(domainQueue.id.toString(), remoteQueue.id)
            assertEquals(domainQueue.type.asRemote, remoteQueue.type)
            assertEquals(domainQueue.name, remoteQueue.name)
            assertEquals(domainQueue.pendingMessagesCount, remoteQueue.pendingMessagesCount)
        }
    }
}

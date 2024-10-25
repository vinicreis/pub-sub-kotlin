package io.github.vinicreis.pubsub.server.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.textMessage
import io.github.vinicreis.pubsub.server.core.test.extension.asTextMessage
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.TextMessageFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.github.vinicreis.domain.server.core.model.data.TextMessageOuterClass.TextMessage as RemoteTextMessage

class TextMessageMapperTests {
    @Test
    fun `Should map domain message to remote message properly`() {
        val queue = QueueFixture.instance()
        val remoteMessage: RemoteTextMessage = textMessage {
            content = TextMessageFixture.any()
        }

        remoteMessage.asDomain(queue).also { domainMessage ->
            assertEquals(remoteMessage.content, domainMessage.content)
        }
    }

    @Test
    fun `Should map remote channel to domain channel properly`() {
        val queue = QueueFixture.instance()
        val domainMessage = TextMessageFixture.any().asTextMessage(queue)

        domainMessage.asRemote.also { remoteMessage ->
            assertEquals(domainMessage.content, remoteMessage.content)
        }
    }
}

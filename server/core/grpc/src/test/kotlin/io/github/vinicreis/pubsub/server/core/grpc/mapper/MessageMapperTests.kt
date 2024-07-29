package io.github.vinicreis.pubsub.server.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.textMessage
import io.github.vinicreis.pubsub.server.core.test.extension.asMessage
import io.github.vinicreis.pubsub.server.core.test.fixture.MessageFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.github.vinicreis.domain.server.core.model.data.TextMessageOuterClass.TextMessage as RemoteTextMessage

class MessageMapperTests {
    @Test
    fun `Should map domain message to remote message properly`() {
        val remoteMessage: RemoteTextMessage = textMessage {
            content = MessageFixture.any()
        }

        remoteMessage.asDomain.also { domainMessage ->
            assertEquals(remoteMessage.content, domainMessage.content)
        }
    }

    @Test
    fun `Should map remote channel to domain channel properly`() {
        val domainMessage = MessageFixture.any().asMessage

        domainMessage.asRemote.also { remoteMessage ->
            assertEquals(domainMessage.content, remoteMessage.content)
        }
    }
}

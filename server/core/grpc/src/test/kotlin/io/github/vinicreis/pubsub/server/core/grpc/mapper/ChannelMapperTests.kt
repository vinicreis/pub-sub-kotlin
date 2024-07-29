package io.github.vinicreis.pubsub.server.core.grpc.mapper

import io.github.vinicreis.domain.server.core.model.data.channel
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.test.fixture.ChannelFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import io.github.vinicreis.domain.server.core.model.data.ChannelOuterClass.Channel as RemoteChannel

class ChannelMapperTests {
    @Test
    fun `Should map domain channel to remote channel properly`() {
        val remoteChannel: RemoteChannel = channel {
            id = UUID.randomUUID().toString()
            name = "channel-1"
            type = RemoteChannel.Type.SIMPLE
            pendingMessagesCount = 3
        }

        remoteChannel.asDomain.also { domainChannel ->
            assertEquals(remoteChannel.id, domainChannel.id.toString())
            assertEquals(remoteChannel.type.asDomain, domainChannel.type)
            assertEquals(remoteChannel.name, domainChannel.name)
            assertEquals(remoteChannel.pendingMessagesCount, domainChannel.pendingMessagesCount)
        }
    }

    @Test
    fun `Should throw exception when mapping unknown channel type`() {
        assertThrows<IllegalStateException> {
            RemoteChannel.Type.UNRECOGNIZED.asDomain
        }
    }

    @Test
    fun `Should map remote channel to domain channel properly`() {
        val domainChannel = Channel(
            id = ChannelFixture.id(),
            code = "channel-3",
            name = "Channel 5",
            type = Channel.Type.MULTIPLE,
            pendingMessagesCount = 3,
        )

        domainChannel.asRemote.also { remoteChannel ->
            assertEquals(domainChannel.id.toString(), remoteChannel.id)
            assertEquals(domainChannel.type.asRemote, remoteChannel.type)
            assertEquals(domainChannel.name, remoteChannel.name)
            assertEquals(domainChannel.pendingMessagesCount, remoteChannel.pendingMessagesCount)
        }
    }
}

package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.domain.server.core.model.data.ResultOuterClass
import io.github.vinicreis.domain.server.core.model.data.SubscriptionStatusOuterClass.SubscriptionStatus
import io.github.vinicreis.domain.server.core.model.data.textMessage
import io.github.vinicreis.domain.server.core.model.request.addRequest
import io.github.vinicreis.domain.server.core.model.request.listRequest
import io.github.vinicreis.domain.server.core.model.request.publishMultipleRequest
import io.github.vinicreis.domain.server.core.model.request.publishSingleRequest
import io.github.vinicreis.domain.server.core.model.request.removeByIdRequest
import io.github.vinicreis.domain.server.core.model.request.subscribeRequest
import io.github.vinicreis.domain.server.core.model.response.SubscribeResponse
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asDomain
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asRemote
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.logging.Logger
import kotlin.random.Random
import io.github.vinicreis.domain.server.core.model.data.ChannelOuterClass.Channel as RemoteChannel
import io.github.vinicreis.domain.server.core.model.data.TextMessageOuterClass.TextMessage as RemoteTextMessage
import kotlinx.coroutines.channels.Channel as KotlinChannel

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelServiceGrpcTests {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val channelRepositoryMock = mockk<ChannelRepository>()
    private val messageRepositoryMock = mockk<MessageRepository>()
    private val subscriberManagerServiceMock = mockk<SubscriberManagerService>()
    private val sut = ChannelServiceGrpc(
        port = 8080,
        coroutineContext = testDispatcher,
        logger = Logger.getLogger("ChannelServiceGrpcTests"),
        channelRepository = channelRepositoryMock,
        messageRepository = messageRepositoryMock,
        subscriberManagerService = subscriberManagerServiceMock
    )

    @Nested
    @DisplayName("Add method tests")
    inner class AddTests {
        @Test
        fun `Should return success response when channel is added successfully`() = runTest(testDispatcher) {
            val channel = channel()
            val request = addRequest { this.channel = channel.asRemote }

            coEvery { channelRepositoryMock.add(channel) } returns ChannelResult.Add.success(channel)

            val response = sut.add(request)

            coVerify(exactly = 1) { channelRepositoryMock.add(channel) }
            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertEquals(channel, response.channel.asDomain)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return error response when channel fails to be added`() = runTest(testDispatcher) {
            val channel = channel()
            val request = addRequest { this.channel = channel.asRemote }

            coEvery { channelRepositoryMock.add(channel) } returns ChannelResult.Add.error("Error message 1")

            val response = sut.add(request)

            coVerify(exactly = 1) { channelRepositoryMock.add(channel) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
            assertEquals("Error message 1", response.message)
        }

        @Test
        fun `Should return error with already found response when channel already exists`() = runTest(testDispatcher) {
            val channel = channel()
            val request = addRequest { this.channel = channel.asRemote }

            coEvery { channelRepositoryMock.add(channel) } returns ChannelResult.Add.alreadyFound()

            val response = sut.add(request)

            coVerify(exactly = 1) { channelRepositoryMock.add(channel) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
            assertTrue(response.message.contains("already exists"))
            assertTrue(response.message.contains(channel.id))
        }

        @Test
        fun `Should return error with exception message when its thrown while adding`() = runTest(testDispatcher) {
            val channel = channel()
            val request = addRequest { this.channel = channel.asRemote }

            coEvery { channelRepositoryMock.add(channel) } throws RuntimeException("Some error message")

            val response = sut.add(request)

            coVerify(exactly = 1) { channelRepositoryMock.add(channel) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
            assertEquals(GENERIC_ERROR_MESSAGE, response.message)
        }
    }

    @Nested
    @DisplayName("List method tests")
    inner class ListTests {
        @Test
        fun `Should return success response when channels are successfully fetch`() = runTest(testDispatcher) {
            val request = listRequest { }
            val channels = buildList {
                repeat(Random.nextInt(20)) {
                    add(channel())
                }
            }

            coEvery { channelRepositoryMock.getAll() } returns ChannelResult.List.success(channels)

            val response = sut.list(request)

            coVerify(exactly = 1) { channelRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertEquals(channels.map(Channel::asRemote), response.channelsList)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return success response with empty list if no channel is found`() = runTest(testDispatcher) {
            val request = listRequest { }

            coEvery { channelRepositoryMock.getAll() } returns ChannelResult.List.success(emptyList())

            val response = sut.list(request)

            coVerify(exactly = 1) { channelRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertTrue(response.channelsList.isEmpty())
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return error with exception message when channels get fails`() = runTest(testDispatcher) {
            val request = listRequest { }

            coEvery { channelRepositoryMock.getAll() } returns ChannelResult.List.error("Failed to get again")

            val response = sut.list(request)

            coVerify(exactly = 1) { channelRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertTrue(response.channelsList.isEmpty())
            assertEquals("Failed to get again", response.message)
        }

        @Test
        fun `Should return error with exception message when its thrown`() = runTest(testDispatcher) {
            val request = listRequest { }

            coEvery { channelRepositoryMock.getAll() } throws RuntimeException("Oops...")

            val response = sut.list(request)

            coVerify(exactly = 1) { channelRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertTrue(response.channelsList.isEmpty())
            assertEquals(GENERIC_ERROR_MESSAGE, response.message)
        }
    }

    @Nested
    @DisplayName("Remove method tests")
    inner class RemoveTests {
        @Test
        fun `Should return success response when valid channel is removed`() =
            runTest(testDispatcher) {
                val id = id()
                val request = removeByIdRequest { this.id = id }
                val channel = channel(id)

                coEvery { channelRepositoryMock.removeById(id) } returns ChannelResult.Remove.success(channel)
                coEvery { messageRepositoryMock.remove(channel) } returns MessageResult.Remove.success()

                val response = sut.removeById(request)

                coVerify(exactly = 1) { channelRepositoryMock.removeById(id) }
                coVerify(exactly = 1) { messageRepositoryMock.remove(channel) }
                assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                assertEquals(channel.asRemote, response.channel)
                assertTrue(response.message.isEmpty())
            }

        @Test
        fun `Should return error response if the removed channel is not found`() = runTest(testDispatcher) {
            val id = id()
            val request = removeByIdRequest { this.id = id }
            val channel = channel(id)

            coEvery { channelRepositoryMock.removeById(id) } returns ChannelResult.Remove.notFound()
            coEvery { messageRepositoryMock.remove(channel) } answers {
                fail("Should not call message repository remove if channel is not found")
            }

            val response = sut.removeById(request)

            coVerify(exactly = 1) { channelRepositoryMock.removeById(id) }
            coVerify(exactly = 0) { messageRepositoryMock.remove(channel) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
            assertTrue(response.message.contains("not found"))
        }

        @Test
        fun `Should return error response if the removed channel message queue is not found`() =
            runTest(testDispatcher) {
                val id = id()
                val request = removeByIdRequest { this.id = id }
                val channel = channel()

                coEvery { channelRepositoryMock.removeById(id) } returns ChannelResult.Remove.success(channel)
                coEvery { messageRepositoryMock.remove(channel) } returns MessageResult.Remove.notFound()

                val response = sut.removeById(request)

                coVerify(exactly = 1) { channelRepositoryMock.removeById(id) }
                coVerify(exactly = 1) { messageRepositoryMock.remove(channel) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                assertTrue(response.message.contains("message queue not found"))
            }

        @Test
        fun `Should return error with exception message when its thrown while removing channel`() =
            runTest(testDispatcher) {
                val id = id()
                val request = removeByIdRequest { this.id = id }
                val channel = channel()

                coEvery { channelRepositoryMock.removeById(id) } returns ChannelResult.Remove.success(channel)
                coEvery { messageRepositoryMock.remove(channel) } throws RuntimeException("Any error happened!")

                val response = sut.removeById(request)

                coVerify(exactly = 1) { channelRepositoryMock.removeById(id) }
                coVerify(exactly = 1) { messageRepositoryMock.remove(channel) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                assertEquals(GENERIC_ERROR_MESSAGE, response.message)
            }
    }

    @Nested
    @DisplayName("Publish method tests")
    inner class PublishTests {
        @Nested
        @DisplayName("Single message publish tests")
        inner class PublishSingleTests {
            @Test
            fun `Should return success response when a message is published in valid channel`() =
                runTest(testDispatcher) {
                    val id = id()
                    val channel = channel()
                    val message = randomMessage()
                    val request = publishSingleRequest {
                        this.channelId = id
                        this.content = message.asRemoteMessage
                    }

                    coEvery { channelRepositoryMock.getById(id) } returns ChannelResult.GetById.success(channel)
                    coEvery {
                        messageRepositoryMock.add(channel, message.asDomainMessage)
                    } returns MessageResult.Add.success()

                    val response = sut.publishSingle(request)

                    coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { messageRepositoryMock.add(channel, message.asDomainMessage) }
                    assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                    assertEquals(channel.asRemote, response.channel)
                    assertTrue(response.message.isEmpty())
                }

            @Test
            fun `Should return error response if the channel is not found`() = runTest(testDispatcher) {
                val id = id()
                val channel = channel()
                val message = randomMessage()
                val request = publishSingleRequest {
                    this.channelId = id
                    this.content = message.asRemoteMessage
                }

                coEvery { channelRepositoryMock.getById(id) } returns ChannelResult.GetById.notFound()
                coEvery {
                    messageRepositoryMock.add(channel, message.asDomainMessage)
                } answers { fail("Should not be called if channel is not found") }

                val response = sut.publishSingle(request)

                coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                coVerify(exactly = 0) { messageRepositoryMock.add(any(), any()) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                assertTrue(response.message.contains("not found"))
            }

            @Test
            fun `Should return error with exception message when its thrown while publishing message`() =
                runTest(testDispatcher) {
                    val id = id()
                    val channel = channel()
                    val message = randomMessage()
                    val request = publishSingleRequest {
                        this.channelId = id
                        this.content = message.asRemoteMessage
                    }

                    coEvery { channelRepositoryMock.getById(id) } returns ChannelResult.GetById.success(channel)
                    coEvery {
                        messageRepositoryMock.add(channel, message.asDomainMessage)
                    } throws RuntimeException(randomMessage())

                    val response = sut.publishSingle(request)

                    coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { messageRepositoryMock.add(channel, message.asDomainMessage) }
                    assertEquals(ResultOuterClass.Result.ERROR, response.result)
                    assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                    assertEquals(GENERIC_ERROR_MESSAGE, response.message)
                }
        }

        @Nested
        @DisplayName("Multiple message publish tests")
        inner class PublishMultipleTests {
            @Test
            fun `Should return success response when messages are published in valid channel`() =
                runTest(testDispatcher) {
                    val id = id()
                    val channel = channel()
                    val messages = MESSAGE_LIST.randomSlice()
                    val request = publishMultipleRequest {
                        this.channelId = id
                        this.content.addAll(messages.map { it.asRemoteMessage })
                    }

                    coEvery { channelRepositoryMock.getById(id) } returns ChannelResult.GetById.success(channel)
                    coEvery {
                        messageRepositoryMock.addAll(channel, messages.map { it.asDomainMessage })
                    } returns MessageResult.Add.success()

                    val response = sut.publishMultiple(request)

                    coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { messageRepositoryMock.addAll(channel, messages.map { it.asDomainMessage }) }
                    assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                    assertEquals(channel.asRemote, response.channel)
                    assertTrue(response.message.isEmpty())
                }

            @Test
            fun `Should return error response if the channel is not found`() = runTest(testDispatcher) {
                val id = id()
                val channel = channel()
                val messages = MESSAGE_LIST.randomSlice()
                val request = publishMultipleRequest {
                    this.channelId = id
                    this.content.addAll(messages.map { it.asRemoteMessage })
                }

                coEvery { channelRepositoryMock.getById(id) } returns ChannelResult.GetById.notFound()
                coEvery {
                    messageRepositoryMock.addAll(channel, messages.map { it.asDomainMessage })
                } answers { fail("Should not be called if channel is not found") }

                val response = sut.publishMultiple(request)

                coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                coVerify(exactly = 0) { messageRepositoryMock.add(any(), any()) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                assertTrue(response.message.contains("not found"))
            }

            @Test
            fun `Should return error with exception message when its thrown while publishing messages`() =
                runTest(testDispatcher) {
                    val id = id()
                    val channel = channel()
                    val messages = MESSAGE_LIST.randomSlice()
                    val request = publishMultipleRequest {
                        this.channelId = id
                        this.content.addAll(messages.map { it.asRemoteMessage })
                    }

                    coEvery { channelRepositoryMock.getById(id) } returns ChannelResult.GetById.success(channel)
                    coEvery {
                        messageRepositoryMock.addAll(channel, messages.map { it.asDomainMessage })
                    } throws RuntimeException(randomMessage())

                    val response = sut.publishMultiple(request)

                    coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { messageRepositoryMock.addAll(channel, messages.map { it.asDomainMessage }) }
                    assertEquals(ResultOuterClass.Result.ERROR, response.result)
                    assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                    assertEquals(GENERIC_ERROR_MESSAGE, response.message)
                }
        }
    }

    @Nested
    @DisplayName("Subscribe method tests")
    inner class SubscribeTests {
        @Test
        fun `Should emit processing when channel processing starts`() = runTest(testDispatcher) {
            val id = id()
            val channel = channel(id)
            val kotlinChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)
            val request = subscribeRequest { this.channelId = id }

            coEvery { channelRepositoryMock.getById(id) } returns ChannelResult.GetById.success(channel)
            every { subscriberManagerServiceMock.subscribe(channel) } returns kotlinChannel.receiveAsFlow()

            val subscriber = sut.subscribe(request)
            val response = subscriber.first()

            coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
            verify(exactly = 1) { subscriberManagerServiceMock.subscribe(channel) }
            assertEquals(SubscriptionStatus.PROCESSING, response.status)
            assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
            assertEquals(RemoteTextMessage.getDefaultInstance(), response.content)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should emit finished status with error when channel is not found`() = runTest(testDispatcher) {
            val id = id()
            val channel = channel(id)
            val request = subscribeRequest { this.channelId = id }
            val responses = mutableListOf<SubscribeResponse>()

            coEvery { channelRepositoryMock.getById(id) } returns ChannelResult.GetById.notFound()
            every { subscriberManagerServiceMock.subscribe(channel) } answers {
                fail("Should not be called if channel is not found")
            }

            val subscriber = sut.subscribe(request)

            backgroundScope.launch { subscriber.toList(responses) }

            coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
            verify(exactly = 0) { subscriberManagerServiceMock.subscribe(any()) }


            assertEquals(SubscriptionStatus.PROCESSING, response.status)
            assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
            assertEquals(RemoteTextMessage.getDefaultInstance(), response.content)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should emit finished status with error when channel fails to be fetch`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should emit active status when subscriber starts collecting results`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should emit update status when some message is sent on message queue`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should emit finish status if the message queue completes`() = runTest(testDispatcher) {

        }
    }

    @Nested
    @DisplayName("Peek method tests")
    inner class PeekTests {
        @Test
        fun `Should return next emitted message successfully in a valid channel`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should return error response when the channel is not found`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should return error response with exception message thrown while getting channel`() =
            runTest(testDispatcher) {

            }

        @Test
        fun `Should fail by timeout in case no message is emitted in the sent seconds time`() =
            runTest(testDispatcher) {

            }

        @Test
        fun `Should return the first emitted message only in case more than one is emitted on message queue`() =
            runTest(testDispatcher) {

            }
    }

    companion object {
        private const val GENERIC_ERROR_MESSAGE = "Something went wrong..."
        private val MESSAGE_LIST = listOf(
            "Any message", "One more message", "This is a test message", "Hello, World!",
            "Sample message", "Another message", "Random text", "Message content",
            "Test data", "Default message"
        )

        fun id(): String = "channel-" + Random.nextInt(100_000)

        fun channel(
            id: String = id(),
            name: String = "Channel 1",
            type: Channel.Type = Channel.Type.SIMPLE,
            pendingMessagesCount: Int = Random.nextInt(1000),
        ) = Channel(
            id = id,
            name = name,
            type = type,
            pendingMessagesCount = pendingMessagesCount,
        )

        private val String.asDomainMessage: Message get() = Message(this)

        private val String.asRemoteMessage: RemoteTextMessage
            get() = textMessage { this.content = this@asRemoteMessage }

        private fun <E> Collection<E>.randomItem(): E = takeIf { it.isNotEmpty() }?.elementAt(Random.nextInt(size))
            ?: error("List can not be empty to get a random element")

        private fun <E> Collection<E>.randomSlice(): List<E> = takeIf { it.isNotEmpty() }?.let {
            buildList { repeat(Random.nextInt(this@randomSlice.size)) { add(this@randomSlice.randomItem()) } }
        } ?: error("List can not be empty to get a random element")

        private fun randomMessage(): String = MESSAGE_LIST.randomItem()

        object ChannelResult {
            object Add {
                fun success(channel: Channel = channel()) = ChannelRepository.Result.Add.Success(channel)
                fun alreadyFound() = ChannelRepository.Result.Add.AlreadyFound
                fun error(message: String = "Failed to add") =
                    ChannelRepository.Result.Add.Error(RuntimeException(message))
            }

            object List {
                fun success(
                    channels: kotlin.collections.List<Channel> = listOf(channel(), channel())
                ) = ChannelRepository.Result.GetAll.Success(channels)

                fun error(message: String = "Failed to list") =
                    ChannelRepository.Result.GetAll.Error(RuntimeException(message))
            }

            object Remove {
                fun success(channel: Channel = channel()) = ChannelRepository.Result.Remove.Success(channel)
                fun error(message: String = "Failed to remove") =
                    ChannelRepository.Result.Remove.Error(RuntimeException(message))

                fun notFound() = ChannelRepository.Result.Remove.NotFound
            }

            object GetById {
                fun success(channel: Channel = channel()) = ChannelRepository.Result.GetById.Success(channel)
                fun error(message: String = "Failed to remove") =
                    ChannelRepository.Result.GetById.Error(RuntimeException(message))

                fun notFound() = ChannelRepository.Result.GetById.NotFound
            }
        }

        object MessageResult {
            object Add {
                fun success() = MessageRepository.Result.Add.Success
                fun error(message: String = "Failed to add") =
                    MessageRepository.Result.Add.Error(RuntimeException(message))
            }

            object Remove {
                fun success() = MessageRepository.Result.Remove.Success
                fun notFound() = MessageRepository.Result.Remove.QueueNotFound
                fun error(message: String = "Failed to add") =
                    MessageRepository.Result.Remove.Error(RuntimeException(message))
            }
        }
    }
}

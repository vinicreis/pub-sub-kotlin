package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.domain.server.core.model.data.ResultOuterClass
import io.github.vinicreis.domain.server.core.model.data.SubscriptionStatusOuterClass.SubscriptionStatus
import io.github.vinicreis.domain.server.core.model.request.addRequest
import io.github.vinicreis.domain.server.core.model.request.listRequest
import io.github.vinicreis.domain.server.core.model.request.peekRequest
import io.github.vinicreis.domain.server.core.model.request.publishMultipleRequest
import io.github.vinicreis.domain.server.core.model.request.publishSingleRequest
import io.github.vinicreis.domain.server.core.model.request.removeByIdRequest
import io.github.vinicreis.domain.server.core.model.request.subscribeRequest
import io.github.vinicreis.domain.server.core.model.response.SubscribeResponse
import io.github.vinicreis.pubsub.server.core.grpc.extension.asRemoteMessage
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asDomain
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asRemote
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.core.test.extension.asMessage
import io.github.vinicreis.pubsub.server.core.test.extension.randomItem
import io.github.vinicreis.pubsub.server.core.test.extension.randomSlice
import io.github.vinicreis.pubsub.server.core.test.fixture.ChannelFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.MessageFixture
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
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
            val channel = ChannelFixture.instance()
            val request = addRequest { this.channel = channel.asRemote }

            coEvery { channelRepositoryMock.add(channel) } returns ChannelFixture.Repository.Add.success(channel)

            val response = sut.add(request)

            coVerify(exactly = 1) { channelRepositoryMock.add(channel) }
            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertEquals(channel, response.channel.asDomain)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return error response when channel fails to be added`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance()
            val request = addRequest { this.channel = channel.asRemote }

            coEvery { channelRepositoryMock.add(channel) } returns ChannelFixture.Repository.Add.error("Error message 1")

            val response = sut.add(request)

            coVerify(exactly = 1) { channelRepositoryMock.add(channel) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
            assertEquals("Error message 1", response.message)
        }

        @Test
        fun `Should return error with already found response when channel already exists`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance()
            val request = addRequest { this.channel = channel.asRemote }

            coEvery { channelRepositoryMock.add(channel) } returns ChannelFixture.Repository.Add.alreadyFound()

            val response = sut.add(request)

            coVerify(exactly = 1) { channelRepositoryMock.add(channel) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
            assertTrue(response.message.contains("already exists"))
            assertTrue(response.message.contains(channel.id.toString()))
        }

        @Test
        fun `Should return error with exception message when its thrown while adding`() = runTest(testDispatcher) {
            val channel = ChannelFixture.instance()
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
                    add(ChannelFixture.instance())
                }
            }

            coEvery { channelRepositoryMock.getAll() } returns ChannelFixture.Repository.List.success(channels)

            val response = sut.list(request)

            coVerify(exactly = 1) { channelRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertEquals(channels.map(Channel::asRemote), response.channelsList)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return success response with empty list if no channel is found`() = runTest(testDispatcher) {
            val request = listRequest { }

            coEvery { channelRepositoryMock.getAll() } returns ChannelFixture.Repository.List.success(emptyList())

            val response = sut.list(request)

            coVerify(exactly = 1) { channelRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertTrue(response.channelsList.isEmpty())
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return error with exception message when channels get fails`() = runTest(testDispatcher) {
            val request = listRequest { }

            coEvery { channelRepositoryMock.getAll() } returns ChannelFixture.Repository.List.error("Failed to get again")

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
                val id = ChannelFixture.id()
                val request = removeByIdRequest { this.id = id.toString() }
                val channel = ChannelFixture.instance(id = id)

                coEvery { channelRepositoryMock.removeById(id) } returns ChannelFixture.Repository.Remove.success(channel)
                coEvery { messageRepositoryMock.remove(channel) } returns MessageFixture.Repository.Remove.success()

                val response = sut.removeById(request)

                coVerify(exactly = 1) { channelRepositoryMock.removeById(id) }
                coVerify(exactly = 1) { messageRepositoryMock.remove(channel) }
                assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                assertEquals(channel.asRemote, response.channel)
                assertTrue(response.message.isEmpty())
            }

        @Test
        fun `Should return error response if the removed channel is not found`() = runTest(testDispatcher) {
            val id = ChannelFixture.id()
            val request = removeByIdRequest { this.id = id.toString() }
            val channel = ChannelFixture.instance(id)

            coEvery { channelRepositoryMock.removeById(id) } returns ChannelFixture.Repository.Remove.notFound()
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
                val id = ChannelFixture.id()
                val request = removeByIdRequest { this.id = id.toString() }
                val channel = ChannelFixture.instance()

                coEvery { channelRepositoryMock.removeById(id) } returns ChannelFixture.Repository.Remove.success(channel)
                coEvery { messageRepositoryMock.remove(channel) } returns MessageFixture.Repository.Remove.notFound()

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
                val id = ChannelFixture.id()
                val request = removeByIdRequest { this.id = id.toString() }

                coEvery { channelRepositoryMock.removeById(id) } returns
                        ChannelFixture.Repository.Remove.error("Failed by some reason")
                coEvery { messageRepositoryMock.remove(any()) } answers {
                    fail("Should not be called if channel is not found")
                }

                val response = sut.removeById(request)

                coVerify(exactly = 1) { channelRepositoryMock.removeById(id) }
                coVerify(exactly = 0) { messageRepositoryMock.remove(any()) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                assertEquals("Failed by some reason", response.message)
            }

        @Test
        fun `Should return error with exception message when its thrown while removing channel message queue`() =
            runTest(testDispatcher) {
                val id = ChannelFixture.id()
                val request = removeByIdRequest { this.id = id.toString() }
                val channel = ChannelFixture.instance()

                coEvery { channelRepositoryMock.removeById(id) } returns ChannelFixture.Repository.Remove.success(channel)
                coEvery { messageRepositoryMock.remove(channel) } throws RuntimeException("Any error happened!")

                val response = sut.removeById(request)

                coVerify(exactly = 1) { channelRepositoryMock.removeById(id) }
                coVerify(exactly = 1) { messageRepositoryMock.remove(channel) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                assertEquals(GENERIC_ERROR_MESSAGE, response.message)
            }

        @Test
        fun `Should return error with exception message when removing channel message queue fails`() =
            runTest(testDispatcher) {
                val id = ChannelFixture.id()
                val request = removeByIdRequest { this.id = id.toString() }
                val channel = ChannelFixture.instance()

                coEvery { channelRepositoryMock.removeById(id) } returns ChannelFixture.Repository.Remove.success(channel)
                coEvery { messageRepositoryMock.remove(channel) } returns MessageFixture.Repository.Remove.error("Didn't remove!")

                val response = sut.removeById(request)

                coVerify(exactly = 1) { channelRepositoryMock.removeById(id) }
                coVerify(exactly = 1) { messageRepositoryMock.remove(channel) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                assertEquals("Didn't remove!", response.message)
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
                    val id = ChannelFixture.id()
                    val channel = ChannelFixture.instance()
                    val message = MessageFixture.any()
                    val request = publishSingleRequest {
                        this.channelId = id.toString()
                        this.content = message.asRemoteMessage
                    }

                    coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
                    coEvery { messageRepositoryMock.add(channel, any()) } returns MessageFixture.Repository.Add.success()

                    val response = sut.publishSingle(request)

                    coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { messageRepositoryMock.add(channel, any()) }
                    assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                    assertEquals(channel.asRemote, response.channel)
                    assertTrue(response.message.isEmpty())
                }

            @Test
            fun `Should return error response if the channel is not found`() = runTest(testDispatcher) {
                val id = ChannelFixture.id()
                val channel = ChannelFixture.instance()
                val message = MessageFixture.any()
                val request = publishSingleRequest {
                    this.channelId = id.toString()
                    this.content = message.asRemoteMessage
                }

                coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.notFound()
                coEvery {
                    messageRepositoryMock.add(channel, any())
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
                    val id = ChannelFixture.id()
                    val channel = ChannelFixture.instance()
                    val message = MessageFixture.any()
                    val request = publishSingleRequest {
                        this.channelId = id.toString()
                        this.content = message.asRemoteMessage
                    }

                    coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
                    coEvery { messageRepositoryMock.add(channel, any()) } throws RuntimeException(MessageFixture.any())

                    val response = sut.publishSingle(request)

                    coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { messageRepositoryMock.add(channel, any()) }
                    assertEquals(ResultOuterClass.Result.ERROR, response.result)
                    assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                    assertEquals(GENERIC_ERROR_MESSAGE, response.message)
                }

            @Test
            fun `Should return error with exception message when publishing message fails`() =
                runTest(testDispatcher) {
                    val id = ChannelFixture.id()
                    val channel = ChannelFixture.instance()
                    val message = MessageFixture.any()
                    val errorMessage = MessageFixture.any()
                    val request = publishSingleRequest {
                        this.channelId = id.toString()
                        this.content = message.asRemoteMessage
                    }

                    coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
                    coEvery {
                        messageRepositoryMock.add(channel, any())
                    } returns MessageFixture.Repository.Add.error(errorMessage)

                    val response = sut.publishSingle(request)

                    coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { messageRepositoryMock.add(channel, any()) }
                    assertEquals(ResultOuterClass.Result.ERROR, response.result)
                    assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                    assertEquals(errorMessage, response.message)
                }
        }

        @Nested
        @DisplayName("Multiple message publish tests")
        inner class PublishMultipleTests {
            @Test
            fun `Should return success response when messages are published in valid channel`() =
                runTest(testDispatcher) {
                    val id = ChannelFixture.id()
                    val channel = ChannelFixture.instance()
                    val messages = MessageFixture.EXAMPLES.randomSlice()
                    val request = publishMultipleRequest {
                        this.channelId = id.toString()
                        this.content.addAll(messages.map { it.asRemoteMessage })
                    }

                    coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
                    coEvery {
                        messageRepositoryMock.addAll(channel, any())
                    } returns MessageFixture.Repository.Add.success()

                    val response = sut.publishMultiple(request)

                    coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { messageRepositoryMock.addAll(channel, any()) }
                    assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                    assertEquals(channel.asRemote, response.channel)
                    assertTrue(response.message.isEmpty())
                }

            @Test
            fun `Should return error response if the channel is not found`() = runTest(testDispatcher) {
                val id = ChannelFixture.id()
                val channel = ChannelFixture.instance()
                val messages = MessageFixture.EXAMPLES.randomSlice()
                val request = publishMultipleRequest {
                    this.channelId = id.toString()
                    this.content.addAll(messages.map { it.asRemoteMessage })
                }

                coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.notFound()
                coEvery {
                    messageRepositoryMock.addAll(channel, any())
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
                    val id = ChannelFixture.id()
                    val channel = ChannelFixture.instance()
                    val messages = MessageFixture.EXAMPLES.randomSlice()
                    val request = publishMultipleRequest {
                        this.channelId = id.toString()
                        this.content.addAll(messages.map { it.asRemoteMessage })
                    }

                    coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
                    coEvery {
                        messageRepositoryMock.addAll(channel, any())
                    } throws RuntimeException(MessageFixture.any())

                    val response = sut.publishMultiple(request)

                    coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { messageRepositoryMock.addAll(channel, any()) }
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
            val id = ChannelFixture.id()
            val channel = ChannelFixture.instance(id)
            val kotlinChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)
            val request = subscribeRequest { this.channelId = id.toString() }

            coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
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
            val id = ChannelFixture.id()
            val channel = ChannelFixture.instance(id)
            val request = subscribeRequest { this.channelId = id.toString() }
            val responses = mutableListOf<SubscribeResponse>()

            coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.notFound()
            every { subscriberManagerServiceMock.subscribe(channel) } answers {
                fail("Should not be called if channel is not found")
            }

            val subscriber = sut.subscribe(request)

            backgroundScope.launch { subscriber.toList(responses) }

            coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
            verify(exactly = 0) { subscriberManagerServiceMock.subscribe(any()) }

            assertEquals(SubscriptionStatus.PROCESSING, responses[0].status)
            assertEquals(RemoteChannel.getDefaultInstance(), responses[0].channel)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[0].content)
            assertTrue(responses[0].message.isEmpty())

            assertEquals(SubscriptionStatus.FINISHED, responses[1].status)
            assertEquals(RemoteChannel.getDefaultInstance(), responses[1].channel)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[1].content)
            assertTrue(responses[1].message.contains("not found"))
        }

        @Test
        fun `Should emit finished status with error when channel fails to be fetch`() = runTest(testDispatcher) {
            val id = ChannelFixture.id()
            val channel = ChannelFixture.instance(id)
            val request = subscribeRequest { this.channelId = id.toString() }
            val responses = mutableListOf<SubscribeResponse>()

            coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.error("No more error messages")
            every { subscriberManagerServiceMock.subscribe(channel) } answers {
                fail("Should not be called if channel is not found")
            }

            val subscriber = sut.subscribe(request)

            backgroundScope.launch { subscriber.toList(responses) }

            coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
            verify(exactly = 0) { subscriberManagerServiceMock.subscribe(any()) }

            assertEquals(SubscriptionStatus.PROCESSING, responses[0].status)
            assertEquals(RemoteChannel.getDefaultInstance(), responses[0].channel)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[0].content)
            assertTrue(responses[0].message.isEmpty())

            assertEquals(SubscriptionStatus.FINISHED, responses[1].status)
            assertEquals(RemoteChannel.getDefaultInstance(), responses[1].channel)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[1].content)
            assertEquals("No more error messages", responses[1].message)
        }

        @Test
        fun `Should emit active status when subscriber starts collecting results`() = runTest(testDispatcher) {
            val id = ChannelFixture.id()
            val channel = ChannelFixture.instance(id)
            val request = subscribeRequest { this.channelId = id.toString() }
            val responses = mutableListOf<SubscribeResponse>()
            val kotlinChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)

            coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
            every { subscriberManagerServiceMock.subscribe(channel) } returns kotlinChannel.receiveAsFlow()

            val subscriber = sut.subscribe(request)

            backgroundScope.launch { subscriber.toList(responses) }

            coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
            verify(exactly = 1) { subscriberManagerServiceMock.subscribe(channel) }

            assertEquals(SubscriptionStatus.PROCESSING, responses[0].status)
            assertEquals(RemoteChannel.getDefaultInstance(), responses[0].channel)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[0].content)
            assertTrue(responses[0].message.isEmpty())

            assertEquals(SubscriptionStatus.ACTIVE, responses[1].status)
            assertEquals(channel, responses[1].channel.asDomain)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[1].content)
            assertTrue(responses[1].message.isEmpty())
        }

        @Test
        fun `Should emit status update when then events happen`() = runTest(testDispatcher) {
            val id = ChannelFixture.id()
            val channel = ChannelFixture.instance(id)
            val request = subscribeRequest { this.channelId = id.toString() }
            val responses = mutableListOf<SubscribeResponse>()
            val kotlinChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)
            val emittedMessages = MessageFixture.EXAMPLES.randomSlice()

            coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
            every { subscriberManagerServiceMock.subscribe(channel) } returns kotlinChannel.receiveAsFlow()

            val subscriber = sut.subscribe(request)

            backgroundScope.launch { subscriber.toList(responses) }

            emittedMessages.forEach { kotlinChannel.send(it.asMessage) }

            coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
            verify(exactly = 1) { subscriberManagerServiceMock.subscribe(channel) }

            assertEquals(SubscriptionStatus.PROCESSING, responses[0].status)
            assertEquals(RemoteChannel.getDefaultInstance(), responses[0].channel)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[0].content)
            assertTrue(responses[0].message.isEmpty())

            assertEquals(SubscriptionStatus.ACTIVE, responses[1].status)
            assertEquals(channel, responses[1].channel.asDomain)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[1].content)
            assertTrue(responses[1].message.isEmpty())

            emittedMessages.forEachIndexed { index, message ->
                assertEquals(SubscriptionStatus.UPDATE, responses[index + 2].status)
                assertEquals(channel, responses[index + 2].channel.asDomain)
                assertEquals(message, responses[index + 2].content.content)
                assertTrue(responses[index + 2].message.isEmpty())
            }

            kotlinChannel.close()

            assertEquals(SubscriptionStatus.FINISHED, responses.last().status)
            assertEquals(channel, responses.last().channel.asDomain)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses.last().content)
            assertTrue(responses.last().message.isEmpty())

            assertEquals(emittedMessages.size + 3, responses.size)
        }
    }

    @Nested
    @DisplayName("Peek method tests")
    inner class PeekTests {
        @Test
        fun `Should return next emitted message successfully in a valid channel`() = runTest(testDispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val id = ChannelFixture.id()
            val channel = ChannelFixture.instance(id)
            val request = peekRequest { this.channelId = id.toString() }
            val message = MessageFixture.EXAMPLES.randomItem()
            val kotlinChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)

            coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
            every { subscriberManagerServiceMock.subscribe(channel) } returns kotlinChannel.receiveAsFlow()

            backgroundScope.launch(dispatcher) {
                delay(1.seconds)
                kotlinChannel.send(message.asMessage)
            }

            val response = backgroundScope.async(dispatcher) { sut.peek(request) }.await()

            coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
            verify(exactly = 1) { subscriberManagerServiceMock.subscribe(channel) }

            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertEquals(message, response.content.content)
            assertEquals(channel, response.channel.asDomain)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return error response when the channel is not found`() = runTest(testDispatcher) {
            val id = ChannelFixture.id()
            val request = peekRequest { this.channelId = id.toString() }

            coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.notFound()
            every { subscriberManagerServiceMock.subscribe(any()) } answers {
                fail("Should not be called if channel is not found")
            }

            val response = sut.peek(request)

            coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
            verify(exactly = 0) { subscriberManagerServiceMock.subscribe(any()) }

            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteTextMessage.getDefaultInstance(), response.content)
            assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
            assertTrue(response.message.contains("not found"))
        }

        @Test
        fun `Should return error response with exception message thrown while getting channel`() =
            runTest(testDispatcher) {
                val id = ChannelFixture.id()
                val request = peekRequest { this.channelId = id.toString() }

                coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.error("Tired of failing...")
                every { subscriberManagerServiceMock.subscribe(any()) } answers {
                    fail("Should not be called if channel is not found")
                }

                val response = sut.peek(request)

                coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                verify(exactly = 0) { subscriberManagerServiceMock.subscribe(any()) }

                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteTextMessage.getDefaultInstance(), response.content)
                assertEquals(RemoteChannel.getDefaultInstance(), response.channel)
                assertEquals("Tired of failing...", response.message)
            }

        @Test
        fun `Should fail by timeout in case no message is emitted in the sent seconds time`() =
            runTest(testDispatcher) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val id = ChannelFixture.id()
                val channel = ChannelFixture.instance(id)
                val timeout = Random.nextLong(60L)
                val request = peekRequest {
                    this.channelId = id.toString()
                    this.timeoutSeconds = timeout
                }
                val kotlinChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)

                coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
                every { subscriberManagerServiceMock.subscribe(channel) } returns kotlinChannel.receiveAsFlow()

                val response = backgroundScope.async(dispatcher) { sut.peek(request) }.await()

                coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                verify(exactly = 1) { subscriberManagerServiceMock.subscribe(channel) }

                assertEquals(timeout.seconds.inWholeMilliseconds, currentTime)
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteTextMessage.getDefaultInstance(), response.content)
                assertEquals(channel, response.channel.asDomain)
                assertTrue(response.message.contains("message timeout"))
            }

        @Test
        fun `Should return the first emitted message only in case more than one is emitted on message queue`() =
            runTest(testDispatcher) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val id = ChannelFixture.id()
                val channel = ChannelFixture.instance(id)
                val request = peekRequest { this.channelId = id.toString() }
                val messages = MessageFixture.EXAMPLES.randomSlice()
                val kotlinChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)
                val waitTime = Random.nextLong(1_000)

                coEvery { channelRepositoryMock.getById(id) } returns ChannelFixture.Repository.GetById.success(channel)
                every { subscriberManagerServiceMock.subscribe(channel) } returns kotlinChannel.receiveAsFlow()

                backgroundScope.launch(dispatcher) {
                    messages.forEach { message ->
                        delay(waitTime)
                        kotlinChannel.send(message.asMessage)
                    }
                }

                val response = backgroundScope.async(dispatcher) { sut.peek(request) }.await()

                coVerify(exactly = 1) { channelRepositoryMock.getById(id) }
                verify(exactly = 1) { subscriberManagerServiceMock.subscribe(channel) }

                assertEquals(waitTime, currentTime)
                assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                assertEquals(messages.first(), response.content.content)
                assertEquals(channel, response.channel.asDomain)
                assertTrue(response.message.isEmpty())
            }
    }

    companion object {
        private const val GENERIC_ERROR_MESSAGE = "Something went wrong..."
    }
}

package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.domain.server.core.model.data.ResultOuterClass
import io.github.vinicreis.domain.server.core.model.request.listRequest
import io.github.vinicreis.domain.server.core.model.request.pollRequest
import io.github.vinicreis.domain.server.core.model.request.postMultipleRequest
import io.github.vinicreis.domain.server.core.model.request.postSingleRequest
import io.github.vinicreis.domain.server.core.model.request.publishRequest
import io.github.vinicreis.domain.server.core.model.request.removeRequest
import io.github.vinicreis.domain.server.core.model.request.subscribeRequest
import io.github.vinicreis.domain.server.core.model.response.SubscribeResponseOuterClass.SubscribeResponse
import io.github.vinicreis.pubsub.server.core.grpc.extension.asRemoteMessage
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asDomain
import io.github.vinicreis.pubsub.server.core.grpc.mapper.asRemote
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.event.HeartbeatEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.core.test.extension.asTextMessage
import io.github.vinicreis.pubsub.server.core.test.extension.randomItem
import io.github.vinicreis.pubsub.server.core.test.extension.randomSlice
import io.github.vinicreis.pubsub.server.core.test.fixture.QueueFixture
import io.github.vinicreis.pubsub.server.core.test.fixture.TextMessageFixture
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
import io.github.vinicreis.domain.server.core.model.data.QueueOuterClass.Queue as RemoteQueue
import io.github.vinicreis.domain.server.core.model.data.SubscriptionEventOuterClass.SubscriptionEvent as RemoteSubscriptionEvent
import io.github.vinicreis.domain.server.core.model.data.TextMessageOuterClass.TextMessage as RemoteTextMessage

@OptIn(ExperimentalCoroutinesApi::class)
class QueueServiceGrpcTests {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val queueRepositoryMock = mockk<QueueRepository>()
    private val textMessageRepositoryMock = mockk<TextMessageRepository>()
    private val subscriberManagerServiceMock = mockk<SubscriberManagerService>()
    private val sut = QueueServiceGrpc(
        port = 8080,
        coroutineContext = testDispatcher,
        logger = Logger.getLogger("ChannelServiceGrpcTests"),
        queueRepository = queueRepositoryMock,
        textMessageRepository = textMessageRepositoryMock,
        subscriberManagerService = subscriberManagerServiceMock
    )

    @Nested
    @DisplayName("Add method tests")
    inner class AddTests {
        @Test
        fun `Should return success response when queue is added successfully`() = runTest(testDispatcher) {
            val queue = QueueFixture.instance()
            val request = publishRequest { this.queue = queue.asRemote }

            coEvery { queueRepositoryMock.add(queue) } returns QueueFixture.Repository.Add.success(queue)

            val response = sut.publish(request)

            coVerify(exactly = 1) { queueRepositoryMock.add(queue) }
            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertEquals(queue, response.queue.asDomain)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return error response when queue fails to be added`() = runTest(testDispatcher) {
            val queue = QueueFixture.instance()
            val request = publishRequest { this.queue = queue.asRemote }

            coEvery { queueRepositoryMock.add(queue) } returns QueueFixture.Repository.Add.error("Error message 1")

            val response = sut.publish(request)

            coVerify(exactly = 1) { queueRepositoryMock.add(queue) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
            assertEquals("Error message 1", response.message)
        }

        @Test
        fun `Should return error with already found response when queue already exists`() = runTest(testDispatcher) {
            val queue = QueueFixture.instance()
            val request = publishRequest { this.queue = queue.asRemote }

            coEvery { queueRepositoryMock.add(queue) } returns QueueFixture.Repository.Add.alreadyFound()

            val response = sut.publish(request)

            coVerify(exactly = 1) { queueRepositoryMock.add(queue) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
            assertTrue(response.message.contains("already exists"))
            assertTrue(response.message.contains(queue.id.toString()))
        }

        @Test
        fun `Should return error with exception message when its thrown while adding`() = runTest(testDispatcher) {
            val queue = QueueFixture.instance()
            val request = publishRequest { this.queue = queue.asRemote }

            coEvery { queueRepositoryMock.add(queue) } throws RuntimeException("Some error message")

            val response = sut.publish(request)

            coVerify(exactly = 1) { queueRepositoryMock.add(queue) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
            assertEquals(GENERIC_ERROR_MESSAGE, response.message)
        }
    }

    @Nested
    @DisplayName("List method tests")
    inner class ListTests {
        @Test
        fun `Should return success response when queues are successfully fetch`() = runTest(testDispatcher) {
            val request = listRequest { }
            val queues = buildList {
                repeat(Random.nextInt(20)) {
                    add(QueueFixture.instance())
                }
            }

            coEvery { queueRepositoryMock.getAll() } returns QueueFixture.Repository.List.success(queues)

            val response = sut.list(request)

            coVerify(exactly = 1) { queueRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertEquals(queues.map(Queue::asRemote), response.queuesList)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return success response with empty list if no queue is found`() = runTest(testDispatcher) {
            val request = listRequest { }

            coEvery { queueRepositoryMock.getAll() } returns QueueFixture.Repository.List.success(emptyList())

            val response = sut.list(request)

            coVerify(exactly = 1) { queueRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertTrue(response.queuesList.isEmpty())
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return error with exception message when queues get fails`() = runTest(testDispatcher) {
            val request = listRequest { }

            coEvery { queueRepositoryMock.getAll() } returns QueueFixture.Repository.List.error("Failed to get again")

            val response = sut.list(request)

            coVerify(exactly = 1) { queueRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertTrue(response.queuesList.isEmpty())
            assertEquals("Failed to get again", response.message)
        }

        @Test
        fun `Should return error with exception message when its thrown`() = runTest(testDispatcher) {
            val request = listRequest { }

            coEvery { queueRepositoryMock.getAll() } throws RuntimeException("Oops...")

            val response = sut.list(request)

            coVerify(exactly = 1) { queueRepositoryMock.getAll() }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertTrue(response.queuesList.isEmpty())
            assertEquals(GENERIC_ERROR_MESSAGE, response.message)
        }
    }

    @Nested
    @DisplayName("Remove method tests")
    inner class RemoveTests {
        @Test
        fun `Should return success response when valid queue is removed`() =
            runTest(testDispatcher) {
                val id = QueueFixture.id()
                val request = removeRequest { this.id = id.toString() }
                val queue = QueueFixture.instance(id = id)

                coEvery { queueRepositoryMock.removeById(id) } returns QueueFixture.Repository.Remove.success(queue)
                coEvery { textMessageRepositoryMock.removeAll(queue) } returns TextMessageFixture.Repository.Remove.success()

                val response = sut.remove(request)

                coVerify(exactly = 1) { queueRepositoryMock.removeById(id) }
                coVerify(exactly = 1) { textMessageRepositoryMock.removeAll(queue) }
                assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                assertEquals(queue.asRemote, response.queue)
                assertTrue(response.message.isEmpty())
            }

        @Test
        fun `Should return error response if the removed queue is not found`() = runTest(testDispatcher) {
            val id = QueueFixture.id()
            val request = removeRequest { this.id = id.toString() }
            val queue = QueueFixture.instance(id)

            coEvery { queueRepositoryMock.removeById(id) } returns QueueFixture.Repository.Remove.notFound()
            coEvery { textMessageRepositoryMock.removeAll(queue) } answers {
                fail("Should not call message repository remove if queue is not found")
            }

            val response = sut.remove(request)

            coVerify(exactly = 1) { queueRepositoryMock.removeById(id) }
            coVerify(exactly = 0) { textMessageRepositoryMock.removeAll(queue) }
            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
            assertTrue(response.message.contains("not found"))
        }

        @Test
        fun `Should return error with exception message when its thrown while removing queue`() =
            runTest(testDispatcher) {
                val id = QueueFixture.id()
                val request = removeRequest { this.id = id.toString() }

                coEvery { queueRepositoryMock.removeById(id) } returns
                        QueueFixture.Repository.Remove.error("Failed by some reason")
                coEvery { textMessageRepositoryMock.removeAll(any()) } answers {
                    fail("Should not be called if queue is not found")
                }

                val response = sut.remove(request)

                coVerify(exactly = 1) { queueRepositoryMock.removeById(id) }
                coVerify(exactly = 0) { textMessageRepositoryMock.removeAll(any()) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
                assertEquals("Failed by some reason", response.message)
            }

        @Test
        fun `Should return error with exception message when its thrown while removing queue message queue`() =
            runTest(testDispatcher) {
                val id = QueueFixture.id()
                val request = removeRequest { this.id = id.toString() }
                val queue = QueueFixture.instance()

                coEvery { queueRepositoryMock.removeById(id) } returns QueueFixture.Repository.Remove.success(queue)
                coEvery { textMessageRepositoryMock.removeAll(queue) } throws RuntimeException("Any error happened!")

                val response = sut.remove(request)

                coVerify(exactly = 1) { queueRepositoryMock.removeById(id) }
                coVerify(exactly = 1) { textMessageRepositoryMock.removeAll(queue) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
                assertEquals(GENERIC_ERROR_MESSAGE, response.message)
            }

        @Test
        fun `Should return error with exception message when removing queue message queue fails`() =
            runTest(testDispatcher) {
                val id = QueueFixture.id()
                val request = removeRequest { this.id = id.toString() }
                val queue = QueueFixture.instance()

                coEvery { queueRepositoryMock.removeById(id) } returns QueueFixture.Repository.Remove.success(queue)
                coEvery { textMessageRepositoryMock.removeAll(queue) } returns TextMessageFixture.Repository.Remove.error("Didn't remove!")

                val response = sut.remove(request)

                coVerify(exactly = 1) { queueRepositoryMock.removeById(id) }
                coVerify(exactly = 1) { textMessageRepositoryMock.removeAll(queue) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
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
            fun `Should return success response when a message is published in valid queue`() =
                runTest(testDispatcher) {
                    val id = QueueFixture.id()
                    val queue = QueueFixture.instance()
                    val message = TextMessageFixture.any()
                    val request = postSingleRequest {
                        this.queueId = id.toString()
                        this.content = message.asRemoteMessage
                    }

                    coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
                    coEvery { textMessageRepositoryMock.add(queue, any()) } returns TextMessageFixture.Repository.Add.success()

                    val response = sut.postSingle(request)

                    coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { textMessageRepositoryMock.add(queue, any()) }
                    assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                    assertEquals(queue.asRemote, response.queue)
                    assertTrue(response.message.isEmpty())
                }

            @Test
            fun `Should return error response if the queue is not found`() = runTest(testDispatcher) {
                val id = QueueFixture.id()
                val queue = QueueFixture.instance()
                val message = TextMessageFixture.any()
                val request = postSingleRequest {
                    this.queueId = id.toString()
                    this.content = message.asRemoteMessage
                }

                coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.notFound()
                coEvery {
                    textMessageRepositoryMock.add(queue, any())
                } answers { fail("Should not be called if queue is not found") }

                val response = sut.postSingle(request)

                coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                coVerify(exactly = 0) { textMessageRepositoryMock.add(any(), any()) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
                assertTrue(response.message.contains("not found"))
            }

            @Test
            fun `Should return error with exception message when its thrown while publishing message`() =
                runTest(testDispatcher) {
                    val id = QueueFixture.id()
                    val queue = QueueFixture.instance()
                    val message = TextMessageFixture.any()
                    val request = postSingleRequest {
                        this.queueId = id.toString()
                        this.content = message.asRemoteMessage
                    }

                    coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
                    coEvery { textMessageRepositoryMock.add(queue, any()) } throws RuntimeException(TextMessageFixture.any())

                    val response = sut.postSingle(request)

                    coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { textMessageRepositoryMock.add(queue, any()) }
                    assertEquals(ResultOuterClass.Result.ERROR, response.result)
                    assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
                    assertEquals(GENERIC_ERROR_MESSAGE, response.message)
                }

            @Test
            fun `Should return error with exception message when publishing message fails`() =
                runTest(testDispatcher) {
                    val id = QueueFixture.id()
                    val queue = QueueFixture.instance()
                    val message = TextMessageFixture.any()
                    val errorMessage = TextMessageFixture.any()
                    val request = postSingleRequest {
                        this.queueId = id.toString()
                        this.content = message.asRemoteMessage
                    }

                    coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
                    coEvery {
                        textMessageRepositoryMock.add(queue, any())
                    } returns TextMessageFixture.Repository.Add.error(errorMessage)

                    val response = sut.postSingle(request)

                    coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { textMessageRepositoryMock.add(queue, any()) }
                    assertEquals(ResultOuterClass.Result.ERROR, response.result)
                    assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
                    assertEquals(errorMessage, response.message)
                }
        }

        @Nested
        @DisplayName("Multiple message publish tests")
        inner class PublishMultipleTests {
            @Test
            fun `Should return success response when messages are published in valid queue`() =
                runTest(testDispatcher) {
                    val id = QueueFixture.id()
                    val queue = QueueFixture.instance()
                    val messages = TextMessageFixture.EXAMPLES.randomSlice()
                    val request = postMultipleRequest {
                        this.queueId = id.toString()
                        this.content.addAll(messages.map { it.asRemoteMessage })
                    }

                    coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
                    coEvery {
                        textMessageRepositoryMock.add(queue, any())
                    } returns TextMessageFixture.Repository.Add.success()

                    val response = sut.postMultiple(request)

                    coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { textMessageRepositoryMock.add(queue, any()) }
                    assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                    assertEquals(queue.asRemote, response.queue)
                    assertTrue(response.message.isEmpty())
                }

            @Test
            fun `Should return error response if the queue is not found`() = runTest(testDispatcher) {
                val id = QueueFixture.id()
                val queue = QueueFixture.instance()
                val messages = TextMessageFixture.EXAMPLES.randomSlice()
                val request = postMultipleRequest {
                    this.queueId = id.toString()
                    this.content.addAll(messages.map { it.asRemoteMessage })
                }

                coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.notFound()
                coEvery {
                    textMessageRepositoryMock.add(queue, any())
                } answers { fail("Should not be called if queue is not found") }

                val response = sut.postMultiple(request)

                coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                coVerify(exactly = 0) { textMessageRepositoryMock.add(any(), any()) }
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
                assertTrue(response.message.contains("not found"))
            }

            @Test
            fun `Should return error with exception message when its thrown while publishing messages`() =
                runTest(testDispatcher) {
                    val id = QueueFixture.id()
                    val queue = QueueFixture.instance()
                    val messages = TextMessageFixture.EXAMPLES.randomSlice()
                    val request = postMultipleRequest {
                        this.queueId = id.toString()
                        this.content.addAll(messages.map { it.asRemoteMessage })
                    }

                    coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
                    coEvery {
                        textMessageRepositoryMock.add(queue, any())
                    } throws RuntimeException(TextMessageFixture.any())

                    val response = sut.postMultiple(request)

                    coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                    coVerify(exactly = 1) { textMessageRepositoryMock.add(queue, any()) }
                    assertEquals(ResultOuterClass.Result.ERROR, response.result)
                    assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
                    assertEquals(GENERIC_ERROR_MESSAGE, response.message)
                }
        }
    }

    @Nested
    @DisplayName("Subscribe method tests")
    inner class SubscribeTests {
        @Test
        fun `Should emit processing when queue processing starts`() = runTest(testDispatcher) {
            val id = QueueFixture.id()
            val queue = QueueFixture.instance(id)
            val request = subscribeRequest { this.queueId = id.toString() }

            coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
            every { subscriberManagerServiceMock.subscribe(queue) } returns flowOf()

            val subscriber = sut.subscribe(request)
            val response = subscriber.first()

            coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
            verify(exactly = 1) { subscriberManagerServiceMock.subscribe(queue) }
            assertEquals(RemoteSubscriptionEvent.PROCESSING, response.event)
            assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
            assertEquals(RemoteTextMessage.getDefaultInstance(), response.content)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should emit finished status with error when queue is not found`() = runTest(testDispatcher) {
            val id = QueueFixture.id()
            val queue = QueueFixture.instance(id)
            val request = subscribeRequest { this.queueId = id.toString() }
            val responses = mutableListOf<SubscribeResponse>()

            coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.notFound()
            every { subscriberManagerServiceMock.subscribe(queue) } answers {
                fail("Should not be called if queue is not found")
            }

            val subscriber = sut.subscribe(request)

            backgroundScope.launch { subscriber.toList(responses) }

            coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
            verify(exactly = 0) { subscriberManagerServiceMock.subscribe(any()) }

            assertEquals(RemoteSubscriptionEvent.PROCESSING, responses[0].event)
            assertEquals(RemoteQueue.getDefaultInstance(), responses[0].queue)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[0].content)
            assertTrue(responses[0].message.isEmpty())

            assertEquals(RemoteSubscriptionEvent.FINISHED, responses[1].event)
            assertEquals(RemoteQueue.getDefaultInstance(), responses[1].queue)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[1].content)
            assertTrue(responses[1].message.contains("not found"))
        }

        @Test
        fun `Should emit finished status with error when queue fails to be fetch`() = runTest(testDispatcher) {
            val id = QueueFixture.id()
            val queue = QueueFixture.instance(id)
            val request = subscribeRequest { this.queueId = id.toString() }
            val responses = mutableListOf<SubscribeResponse>()

            coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.error("No more error messages")
            every { subscriberManagerServiceMock.subscribe(queue) } answers {
                fail("Should not be called if queue is not found")
            }

            val subscriber = sut.subscribe(request)

            backgroundScope.launch { subscriber.toList(responses) }

            coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
            verify(exactly = 0) { subscriberManagerServiceMock.subscribe(any()) }

            assertEquals(RemoteSubscriptionEvent.PROCESSING, responses[0].event)
            assertEquals(RemoteQueue.getDefaultInstance(), responses[0].queue)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[0].content)
            assertTrue(responses[0].message.isEmpty())

            assertEquals(RemoteSubscriptionEvent.FINISHED, responses[1].event)
            assertEquals(RemoteQueue.getDefaultInstance(), responses[1].queue)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[1].content)
            assertEquals("No more error messages", responses[1].message)
        }

        @Test
        fun `Should emit active status when subscriber starts collecting results`() = runTest(testDispatcher) {
            val id = QueueFixture.id()
            val queue = QueueFixture.instance(id)
            val request = subscribeRequest { this.queueId = id.toString() }
            val responses = mutableListOf<SubscribeResponse>()

            coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
            every { subscriberManagerServiceMock.subscribe(queue) } returns flowOf()

            val subscriber = sut.subscribe(request)

            backgroundScope.launch { subscriber.toList(responses) }

            coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
            verify(exactly = 1) { subscriberManagerServiceMock.subscribe(queue) }

            assertEquals(RemoteSubscriptionEvent.PROCESSING, responses[0].event)
            assertEquals(RemoteQueue.getDefaultInstance(), responses[0].queue)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[0].content)
            assertTrue(responses[0].message.isEmpty())

            assertEquals(RemoteSubscriptionEvent.ACTIVE, responses[1].event)
            assertEquals(queue, responses[1].queue.asDomain)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[1].content)
            assertTrue(responses[1].message.isEmpty())
        }

        @Test
        fun `Should emit status update when then events happen`() = runTest(testDispatcher) {
            val id = QueueFixture.id()
            val queue = QueueFixture.instance(id)
            val request = subscribeRequest { this.queueId = id.toString() }
            val responses = mutableListOf<SubscribeResponse>()
            val emittedMessages = TextMessageFixture.EXAMPLES.randomSlice().map {
                TextMessageReceivedEvent(
                    textMessage = it.asTextMessage(queue),
                )
            }

            coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
            every { subscriberManagerServiceMock.subscribe(queue) } returns flow {
                emittedMessages.forEach { emit(it) }
            }

            val subscriber = sut.subscribe(request)

            backgroundScope.launch { subscriber.toList(responses) }

            coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
            verify(exactly = 1) { subscriberManagerServiceMock.subscribe(queue) }

            assertEquals(RemoteSubscriptionEvent.PROCESSING, responses[0].event)
            assertEquals(RemoteQueue.getDefaultInstance(), responses[0].queue)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[0].content)
            assertTrue(responses[0].message.isEmpty())

            assertEquals(RemoteSubscriptionEvent.ACTIVE, responses[1].event)
            assertEquals(queue, responses[1].queue.asDomain)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses[1].content)
            assertTrue(responses[1].message.isEmpty())

            emittedMessages.forEachIndexed { index, message ->
                assertEquals(RemoteSubscriptionEvent.UPDATE, responses[index + 2].event)
                assertEquals(queue, responses[index + 2].queue.asDomain)
                assertEquals(message.textMessage.content, responses[index + 2].content.content)
                assertTrue(responses[index + 2].message.isEmpty())
            }

            assertEquals(RemoteSubscriptionEvent.FINISHED, responses.last().event)
            assertEquals(queue, responses.last().queue.asDomain)
            assertEquals(RemoteTextMessage.getDefaultInstance(), responses.last().content)
            assertTrue(responses.last().message.isEmpty())

            assertEquals(emittedMessages.size + 3, responses.size)
        }
    }

    @Nested
    @DisplayName("Peek method tests")
    inner class PeekTests {
        @Test
        fun `Should return next emitted message successfully in a valid queue`() = runTest(testDispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val id = QueueFixture.id()
            val queue = QueueFixture.instance(id)
            val request = pollRequest { this.queueId = id.toString() }
            val message = TextMessageFixture.EXAMPLES.randomItem().let {
                TextMessageReceivedEvent(
                    textMessage = it.asTextMessage(queue)
                )
            }

            coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
            every { subscriberManagerServiceMock.subscribe(queue) } returns flow {
                delay(1.seconds)
                emit(message)
            }

            val response = backgroundScope.async(dispatcher) { sut.poll(request) }.await()

            coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
            verify(exactly = 1) { subscriberManagerServiceMock.subscribe(queue) }

            assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
            assertEquals(message.textMessage.content, response.content.content)
            assertEquals(queue, response.queue.asDomain)
            assertTrue(response.message.isEmpty())
        }

        @Test
        fun `Should return error response when the queue is not found`() = runTest(testDispatcher) {
            val id = QueueFixture.id()
            val request = pollRequest { this.queueId = id.toString() }

            coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.notFound()
            every { subscriberManagerServiceMock.subscribe(any()) } answers {
                fail("Should not be called if queue is not found")
            }

            val response = sut.poll(request)

            coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
            verify(exactly = 0) { subscriberManagerServiceMock.subscribe(any()) }

            assertEquals(ResultOuterClass.Result.ERROR, response.result)
            assertEquals(RemoteTextMessage.getDefaultInstance(), response.content)
            assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
            assertTrue(response.message.contains("not found"))
        }

        @Test
        fun `Should return error response with exception message thrown while getting queue`() =
            runTest(testDispatcher) {
                val id = QueueFixture.id()
                val request = pollRequest { this.queueId = id.toString() }

                coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.error("Tired of failing...")
                every { subscriberManagerServiceMock.subscribe(any()) } answers {
                    fail("Should not be called if queue is not found")
                }

                val response = sut.poll(request)

                coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                verify(exactly = 0) { subscriberManagerServiceMock.subscribe(any()) }

                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteTextMessage.getDefaultInstance(), response.content)
                assertEquals(RemoteQueue.getDefaultInstance(), response.queue)
                assertEquals("Tired of failing...", response.message)
            }

        @Test
        fun `Should fail by timeout in case no message is emitted in the sent seconds time`() =
            runTest(testDispatcher) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val id = QueueFixture.id()
                val queue = QueueFixture.instance(id)
                val timeout = Random.nextLong(60L)
                val request = pollRequest {
                    this.queueId = id.toString()
                    this.timeoutSeconds = timeout
                }

                coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
                every { subscriberManagerServiceMock.subscribe(queue) } returns flow {
                    while (true) {
                        delay(1.seconds)

                        emit(HeartbeatEvent)
                    }
                }

                val response = backgroundScope.async(dispatcher) { sut.poll(request) }.await()

                coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                verify(exactly = 1) { subscriberManagerServiceMock.subscribe(queue) }

                assertEquals(timeout.seconds.inWholeMilliseconds, currentTime)
                assertEquals(ResultOuterClass.Result.ERROR, response.result)
                assertEquals(RemoteTextMessage.getDefaultInstance(), response.content)
                assertEquals(queue, response.queue.asDomain)
                assertTrue(response.message.contains("message timeout"))
            }

        @Test
        fun `Should return the first emitted message only in case more than one is emitted on message queue`() =
            runTest(testDispatcher) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val id = QueueFixture.id()
                val queue = QueueFixture.instance(id)
                val request = pollRequest { this.queueId = id.toString() }
                val waitTime = Random.nextLong(1_000)
                val messages = TextMessageFixture.EXAMPLES.randomSlice().map {
                    TextMessageReceivedEvent(
                        textMessage = it.asTextMessage(queue)
                    )
                }

                coEvery { queueRepositoryMock.getById(id) } returns QueueFixture.Repository.GetById.success(queue)
                every { subscriberManagerServiceMock.subscribe(queue) } returns flow {
                    messages.forEach { message ->
                        delay(waitTime)
                        emit(message)
                    }
                }

                val response = backgroundScope.async(dispatcher) { sut.poll(request) }.await()

                coVerify(exactly = 1) { queueRepositoryMock.getById(id) }
                verify(exactly = 1) { subscriberManagerServiceMock.subscribe(queue) }

                assertEquals(waitTime, currentTime)
                assertEquals(ResultOuterClass.Result.SUCCESS, response.result)
                assertEquals(messages.first().textMessage.content, response.content.content)
                assertEquals(queue, response.queue.asDomain)
                assertTrue(response.message.isEmpty())
            }
    }

    companion object {
        private const val GENERIC_ERROR_MESSAGE = "Something went wrong..."
    }
}

package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.logging.Logger
import kotlin.random.Random

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

        }

        @Test
        fun `Should return error response when channel fails to be added`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should return error with already found response when channel already exists`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should return error with exception message when its thrown while adding`() = runTest(testDispatcher) {

        }
    }

    @Nested
    @DisplayName("List method tests")
    inner class ListTests {
        @Test
        fun `Should return success response when channels are successfully fetch`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should return success response with empty list if no channel is found`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should return error with exception message when its thrown while getting all channels`() =
            runTest(testDispatcher) {

            }
    }

    @Nested
    @DisplayName("Remove method tests")
    inner class RemoveTests {
        @Test
        fun `Should return success response when valid channel is removed, closing its subscribers`() =
            runTest(testDispatcher) {

            }

        @Test
        fun `Should return error response if the removed channel is not found`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should return error response if the removed channel message queue is not found`() =
            runTest(testDispatcher) {

            }

        @Test
        fun `Should return error with exception message when its thrown while closing subscriber queue`() =
            runTest(testDispatcher) {

            }

        @Test
        fun `Should return error with exception message when its thrown while removing channel`() =
            runTest(testDispatcher) {

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

                }

            @Test
            fun `Should return error response if the channel is not found`() = runTest(testDispatcher) {

            }

            @Test
            fun `Should return error with exception message when its thrown while publishing message`() =
                runTest(testDispatcher) {

                }
        }

        @Nested
        @DisplayName("Multiple message publish tests")
        inner class PublishMultipleTests {
            @Test
            fun `Should return success response when messages are published in valid channel`() =
                runTest(testDispatcher) {

                }

            @Test
            fun `Should return error response if the channel is not found`() = runTest(testDispatcher) {

            }

            @Test
            fun `Should return error with exception message when its thrown while publishing messages`() =
                runTest(testDispatcher) {

                }
        }
    }

    @Nested
    @DisplayName("Subscribe method tests")
    inner class SubscribeTests {
        @Test
        fun `Should emit processing when channel processing starts`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should emit finished status with error when channel is not found`() = runTest(testDispatcher) {

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
        fun channel(
            id: String = "channel-1",
            name: String = "Channel 1",
            type: Channel.Type = Channel.Type.SIMPLE,
            pendingMessagesCount: Int = Random.nextInt(1000),
        ) = Channel(
            id = id,
            name = name,
            type = type,
            pendingMessagesCount = pendingMessagesCount,
        )

        object ChannelResult {
            object Add {
                fun success(channel: Channel = channel()) = ChannelRepository.Result.Add.Success(channel)
                fun alreadyFound() = ChannelRepository.Result.Add.AlreadyFound
                fun error(e: Exception = RuntimeException("Failed to add")) = ChannelRepository.Result.Add.Error(e)
            }

            object List {
                fun success(
                    channels: kotlin.collections.List<Channel> = listOf(
                        channel(),
                        channel(),
                    )
                ) = ChannelRepository.Result.GetAll.Success(channels)
                fun error(e: Exception = RuntimeException("Failed to list")) = ChannelRepository.Result.Add.Error(e)
            }

            object Remove {
                fun success(channel: Channel = channel()) = ChannelRepository.Result.Remove.Success(channel)
                fun error(e: Exception = RuntimeException("Failed to remove")) = ChannelRepository.Result.Remove.Error(e)
                fun notFound() = ChannelRepository.Result.Remove.NotFound
            }

            object GetById {
                fun success(channel: Channel = channel()) = ChannelRepository.Result.GetById.Success(channel)
                fun error(e: Exception = RuntimeException("Failed to remove")) = ChannelRepository.Result.GetById.Error(e)
                fun notFound() = ChannelRepository.Result.GetById.NotFound
            }
        }

        object MessageResult {
            object Add {
                fun success() = MessageRepository.Result.Add.Success
                fun error(e: Exception = RuntimeException("Failed to add")) = MessageRepository.Result.Add.Error(e)
            }
        }
    }
}

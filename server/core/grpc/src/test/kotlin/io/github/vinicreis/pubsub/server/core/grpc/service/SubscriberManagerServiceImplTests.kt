package io.github.vinicreis.pubsub.server.core.grpc.service

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.logging.Logger
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel as KotlinChannel

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriberManagerServiceImplTests {
    private val messageRepository = mockk<MessageRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val sut = SubscriberManagerImpl(
        messageRepository = messageRepository,
        coroutineContext = testDispatcher,
        logger = Logger.getLogger("SubscriberManagerImplTests")
    )

    @Test
    fun `Should close channel when the channel message queue is not found`() = runTest {
        every { messageRepository.subscribe(any()) } returns MessageRepository.Result.Subscribe.QueueNotFound

        val messages = mutableListOf<Message>()
        val channel = Fixture.channel()
        val subscriber = sut.subscribe(channel)

        launch { assertThrows<IllegalStateException> { subscriber.toList(messages) } }
        advanceUntilIdle()

        verify(exactly = 2) { messageRepository.subscribe(channel) }
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `Should close channel when some error happens to fetch message queue`() = runTest {
        every { messageRepository.subscribe(any()) } returns
                MessageRepository.Result.Subscribe.Error(IllegalArgumentException("Error"))

        val messages = mutableListOf<Message>()
        val channel = Fixture.channel()
        val subscriber = sut.subscribe(channel)

        launch { assertThrows<RuntimeException> { subscriber.toList(messages) } }
        advanceUntilIdle()

        verify(exactly = 2) { messageRepository.subscribe(channel) }
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `Should return a valid channel flow for subscriber when the channel is found`() = runTest {
        val messageChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)
        val messages = mutableListOf<Message>()
        val channel = Fixture.channel(type = Channel.Type.SIMPLE)
        val messagesEmitted = Random.nextInt(10)

        every { messageRepository.subscribe(any()) } returns
                MessageRepository.Result.Subscribe.Success(messageChannel)

        val subscriber = sut.subscribe(channel)

        backgroundScope.launch(testDispatcher) { subscriber.toList(messages) }
        launch {
            repeat(messagesEmitted) { i ->
                messageChannel.send("Message $i".asMessage)
            }
        }

        advanceUntilIdle()

        verify(exactly = 2) { messageRepository.subscribe(channel) }
        assertEquals(messagesEmitted, messages.size)
    }

    @Test
    fun `Should send message to only one subscriber when some message is posted on a simple channel`() = runTest {
        val messageChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)
        val channel = Fixture.channel(type = Channel.Type.SIMPLE)
        val messagesEmitted = Random.nextInt(10)
        val messages1 = mutableListOf<Message>()
        val messages2 = mutableListOf<Message>()
        val messages3 = mutableListOf<Message>()
        val messages4 = mutableListOf<Message>()

        every { messageRepository.subscribe(any()) } returns
                MessageRepository.Result.Subscribe.Success(messageChannel)

        val subscriber1 = sut.subscribe(channel)
        val subscriber2 = sut.subscribe(channel)
        val subscriber3 = sut.subscribe(channel)
        val subscriber4 = sut.subscribe(channel)

        backgroundScope.launch(testDispatcher) { subscriber1.toList(messages1) }
        backgroundScope.launch(testDispatcher) { subscriber2.toList(messages2) }
        backgroundScope.launch(testDispatcher) { subscriber3.toList(messages3) }
        backgroundScope.launch(testDispatcher) { subscriber4.toList(messages4) }

        launch {
            repeat(messagesEmitted) { i ->
                messageChannel.send("Message $i".asMessage)
            }
        }

        advanceUntilIdle()

        verify(exactly = 5) { messageRepository.subscribe(channel) }
        assertEquals(messagesEmitted, messages1.size + messages2.size + messages3.size + messages4.size)
    }

    @Test
    fun `Should send message to all subscribers when some message is posted on a multiple channel`() = runTest {
        val messageChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)
        val channel = Fixture.channel(type = Channel.Type.MULTIPLE)
        val messages1 = mutableListOf<Message>()
        val messages2 = mutableListOf<Message>()
        val messages3 = mutableListOf<Message>()
        val messages4 = mutableListOf<Message>()
        val messagesEmitted = buildList {
            repeat(Random.nextInt(10)) { i ->
                add("Message $i".asMessage)
            }
        }

        every { messageRepository.subscribe(any()) } returns
                MessageRepository.Result.Subscribe.Success(messageChannel)

        val subscriber1 = sut.subscribe(channel)
        val subscriber2 = sut.subscribe(channel)
        val subscriber3 = sut.subscribe(channel)
        val subscriber4 = sut.subscribe(channel)

        backgroundScope.launch(testDispatcher) { subscriber1.toList(messages1) }
        backgroundScope.launch(testDispatcher) { subscriber2.toList(messages2) }
        backgroundScope.launch(testDispatcher) { subscriber3.toList(messages3) }
        backgroundScope.launch(testDispatcher) { subscriber4.toList(messages4) }

        launch {
            messagesEmitted.forEach { message ->
                messageChannel.send(message)
            }
        }

        advanceUntilIdle()

        verify(exactly = 5) { messageRepository.subscribe(channel) }
        assertEquals(messagesEmitted, messages1)
        assertEquals(messagesEmitted, messages2)
        assertEquals(messagesEmitted, messages3)
        assertEquals(messagesEmitted, messages4)
    }

    @Test
    fun `Should close channel if the channel is removed from message queue`() = runTest {
        val messageChannel = KotlinChannel<Message>(KotlinChannel.UNLIMITED)
        val channel = Fixture.channel(type = Channel.Type.MULTIPLE)
        val messagesEmitted = Random.nextInt(10)
        val messages = mutableListOf<Message>()

        every { messageRepository.subscribe(any()) } returns
                MessageRepository.Result.Subscribe.Success(messageChannel)

        val subscriber = sut.subscribe(channel)

        val listJob = launch(testDispatcher) { subscriber.toList(messages) }

        launch {
            repeat(messagesEmitted) { i ->
                messageChannel.send("Message $i".asMessage)
            }

            messageChannel.close(CancellationException("Channel removed!"))
            println("Channel removed!")
        }

        advanceUntilIdle()

        verify(exactly = 2) { messageRepository.subscribe(channel) }
        assertEquals(messagesEmitted, messages.size)
        assertEquals(0, sut.subscribersCount(channel))
        println("Validating cancellation...")
        assertTrue(listJob.isCancelled)
    }

    companion object {
        private object Fixture {
            fun channel(
                id: String = "channel-1",
                name: String = "Channel 1",
                type: Channel.Type = Channel.Type.SIMPLE,
            ): Channel = Channel(
                id = id,
                name = name,
                type = type,
            )
        }

        private val String.asMessage: Message get() = Message(this)
    }
}
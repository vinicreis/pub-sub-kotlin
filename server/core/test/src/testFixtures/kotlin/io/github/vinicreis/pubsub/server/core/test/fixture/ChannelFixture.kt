package io.github.vinicreis.pubsub.server.core.test.fixture

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import java.util.*
import kotlin.random.Random

object ChannelFixture {
    fun id(): UUID = UUID.randomUUID()

    fun code(): String = "channel-" + Random.nextInt(100_000)

    fun instance(
        id: UUID = id(),
        code: String = code(),
        name: String = "Channel 1",
        type: Channel.Type = Channel.Type.SIMPLE,
        pendingMessagesCount: Long = 0L, // Random.nextInt(1000),
    ) = Channel(
        id = id,
        code = code,
        name = name,
        type = type,
        pendingMessagesCount = pendingMessagesCount,
    )

    object Repository {
        object Add {
            fun success(channel: Channel = instance()) = ChannelRepository.Result.Add.Success(channel)
            fun alreadyFound() = ChannelRepository.Result.Add.AlreadyFound
            fun error(message: String = "Failed to add") =
                ChannelRepository.Result.Add.Error(RuntimeException(message))
        }

        object List {
            fun success(
                channels: kotlin.collections.List<Channel> = listOf(instance(), instance())
            ) = ChannelRepository.Result.GetAll.Success(channels)

            fun error(message: String = "Failed to list") =
                ChannelRepository.Result.GetAll.Error(RuntimeException(message))
        }

        object Remove {
            fun success(channel: Channel = instance()) = ChannelRepository.Result.Remove.Success(channel)
            fun error(message: String = "Failed to remove") =
                ChannelRepository.Result.Remove.Error(RuntimeException(message))

            fun notFound() = ChannelRepository.Result.Remove.NotFound
        }

        object GetById {
            fun success(channel: Channel = instance()) = ChannelRepository.Result.GetById.Success(channel)
            fun error(message: String = "Failed to remove") =
                ChannelRepository.Result.GetById.Error(RuntimeException(message))

            fun notFound() = ChannelRepository.Result.GetById.NotFound
        }
    }
}

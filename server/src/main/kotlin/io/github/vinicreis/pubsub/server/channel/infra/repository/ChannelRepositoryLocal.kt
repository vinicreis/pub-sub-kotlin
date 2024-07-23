package io.github.vinicreis.pubsub.server.channel.infra.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.repository.ChannelRepository
import java.util.logging.Logger

class ChannelRepositoryLocal(
    private val logger: Logger = Logger.getLogger(ChannelRepositoryLocal::class.qualifiedName),
) : ChannelRepository {
    private val channels = mutableMapOf<String, Channel>()

    override suspend fun exists(channelId: String): Boolean = channels.containsKey(channelId)

    override suspend fun add(id: String, name: String, type: Channel.Type): ChannelRepository.Result.Add {
        if (exists(id)) return ChannelRepository.Result.Add.AlreadyFound

        return Channel(id, name, type).let { channel ->
            channels[id] = channel

            ChannelRepository.Result.Add.Success(channel)
        }
    }

    override suspend fun removeById(id: String): ChannelRepository.Result.Remove =
        channels.remove(id)?.let { removedChannel ->
            ChannelRepository.Result.Remove.Success(removedChannel)
        } ?: ChannelRepository.Result.Remove.NotFound

    override suspend fun remove(channel: Channel): ChannelRepository.Result.Remove = removeById(channel.id)

    override suspend fun getAll(): ChannelRepository.Result.GetAll =
        ChannelRepository.Result.GetAll.Success(channels.values.toList())

    override suspend fun getById(id: String): ChannelRepository.Result.GetById =
        channels[id]?.let { ChannelRepository.Result.GetById.Success(it) }
            ?: ChannelRepository.Result.GetById.NotFound
}

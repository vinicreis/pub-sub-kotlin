package io.github.vinicreis.pubsub.server.channel.infra.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.repository.ChannelRepository

class ChannelRepositoryLocal : ChannelRepository {
    private val channels = mutableMapOf<String, Channel>()

    override suspend fun add(channel: Channel): ChannelRepository.Result.Add =
        channels[channel.id]?.let { conflictingChannel ->
            ChannelRepository.Result.Add.AlreadyFound(conflictingChannel)
        } ?: run {
            channels[channel.id] = channel
            ChannelRepository.Result.Add.Success(channel)
        }

    override suspend fun removeById(id: String): ChannelRepository.Result.Remove =
        channels.remove(id)?.let { removedChannel ->
            ChannelRepository.Result.Remove.Success(removedChannel)
        } ?: ChannelRepository.Result.Remove.NotFound

    override suspend fun remove(channel: Channel): ChannelRepository.Result.Remove = removeById(channel.id)

    override suspend fun getAll(): ChannelRepository.Result.GetAll =
        ChannelRepository.Result.GetAll.Success(channels.values.toList())
}

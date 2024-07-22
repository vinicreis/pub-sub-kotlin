package io.github.vinicreis.pubsub.server.channel.infra.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Queue
import io.github.vinicreis.pubsub.server.channel.domain.repository.ChannelRepository

class ChannelRepositoryLocal : ChannelRepository {
    private val channels = mutableMapOf<String, Queue>()

    override suspend fun add(queue: Queue): ChannelRepository.Result.Add =
        channels[queue.id]?.let { conflictingChannel ->
            ChannelRepository.Result.Add.AlreadyFound(conflictingChannel)
        } ?: run {
            channels[queue.id] = queue
            ChannelRepository.Result.Add.Success(queue)
        }

    override suspend fun removeById(id: String): ChannelRepository.Result.Remove =
        channels.remove(id)?.let { removedChannel ->
            when(removedChannel) {
                is Queue.Simple -> removedChannel.close()
                is Queue.Multiple -> removedChannel.close()
            }

            ChannelRepository.Result.Remove.Success(removedChannel)
        } ?: ChannelRepository.Result.Remove.NotFound

    override suspend fun remove(queue: Queue): ChannelRepository.Result.Remove = removeById(queue.id)

    override suspend fun getAll(): ChannelRepository.Result.GetAll =
        ChannelRepository.Result.GetAll.Success(channels.values.toList())

    override suspend fun getById(id: String): ChannelRepository.Result.GetById =
        channels[id]?.let { ChannelRepository.Result.GetById.Success(it) }
            ?: ChannelRepository.Result.GetById.NotFound
}

package io.github.vinicreis.pubsub.server.channel.infra.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.channel.domain.repository.MessageFlowRepository
import java.util.logging.Logger

class ChannelRepositoryLocal(
    private val messageFlowRepository: MessageFlowRepository,
    private val logger: Logger = Logger.getLogger(ChannelRepositoryLocal::class.qualifiedName),
) : ChannelRepository {
    private val channels = mutableMapOf<String, Channel>()

    override suspend fun exists(channelId: String): Boolean = channels.containsKey(channelId)

    override suspend fun add(id: String, name: String, type: Channel.Type): ChannelRepository.Result.Add {
        if(exists(id)) return ChannelRepository.Result.Add.AlreadyFound

        return messageFlowRepository.getOrPut(id, type).let {
            when(it) {
                is MessageFlowRepository.Result.GetOrPut.Error -> ChannelRepository.Result.Add.Error(it.e)
                is MessageFlowRepository.Result.GetOrPut.Success -> Channel(
                    id = id,
                    name = name,
                    type = type,
                    messageFlow = it.messageFlow
                ).let { channel ->
                    channels[id] = channel

                    ChannelRepository.Result.Add.Success(channel)
                }
            }
        }
    }

    private suspend fun Channel.close() {
        messageFlowRepository.getOrPut(id, type).let { result ->
            when(result) {
                is MessageFlowRepository.Result.GetOrPut.Error ->
                    logger.warning("Failed to close on channel $id: ${result.e}")
                is MessageFlowRepository.Result.GetOrPut.Success -> result.messageFlow.close()
            }
        }
    }

    override suspend fun removeById(id: String): ChannelRepository.Result.Remove =
        channels.remove(id)?.let { removedChannel ->
            removedChannel.close()
            ChannelRepository.Result.Remove.Success(removedChannel)
        } ?: ChannelRepository.Result.Remove.NotFound

    override suspend fun remove(channel: Channel): ChannelRepository.Result.Remove = removeById(channel.id)

    override suspend fun getAll(): ChannelRepository.Result.GetAll =
        ChannelRepository.Result.GetAll.Success(channels.values.toList())

    override suspend fun getById(id: String): ChannelRepository.Result.GetById =
        channels[id]?.let { ChannelRepository.Result.GetById.Success(it) }
            ?: ChannelRepository.Result.GetById.NotFound
}

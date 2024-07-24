package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity.Channels
import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.mapper.asDomain
import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.mapper.from
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.logging.Logger

class ChannelRepositoryDatabase(
    private val logger: Logger = Logger.getLogger(ChannelRepositoryDatabase::class.java.simpleName)
) : ChannelRepository {
    init {
        transaction {
            check(Channels.exists()) { "Table ${Channels.tableName} does not exist!" }
        }
    }

    override suspend fun exists(channelId: String): Boolean = transaction {
        Channels.selectAll().count { it[Channels.code] == channelId } > 0
    }

    private fun Channel.validate() {
        require(id.isNotBlank()) { "Channel id cannot be blank" }
        require(name.isNotBlank()) { "Channel name cannot be blank" }
    }

    override suspend fun add(channel: Channel): ChannelRepository.Result.Add {
        return try {
            channel.validate()
            if (exists(channel.id)) return ChannelRepository.Result.Add.AlreadyFound

            transaction { Channels.insert { it from channel } }

            ChannelRepository.Result.Add.Success(channel)
        } catch (e: Exception) {
            logger.severe("Failed to add channel ${channel.id}")
            e.printStackTrace()

            ChannelRepository.Result.Add.Error(e)
        }
    }

    override suspend fun remove(channel: Channel): ChannelRepository.Result.Remove = try {
        transaction {
            Channels.selectAll().where { Channels.code eq channel.id }.map { it.asDomain }.firstOrNull()?.let {
                Channels.deleteWhere { code eq channel.id }

                ChannelRepository.Result.Remove.Success(it)
            } ?: ChannelRepository.Result.Remove.NotFound
        }
    } catch (e: Exception) {
        logger.severe("Failed to remove channel ${channel.id}")
        e.printStackTrace()

        ChannelRepository.Result.Remove.Error(e)
    }

    override suspend fun removeById(id: String): ChannelRepository.Result.Remove = try {
        transaction {
            Channels.selectAll().where { Channels.code eq id }.map { it.asDomain }.firstOrNull()?.let {
                Channels.deleteWhere { code eq id }

                ChannelRepository.Result.Remove.Success(it)
            } ?: ChannelRepository.Result.Remove.NotFound
        }
    } catch (e: Exception) {
        logger.severe("Failed to remove channel $id")
        e.printStackTrace()

        ChannelRepository.Result.Remove.Error(e)
    }

    override suspend fun getAll(): ChannelRepository.Result.GetAll = try {
        transaction {
            Channels.selectAll().map { it.asDomain }.let { channels ->
                ChannelRepository.Result.GetAll.Success(channels)
            }
        }
    } catch (e: Exception) {
        logger.severe("Failed to get all channels")
        e.printStackTrace()

        ChannelRepository.Result.GetAll.Error(e)
    }

    override suspend fun getById(id: String): ChannelRepository.Result.GetById = try {
        transaction {
            Channels.selectAll().firstOrNull { it[Channels.code] == id }?.asDomain?.let { channel ->
                ChannelRepository.Result.GetById.Success(channel)
            } ?: ChannelRepository.Result.GetById.NotFound
        }
    } catch (e: Exception) {
        logger.severe("Failed to get all channels")
        e.printStackTrace()

        ChannelRepository.Result.GetById.Error(e)
    }
}

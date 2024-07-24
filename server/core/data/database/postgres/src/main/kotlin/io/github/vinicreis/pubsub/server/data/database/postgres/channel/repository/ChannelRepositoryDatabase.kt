package io.github.vinicreis.pubsub.server.data.database.postgres.channel.repository

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.data.database.postgres.channel.entity.Channels
import io.github.vinicreis.pubsub.server.data.database.postgres.channel.mapper.asDomain
import io.github.vinicreis.pubsub.server.data.database.postgres.channel.mapper.from
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteReturning
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insertReturning
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

    override suspend fun add(channel: Channel): ChannelRepository.Result.Add {
        return try {
            if (exists(channel.id)) return ChannelRepository.Result.Add.AlreadyFound

            transaction {
                Channels.insertReturning(Channels.columns) { it from channel }
                    .firstOrNull()?.asDomain?.let { ChannelRepository.Result.Add.Success(it) }
                    ?: error("Failed to add channel ${channel.id}")
            }
        } catch (e: Exception) {
            logger.severe("Failed to add channel ${channel.id}")
            e.printStackTrace()

            ChannelRepository.Result.Add.Error(e)
        }
    }

    override suspend fun remove(channel: Channel): ChannelRepository.Result.Remove = try {
        transaction { Channels.deleteWhere { name eq channel.id } }

        ChannelRepository.Result.Remove.Success(channel)
    } catch (e: Exception) {
        logger.severe("Failed to remove channel ${channel.id}")
        e.printStackTrace()

        ChannelRepository.Result.Remove.Error(e)
    }

    override suspend fun removeById(id: String): ChannelRepository.Result.Remove = try {
        transaction {
            Channels.deleteReturning { Channels.name eq id }.firstOrNull()?.asDomain
        }?.let { ChannelRepository.Result.Remove.Success(it) }
            ?: ChannelRepository.Result.Remove.NotFound
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

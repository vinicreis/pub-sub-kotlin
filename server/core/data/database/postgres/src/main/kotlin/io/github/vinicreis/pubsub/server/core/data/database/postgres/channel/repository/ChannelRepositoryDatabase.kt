package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity.Channels
import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.mapper.asDomainChannel
import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.mapper.from
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.logging.Logger

class ChannelRepositoryDatabase(
    private val logger: Logger = Logger.getLogger(ChannelRepositoryDatabase::class.java.simpleName)
) : ChannelRepository {
    init {
        transaction {
            check(Channels.exists()) { "Table ${Channels.tableName} does not exist!" }
        }
    }

    override suspend fun exists(channel: Channel): Boolean = transaction {
        Channels.selectAll().any { it[Channels.id].value == channel.id || it[Channels.code] == channel.code }
    }

    private fun Channel.validate() {
        require(code.isNotBlank()) { "Channel id cannot be blank" }
        require(name.isNotBlank()) { "Channel name cannot be blank" }
    }

    override suspend fun add(channel: Channel): ChannelRepository.Result.Add {
        return try {
            channel.validate()
            if (exists(channel)) return ChannelRepository.Result.Add.AlreadyFound

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
            Channels.selectAll().where { Channels.id eq channel.id }.map { it.asDomainChannel }.firstOrNull()?.let {
                Channels.deleteWhere { id eq channel.id }

                ChannelRepository.Result.Remove.Success(it)
            } ?: ChannelRepository.Result.Remove.NotFound
        }
    } catch (e: Exception) {
        logger.severe("Failed to remove channel ${channel.id}")
        e.printStackTrace()

        ChannelRepository.Result.Remove.Error(e)
    }

    override suspend fun removeById(id: UUID): ChannelRepository.Result.Remove = try {
        transaction {
            Channels.selectAll().where { Channels.id eq id }.map { it.asDomainChannel }.firstOrNull()?.let {
                Channels.deleteWhere { this.id eq id }

                ChannelRepository.Result.Remove.Success(it)
            } ?: ChannelRepository.Result.Remove.NotFound
        }
    } catch (e: Exception) {
        logger.severe("Failed to remove channel $id")
        e.printStackTrace()

        ChannelRepository.Result.Remove.Error(e)
    }

    override suspend fun removeByCode(code: String): ChannelRepository.Result.Remove = try {
        transaction {
            Channels.selectAll().where { Channels.code eq code }.firstOrNull()?.asDomainChannel?.let { removedChannel ->
                Channels.deleteWhere { this.code eq code }.takeIf { it > 0 }?.let {
                    ChannelRepository.Result.Remove.Success(removedChannel)
                } ?: ChannelRepository.Result.Remove.NotFound
            } ?: ChannelRepository.Result.Remove.NotFound
        }
    } catch (e: Exception) {
        logger.severe("Failed to remove channel with code $code")
        e.printStackTrace()

        ChannelRepository.Result.Remove.Error(e)
    }

    override suspend fun getAll(): ChannelRepository.Result.GetAll = try {
        transaction {
            Channels.selectAll().map { it.asDomainChannel }.let { channels ->
                ChannelRepository.Result.GetAll.Success(channels)
            }
        }
    } catch (e: Exception) {
        logger.severe("Failed to get all channels")
        e.printStackTrace()

        ChannelRepository.Result.GetAll.Error(e)
    }

    override suspend fun getById(id: UUID): ChannelRepository.Result.GetById = try {
        transaction {
            Channels.selectAll().firstOrNull { it[Channels.id].value == id }?.asDomainChannel?.let { channel ->
                ChannelRepository.Result.GetById.Success(channel)
            } ?: ChannelRepository.Result.GetById.NotFound
        }
    } catch (e: Exception) {
        logger.severe("Failed to get all channels")
        e.printStackTrace()

        ChannelRepository.Result.GetById.Error(e)
    }
}

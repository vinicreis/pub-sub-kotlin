package io.github.vinicreis.pubsub.server.data.repository

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import java.util.*

interface ChannelRepository {
    sealed interface Result {
        sealed interface Error : Result {
            val e: Exception
        }

        sealed interface Add : Result {
            data class Success(val channel: Channel) : Add
            data object AlreadyFound : Add
            data class Error(override val e: Exception) : Add, Result.Error
        }

        sealed interface Remove : Result {
            data class Success(val channel: Channel) : Remove
            data object NotFound : Remove
            data class Error(override val e: Exception) : Remove, Result.Error
        }

        sealed interface GetAll : Result {
            data class Success(val channels: List<Channel>) : GetAll
            data class Error(override val e: Exception) : GetAll, Result.Error
        }

        sealed interface GetById : Result {
            data object NotFound : GetById
            data class Success(val channel: Channel) : GetById
            data class Error(override val e: Exception) : GetById, Result.Error
        }
    }

    suspend fun exists(channel: Channel): Boolean
    suspend fun add(channel: Channel): Result.Add
    suspend fun remove(channel: Channel): Result.Remove
    suspend fun removeByCode(code: String): Result.Remove
    suspend fun removeById(id: UUID): Result.Remove
    suspend fun getAll(): Result.GetAll
    suspend fun getById(id: UUID): Result.GetById
}
package io.github.vinicreis.pubsub.server.channel.domain.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel

interface ChannelRepository {
    sealed interface Result {
        sealed interface Error : Result {
            val message: String
        }

        sealed interface Add : Result {
            data class Success(val channel: Channel) : Add
            data class AlreadyFound(val channel: Channel) : Add
            data class Error(override val message: String) : Add, Result.Error
        }

        sealed interface Remove : Result {
            data class Success(val channel: Channel) : Remove
            data object NotFound : Remove
            data class Error(override val message: String) : Remove, Result.Error
        }

        sealed interface GetAll : Result {
            data class Success(val channels: List<Channel>) : GetAll
            data class Error(override val message: String) : GetAll, Result.Error
        }
    }

    suspend fun add(channel: Channel): Result.Add
    suspend fun remove(channel: Channel): Result.Remove
    suspend fun removeById(id: String): Result.Remove
    suspend fun getAll(): Result.GetAll
}
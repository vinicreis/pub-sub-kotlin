package io.github.vinicreis.pubsub.server.channel.domain.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Queue

interface ChannelRepository {
    sealed interface Result {
        sealed interface Error : Result {
            val message: String
        }

        sealed interface Add : Result {
            data class Success(val queue: Queue) : Add
            data class AlreadyFound(val queue: Queue) : Add
            data class Error(override val message: String) : Add, Result.Error
        }

        sealed interface Remove : Result {
            data class Success(val queue: Queue) : Remove
            data object NotFound : Remove
            data class Error(override val message: String) : Remove, Result.Error
        }

        sealed interface GetAll : Result {
            data class Success(val queues: List<Queue>) : GetAll
            data class Error(override val message: String) : GetAll, Result.Error
        }

        sealed interface GetById : Result {
            data object NotFound : GetById
            data class Success(val queue: Queue) : GetById
            data class Error(override val message: String) : GetById, Result.Error
        }
    }

    suspend fun add(queue: Queue): Result.Add
    suspend fun remove(queue: Queue): Result.Remove
    suspend fun removeById(id: String): Result.Remove
    suspend fun getAll(): Result.GetAll
    suspend fun getById(id: String): Result.GetById
}
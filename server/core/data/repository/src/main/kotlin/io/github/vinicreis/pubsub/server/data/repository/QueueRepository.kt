package io.github.vinicreis.pubsub.server.data.repository

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import java.util.*

interface QueueRepository {
    sealed interface Result {
        sealed interface Error : Result {
            val e: Exception
        }

        sealed interface Add : Result {
            data class Success(val queue: Queue) : Add
            data object AlreadyFound : Add
            data class Error(override val e: Exception) : Add, Result.Error
        }

        sealed interface Remove : Result {
            data class Success(val queue: Queue) : Remove
            data object NotFound : Remove
            data class Error(override val e: Exception) : Remove, Result.Error
        }

        sealed interface GetAll : Result {
            data class Success(val queues: List<Queue>) : GetAll
            data class Error(override val e: Exception) : GetAll, Result.Error
        }

        sealed interface GetById : Result {
            data object NotFound : GetById
            data class Success(val queue: Queue) : GetById
            data class Error(override val e: Exception) : GetById, Result.Error
        }
    }

    suspend fun exists(queue: Queue): Boolean
    suspend fun add(queue: Queue): Result.Add
    suspend fun removeById(id: UUID): Result.Remove
    suspend fun getAll(): Result.GetAll
    suspend fun getById(id: UUID): Result.GetById
}

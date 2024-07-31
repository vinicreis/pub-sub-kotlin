package io.github.vinicreis.pubsub.server.core.test.fixture

import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import java.util.*
import kotlin.random.Random

object QueueFixture {
    fun id(): UUID = UUID.randomUUID()

    fun code(): String = "queue-" + Random.nextInt(100_000)

    fun instance(
        id: UUID = id(),
        code: String = code(),
        name: String = "Queue 1",
        type: Queue.Type = Queue.Type.SIMPLE,
        pendingMessagesCount: Long = 0L, // Random.nextInt(1000),
    ) = Queue(
        id = id,
        code = code,
        name = name,
        type = type,
        pendingMessagesCount = pendingMessagesCount,
    )

    object Repository {
        object Add {
            fun success(queue: Queue = instance()) = QueueRepository.Result.Add.Success(queue)
            fun alreadyFound() = QueueRepository.Result.Add.AlreadyFound
            fun error(message: String = "Failed to add") =
                QueueRepository.Result.Add.Error(RuntimeException(message))
        }

        object List {
            fun success(
                queues: kotlin.collections.List<Queue> = listOf(instance(), instance())
            ) = QueueRepository.Result.GetAll.Success(queues)

            fun error(message: String = "Failed to list") =
                QueueRepository.Result.GetAll.Error(RuntimeException(message))
        }

        object Remove {
            fun success(queue: Queue = instance()) = QueueRepository.Result.Remove.Success(queue)
            fun error(message: String = "Failed to remove") =
                QueueRepository.Result.Remove.Error(RuntimeException(message))

            fun notFound() = QueueRepository.Result.Remove.NotFound
        }

        object GetById {
            fun success(queue: Queue = instance()) = QueueRepository.Result.GetById.Success(queue)
            fun error(message: String = "Failed to remove") =
                QueueRepository.Result.GetById.Error(RuntimeException(message))

            fun notFound() = QueueRepository.Result.GetById.NotFound
        }
    }
}

package io.github.vinicreis.pubsub.server.core.test.fixture

import io.github.vinicreis.pubsub.server.data.repository.MessageRepository

object MessageFixture {
    val EXAMPLES = listOf(
        "Any message", "One more message", "This is a test message", "Hello, World!",
        "Sample message", "Another message", "Random text", "Message content",
        "Test data", "Default message"
    )

    fun any(): String = EXAMPLES.random()

    object Repository {
        object Add {
            fun success() = MessageRepository.Result.Add.Success
            fun error(message: String = "Failed to add") =
                MessageRepository.Result.Add.Error(RuntimeException(message))
        }

        object Remove {
            fun success() = MessageRepository.Result.Remove.Success
            fun notFound() = MessageRepository.Result.Remove.QueueNotFound
            fun error(message: String = "Failed to add") =
                MessageRepository.Result.Remove.Error(RuntimeException(message))
        }
    }
}

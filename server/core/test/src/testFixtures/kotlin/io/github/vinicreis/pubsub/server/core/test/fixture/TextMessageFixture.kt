package io.github.vinicreis.pubsub.server.core.test.fixture

import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository

object TextMessageFixture {
    val EXAMPLES = listOf(
        "Any message", "One more message", "This is a test message", "Hello, World!",
        "Sample message", "Another message", "Random text", "Message content",
        "Test data", "Default message"
    )

    fun any(): String = EXAMPLES.random()

    object Repository {
        object Add {
            fun success() = TextMessageRepository.Result.Add.Success
            fun error(message: String = "Failed to add") =
                TextMessageRepository.Result.Add.Error(RuntimeException(message))
        }

        object Remove {
            fun success() = TextMessageRepository.Result.Remove.Success
            fun error(message: String = "Failed to add") =
                TextMessageRepository.Result.Remove.Error(RuntimeException(message))
        }
    }
}

package io.github.vinicreis.pubsub.server.core.data.database.postgres.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.extensions.withExposedTransaction
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.from
import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.TextMessages
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.data.repository.EventRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class TextMessageRepositoryDatabase(
    private val coroutineContext: CoroutineContext,
    private val eventRepository: EventRepository,
    private val logger: Logger = Logger.getLogger(TextMessageRepositoryDatabase::class.java.simpleName)
) : TextMessageRepository {
    private fun TextMessage.validate() {
        require(content.isNotBlank()) { "Message content can not be blank" }
        require(content.length <= TextMessage.MAX_CONTENT_LENGTH) {
            "Message content can not be longer than ${TextMessage.MAX_CONTENT_LENGTH} characters"
        }
    }

    private suspend fun <R : TextMessageRepository.Result> runCatchingErrors(
        block: suspend CoroutineScope.() -> R,
        errorBuilder: (Exception) -> R
    ): R = try {
        withContext(coroutineContext) { block() }
    } catch (e: IllegalArgumentException) {
        logger.fine(e.message)
        errorBuilder(e)
    } catch (e: Exception) {
        logger.fine(e.message)
        errorBuilder(RuntimeException(GENERIC_ERROR_MESSAGE, e))
    }

    override suspend fun add(queue: Queue, textMessages: List<TextMessage>): TextMessageRepository.Result.Add {
        return runCatchingErrors(
            block = {
                textMessages.forEach { it.validate() }

                withExposedTransaction {
                    textMessages.forEach { textMessage ->
                        TextMessages.insert { it from textMessage }
                        eventRepository.notify(TextMessageReceivedEvent(textMessage = textMessage))
                    }
                }.let { TextMessageRepository.Result.Add.Success }
            },
            errorBuilder = { e -> TextMessageRepository.Result.Add.Error(e) }
        )
    }

    override suspend fun removeAll(queue: Queue): TextMessageRepository.Result.Remove {
        return runCatchingErrors(
            block = {
                transaction { TextMessages.deleteWhere { queueId eq queue.id } }

                TextMessageRepository.Result.Remove.Success
            },
            errorBuilder = { e -> TextMessageRepository.Result.Remove.Error(e) }
        )
    }

    companion object {
        private const val GENERIC_ERROR_MESSAGE = "Something went wrong while processing database operation"
    }
}

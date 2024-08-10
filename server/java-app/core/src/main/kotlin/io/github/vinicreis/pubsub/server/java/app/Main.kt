package io.github.vinicreis.pubsub.server.java.app

import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.EventRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.QueueRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.TextMessageRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.script.initializePostgres
import io.github.vinicreis.pubsub.server.core.grpc.service.QueueServiceGrpc
import io.github.vinicreis.pubsub.server.core.grpc.service.SubscriberManagerServiceImpl
import io.github.vinicreis.pubsub.server.core.service.QueueService
import io.github.vinicreis.pubsub.server.core.service.SubscriberManagerService
import io.github.vinicreis.pubsub.server.data.repository.EventRepository
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) onInputError()

    val port: Int = args.firstOrNull()?.toIntOrNull() ?: onInputError()
    val eventsRepository: EventRepository = EventRepositoryDatabase(coroutineContext = Dispatchers.IO)
    val queueRepository: QueueRepository = QueueRepositoryDatabase(
        coroutineContext = Dispatchers.IO,
        eventRepository = eventsRepository
    )
    val textMessageRepository: TextMessageRepository = TextMessageRepositoryDatabase(
        coroutineContext = Dispatchers.IO,
        eventRepository = eventsRepository
    )
    val subscriberManagerService: SubscriberManagerService = SubscriberManagerServiceImpl(
        coroutineContext = Dispatchers.IO,
        eventRepository = eventsRepository,
    )
    val service: QueueService = QueueServiceGrpc(
        port = port,
        coroutineContext = Dispatchers.IO,
        queueRepository = queueRepository,
        textMessageRepository = textMessageRepository,
        subscriberManagerService = subscriberManagerService
    )

    Database.initializePostgres()
    service.start()
    service.blockUntilShutdown()
}

private object Message {
    val USAGE: String = """
        Usage: server <port>
          <port>  The port number to start the server
    """.trimIndent()
}

private fun onInputError(message: String? = null): Nothing {
    message?.also(::println)
    println(Message.USAGE)
    exitProcess(-1)
}

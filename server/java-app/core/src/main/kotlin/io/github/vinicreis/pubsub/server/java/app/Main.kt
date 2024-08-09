package io.github.vinicreis.pubsub.server.java.app

import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.EventRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.QueueRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.repository.TextMessageRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.script.initializePostgres
import io.github.vinicreis.pubsub.server.core.grpc.service.QueueServiceGrpc
import io.github.vinicreis.pubsub.server.core.grpc.service.SubscriberManagerServiceImpl
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database

fun main(args: Array<String>) {
    fun onError(message: String? = null) {
        message?.also(::println)
        println(Message.USAGE)
    }

    Database.initializePostgres()

    val port = args.firstOrNull()?.toIntOrNull() ?: run { onError(); return }
    val eventsRepository = EventRepositoryDatabase(Dispatchers.IO)
    val queueRepository = QueueRepositoryDatabase(Dispatchers.IO, eventsRepository)
    val textMessageRepository = TextMessageRepositoryDatabase(Dispatchers.IO, eventsRepository)
    val service = QueueServiceGrpc(
        port = port,
        coroutineContext = Dispatchers.IO,
        queueRepository = queueRepository,
        textMessageRepository = textMessageRepository,
        subscriberManagerService = SubscriberManagerServiceImpl(
            coroutineContext = Dispatchers.Default,
            eventRepository = eventsRepository,
        )
    )

    service.start()
    service.blockUntilShutdown()
}

private object Message {
    val USAGE: String = """
        Usage: server <port>
          <port>  The port to start the server
    """.trimIndent()
}

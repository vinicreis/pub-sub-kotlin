package io.github.vinicreis.pubsub.server

import io.github.vinicreis.pubsub.server.channel.infra.repository.MessageRepositoryLocal
import io.github.vinicreis.pubsub.server.channel.infra.service.ChannelServiceGRPC
import io.github.vinicreis.pubsub.server.channel.infra.service.SubscriberManagerImpl
import io.github.vinicreis.pubsub.server.data.postgres.channel.repository.ChannelRepositoryDatabase
import io.github.vinicreis.pubsub.server.data.postgres.migration.createMissingTablesAndObjects
import io.github.vinicreis.pubsub.server.data.postgres.script.initializePostgres
import io.github.vinicreis.pubsub.server.data.postgres.script.setLogger
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    fun onError(message: String? = null) {
        message?.also(::println)
        println(Message.USAGE)
    }

    Database.initializePostgres()

    transaction {
        setLogger()
        createMissingTablesAndObjects()

        commit()
    }

    val port = args.firstOrNull()?.toIntOrNull() ?: run { onError(); return }
    val messageRepository = MessageRepositoryLocal()
    val service = ChannelServiceGRPC(
        port = port,
        coroutineContext = Dispatchers.IO,
        channelRepository = ChannelRepositoryDatabase(),
        messageRepository = messageRepository,
        subscriberManager = SubscriberManagerImpl(messageRepository, Dispatchers.Default)
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

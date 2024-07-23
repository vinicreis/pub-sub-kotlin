package io.github.vinicreis.pubsub.server

import io.github.vinicreis.pubsub.server.channel.infra.repository.ChannelRepositoryLocal
import io.github.vinicreis.pubsub.server.channel.infra.repository.MessageRepositoryLocal
import io.github.vinicreis.pubsub.server.channel.infra.service.ChannelServiceGRPC
import io.github.vinicreis.pubsub.server.channel.infra.service.SubscriberManagerImpl
import io.github.vinicreis.pubsub.server.data.domain.model.Channels
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    fun onError(message: String? = null) {
        message?.also(::println)
        println(Message.USAGE)
    }

    val database = object {
        val NAME = System.getProperty("database.name")
        val USER = System.getProperty("database.user")
        val PASSWORD = System.getProperty("database.password")
        val PORT = System.getProperty("database.port")
    }

    Database.connect(
        url = "jdbc:postgresql://localhost:${database.PORT}/${database.NAME}",
        driver = "org.postgresql.Driver",
        user = database.USER,
        password = database.PASSWORD,
    )

    try {
        transaction {
            exec("CREATE TYPE ChannelType AS ENUM ('SIMPLE', 'MULTIPLE');")

            commit()
        }
    } catch (e: Exception) {
        println()
    }

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Channels)

        commit()
    }

    transaction {
        Channels.selectAll().toList()
    }.also { channels -> channels.forEach { channel -> println(channel) } }

    val port = args.firstOrNull()?.toIntOrNull() ?: run { onError(); return }
    val messageRepository = MessageRepositoryLocal()
    val service = ChannelServiceGRPC(
        port = port,
        coroutineContext = Dispatchers.IO,
        channelRepository = ChannelRepositoryLocal(),
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

package io.github.vinicreis.pubsub.server.java.app

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.repository.ChannelRepositoryDatabase
import io.github.vinicreis.pubsub.server.core.data.database.postgres.script.initializePostgres
import io.github.vinicreis.pubsub.server.core.grpc.service.ChannelServiceGrpc
import io.github.vinicreis.pubsub.server.core.grpc.service.SubscriberManagerImpl
import io.github.vinicreis.pubsub.server.data.database.local.MessageRepositoryLocal
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database

fun main(args: Array<String>) {
    fun onError(message: String? = null) {
        message?.also(::println)
        println(Message.USAGE)
    }

    Database.initializePostgres()

    val port = args.firstOrNull()?.toIntOrNull() ?: run { onError(); return }
    val channelRepository = ChannelRepositoryDatabase()
    val messageRepository = MessageRepositoryLocal(channelRepository, Dispatchers.Default)
    val service = ChannelServiceGrpc(
        port = port,
        coroutineContext = Dispatchers.IO,
        channelRepository = channelRepository,
        messageRepository = messageRepository,
        subscriberManagerService = SubscriberManagerImpl(messageRepository, Dispatchers.Default)
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

package io.github.vinicreis.pubsub.server

import io.github.vinicreis.pubsub.server.channel.infra.repository.ChannelRepositoryLocal
import io.github.vinicreis.pubsub.server.channel.infra.repository.MessageFlowRepositoryLocal
import io.github.vinicreis.pubsub.server.channel.infra.repository.MessageRepositoryLocal
import io.github.vinicreis.pubsub.server.channel.infra.service.ChannelServiceGRPC
import kotlinx.coroutines.Dispatchers

fun main(args: Array<String>) {
    fun onError(message: String? = null) {
        message?.also(::println)
        println(Message.USAGE)
    }

    val port = args.firstOrNull()?.toIntOrNull() ?: run { onError(); return }
    val messageFlowRepository = MessageFlowRepositoryLocal()
    val service = ChannelServiceGRPC(
        port = port,
        coroutineContext = Dispatchers.IO,
        channelRepository = ChannelRepositoryLocal(messageFlowRepository),
        messageRepository = MessageRepositoryLocal()
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

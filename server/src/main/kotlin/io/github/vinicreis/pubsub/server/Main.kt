package io.github.vinicreis.pubsub.server

import io.github.vinicreis.pubsub.server.channel.infra.repository.ChannelRepositoryLocal
import io.github.vinicreis.pubsub.server.channel.infra.service.ChannelServiceGRPC
import io.grpc.ServerBuilder
import kotlinx.coroutines.Dispatchers

fun main() {
    val service = ChannelServiceGRPC(
        coroutineContext = Dispatchers.IO,
        channelRepository = ChannelRepositoryLocal()
    )

    ServerBuilder
        .forPort(18000)
        .addService(service)
        .build()
        .run {
            start()
            awaitTermination()
        }
}

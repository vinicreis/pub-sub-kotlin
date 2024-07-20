package io.github.vinicreis.pubsub.client

import io.github.vinicreis.pubsub.client.subscriber.domain.model.Channel
import io.github.vinicreis.pubsub.client.subscriber.domain.model.ServerInfo
import io.github.vinicreis.pubsub.client.subscriber.domain.service.SubscriberServiceClient
import io.github.vinicreis.pubsub.client.subscriber.infra.SubscriberServiceGRPC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private fun SubscriberServiceClient.Response.print() {
    println(this)
}

fun main() {
    runBlocking {
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            publish(Channel("channel-1", "Channel 1", Channel.Type.SIMPLE)).print()
            publish(Channel("channel-2", "Channel 2", Channel.Type.MULTIPLE)).print()
            list()
        }
    }
}

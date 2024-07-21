package io.github.vinicreis.pubsub.client

import io.github.vinicreis.pubsub.client.subscriber.domain.model.Channel
import io.github.vinicreis.pubsub.client.subscriber.domain.model.Message
import io.github.vinicreis.pubsub.client.subscriber.domain.model.ServerInfo
import io.github.vinicreis.pubsub.client.subscriber.domain.service.SubscriberServiceClient
import io.github.vinicreis.pubsub.client.subscriber.infra.SubscriberServiceGRPC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

private fun SubscriberServiceClient.Response.print() {
    println(this)
}

private val String.asMessage get()  = Message(this)

fun main() {
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    runBlocking {
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            publish(Channel("channel-1", "Channel 1", Channel.Type.SIMPLE)).print()
            list()

            val postJob = coroutineScope.launch {
                delay(5.seconds)
                post("channel-1", "Message 1".asMessage).print()
                delay(3.seconds)
                post("channel-1", "Message 2".asMessage).print()
            }

            val collectJob = coroutineScope.launch {
                withTimeout(10.seconds) {
                    (subscribe("channel-1", Channel.Type.MULTIPLE) as SubscriberServiceClient.Response.Subscribed)
                        .messages.collect {
                            println("Received message: $it")
                        }
                }
            }

            postJob.join()
            collectJob.join()

            println("Finished!")
        }
    }
}

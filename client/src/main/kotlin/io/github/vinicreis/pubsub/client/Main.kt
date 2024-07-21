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
import kotlin.time.Duration.Companion.seconds

private fun SubscriberServiceClient.Response.print() {
    println(this)
}

private val String.asMessage get()  = Message(this)

fun main() {
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    val job1 = coroutineScope.launch {
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            publish(Channel("channel-1", "Channel 1", Channel.Type.SIMPLE)).print()
            list()

            val postJob = coroutineScope.launch {
                var i = 0

                while(true) {
                    delay(2.seconds)
                    post("channel-1", "Message ${++i}".asMessage).print()
                }
            }

            postJob.join()
        }
    }

    val job2 = coroutineScope.launch {
        delay(5.seconds)
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            peek("channel-1").also {
                println("Peeked message on 1: $it")
            }
        }
    }

    val job3 = coroutineScope.launch {
        delay(5.seconds)
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            peek("channel-1").also {
                println("Peeked message on 2: $it")
            }
        }
    }

    val job4 = coroutineScope.launch {
        delay(5.seconds)
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            peek("channel-1").also {
                println("Peeked message on 3: $it")
            }
        }
    }

    runBlocking {
        job1.join()
        job2.join()
        job3.join()
        job4.join()
    }
}

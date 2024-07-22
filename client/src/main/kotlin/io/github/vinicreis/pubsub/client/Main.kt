package io.github.vinicreis.pubsub.client

import io.github.vinicreis.pubsub.client.subscriber.domain.model.Channel
import io.github.vinicreis.pubsub.client.subscriber.domain.model.Message
import io.github.vinicreis.pubsub.client.subscriber.domain.model.ServerInfo
import io.github.vinicreis.pubsub.client.subscriber.domain.service.SubscriberServiceClient
import io.github.vinicreis.pubsub.client.subscriber.infra.SubscriberServiceGRPC
import kotlinx.coroutines.CoroutineExceptionHandler
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
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    val coroutineScope = CoroutineScope(Dispatchers.IO + exceptionHandler)

    val job1 = coroutineScope.launch {
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            publish(Channel("channel-1", "Channel 1", Channel.Type.MULTIPLE)).print()
            list()
            var i = 0

            while(true) {
                delay(1.seconds)
                post("channel-1", "Message ${++i}".asMessage).print()
                println("Posted message $i")
            }
        }
    }

    val job2 = coroutineScope.launch {
        delay(2.seconds)
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            (subscribe("channel-1") as SubscriberServiceClient.Response.Subscribed).also { flow ->
                flow.messages.collect {
                    println("Received message on 1: $it")
                }
            }
        }
    }

    val job3 = coroutineScope.launch {
        delay(2.seconds)
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            (subscribe("channel-1") as SubscriberServiceClient.Response.Subscribed).also { flow ->
                flow.messages.collect {
                    println("Received message on 2: $it")
                }
            }
        }
    }

//    val job2 = coroutineScope.launch {
//        delay(2.seconds)
//        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
//            peek("channel-1").also {
//                println("Peeked message on 1: $it")
//            }
//        }
//    }
//
//    val job3 = coroutineScope.launch {
//        delay(2.seconds)
//        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
//            peek("channel-1").also {
//                println("Peeked message on 2: $it")
//            }
//        }
//    }

    val job4 = coroutineScope.launch {
        delay(2.seconds)
        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            peek("channel-1").also {
                println("Peeked message on 3: $it")
            }
        }

        delay(5.seconds)

        SubscriberServiceGRPC(ServerInfo("localhost", 10090), Dispatchers.IO).run {
            peek("channel-1").also {
                println("Peeked message on 3 after some time: $it")
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

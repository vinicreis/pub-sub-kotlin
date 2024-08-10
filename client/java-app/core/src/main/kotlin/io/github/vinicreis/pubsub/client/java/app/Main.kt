package io.github.vinicreis.pubsub.client.java.app

import io.github.vinicreis.pubsub.client.core.grpc.service.QueueServiceClientGrpc
import io.github.vinicreis.pubsub.client.core.service.QueueServiceClient
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.stopUntilKeyPressed
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.config.getServerInfo
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.ClientMenuOptions
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.selectMenuOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.common.withQueueList
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.poll.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.getMessage
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.selectQueue
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish.getQueueData
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.remove.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.subscribe.collectAndPrint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicBoolean

fun main() {
    val notFinished = AtomicBoolean(true)
    val serverInfo = runBlocking { getServerInfo() }
    val service = QueueServiceClientGrpc(
        serverInfo = serverInfo,
        coroutineContext = Dispatchers.IO,
    )
    val ioScope = CoroutineScope(Dispatchers.IO)

    val mainJob = ioScope.launch {
        while (notFinished.get()) {
            try {
                when (selectMenuOption()) {
                    ClientMenuOptions.LIST_QUEUES -> service.list().print()
                    ClientMenuOptions.PUBLISH_QUEUE -> service.publish(getQueueData()).print()
                    ClientMenuOptions.POST_MESSAGE -> service.withQueueList { queues ->
                        queues.selectQueue { selectedQueue ->
                            service.post(selectedQueue.id.toString(), getMessage()).print()
                        }
                    }

                    ClientMenuOptions.POLL_QUEUE -> service.withQueueList { queues ->
                        queues.selectQueue { selectedQueue ->
                            service.poll(selectedQueue.id.toString()).print()
                        }
                    }

                    ClientMenuOptions.SUBSCRIBE_QUEUE -> service.withQueueList { queues ->
                        queues.selectQueue { selectedQueue ->
                            val subscriberJob = ioScope.launch {
                                service.subscribe(selectedQueue.id.toString()).collectAndPrint()
                            }

                            stopUntilKeyPressed(StringResource.Message.Input.PRESS_ENTER_TO_STOP_SUBSCRIPTION)
                            subscriberJob.cancel()
                        }
                    }

                    ClientMenuOptions.REMOVE_QUEUE -> service.withQueueList { queues ->
                        queues.selectQueue { selectedQueue ->
                            service.remove(selectedQueue.id.toString()).print()
                        }
                    }

                    ClientMenuOptions.EXIT -> notFinished.set(false)
                }
            } catch (e: ConnectException) {
                println("Failed to connect to server $serverInfo: ${e.message}")
            } catch (e: RuntimeException) {
                notFinished.set(false)
            } catch (t: Throwable) {
//                println("${StringResource.Error.GENERIC}: ${t.message}")
                t.printStackTrace()
                notFinished.set(false)
            }
        }
    }

    ioScope.launch {
        try {
            service.watch().collect { health ->
                when (health) {
                    QueueServiceClient.Response.Health.Healthy -> println("Server is healthy")
                    QueueServiceClient.Response.Health.NotHealthy -> mainJob.cancel("Server is not healthy")
                }
            }
        } catch (t: Throwable) {
            println("Failed to connect to server $serverInfo: ${t.message}")
            mainJob.cancel("Failed to connect to server", t)
        }
    }

    runBlocking { mainJob.join() }
}

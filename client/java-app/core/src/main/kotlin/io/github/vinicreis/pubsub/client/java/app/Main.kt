package io.github.vinicreis.pubsub.client.java.app

import io.github.vinicreis.pubsub.client.core.grpc.service.SubscriberServiceGRPC
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.config.getServerInfo
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.ClientMenuOptions
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.selectMenuOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.common.withQueueList
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.getMessage
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.selectQueue
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish.getQueueData
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.remove.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.subscribe.collectAndPrint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    var notFinished = true
    val serverInfo = runBlocking { getServerInfo() }
    val service = SubscriberServiceGRPC(
        serverInfo = serverInfo,
        coroutineContext = Dispatchers.IO,
    )
    val uiScope = CoroutineScope(Dispatchers.Default)
    val subscriberJobs = mutableListOf<Job>()

    runBlocking {
        while (notFinished) {
            try {
                when(selectMenuOption()) {
                    ClientMenuOptions.LIST_QUEUES -> service.list().print()
                    ClientMenuOptions.PUBLISH_QUEUE -> service.publish(getQueueData()).print()
                    ClientMenuOptions.POST_MESSAGE -> service.withQueueList { queues ->
                        queues.selectQueue { selectedQueue ->
                            service.post(selectedQueue.id.toString(), getMessage()).print()
                        }
                    }

                    ClientMenuOptions.SUBSCRIBE_QUEUE -> service.withQueueList { queues ->
                        queues.selectQueue { selectedQueue ->
                            uiScope.launch {
                                service.subscribe(selectedQueue.id.toString()).collectAndPrint()
                            }.also { job -> subscriberJobs.add(job) }
                        }
                    }

                    ClientMenuOptions.REMOVE_QUEUE -> service.withQueueList { queues ->
                        queues.selectQueue { selectedQueue ->
                            service.remove(selectedQueue.id.toString()).print()
                        }
                    }

                    ClientMenuOptions.EXIT -> {
                        subscriberJobs.forEach { it.cancel() }
                        notFinished = false
                    }
                }
            } catch (t: Throwable) {
                println("${StringResource.Error.GENERIC}: ${t.message}")
            }
        }
    }
}

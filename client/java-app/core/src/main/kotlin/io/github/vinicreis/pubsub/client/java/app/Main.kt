package io.github.vinicreis.pubsub.client.java.app

import io.github.vinicreis.pubsub.client.core.grpc.service.QueueServiceClientGrpc
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.stopUntilKeyPressed
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.config.getServerInfo
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.ClientMenuOptions
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.selectMenuOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.common.getTimeout
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.common.withQueueList
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.poll.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.getMessages
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.selectQueue
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish.getQueueData
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.remove.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.subscribe.collectAndPrint
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    var notFinished = true
    val serverInfo = getServerInfo()
    val service = QueueServiceClientGrpc(
        serverInfo = serverInfo,
        coroutineContext = Dispatchers.IO,
    )
    val uiScope = CoroutineScope(Dispatchers.Default)

    runBlocking {
        while (notFinished) {
            try {
                when(selectMenuOption()) {
                    ClientMenuOptions.LIST_QUEUES -> service.list().print()
                    ClientMenuOptions.PUBLISH_QUEUE -> service.publish(getQueueData()).print()
                    ClientMenuOptions.POST_MESSAGE -> service.withQueueList { queues ->
                        queues.selectQueue().also { selectedQueue ->
                            service.post(selectedQueue.id.toString(), *getMessages().toTypedArray()).print()
                        }
                    }

                    ClientMenuOptions.POLL_QUEUE -> service.withQueueList { queues ->
                        queues.selectQueue().also { selectedQueue ->
                            try {
                                service.poll(selectedQueue.id.toString(), getTimeout()).print()
                            } catch (e: StatusException) {
                                stopUntilKeyPressed("Polling cancelled by timeout. Press Enter to continue...")
                            }
                        }
                    }

                    ClientMenuOptions.SUBSCRIBE_QUEUE -> service.withQueueList { queues ->
                        queues.selectQueue().also { selectedQueue ->
                            val timeout = getTimeout() ?: Long.MAX_VALUE
                            val subscriberJob = uiScope.launch {
                                try {
                                    service.subscribe(selectedQueue.id.toString(), timeout).collectAndPrint()
                                } catch (e: StatusException) {
                                    stopUntilKeyPressed("Subscription cancelled by timeout. Press Enter to continue...")
                                }
                            }

                            stopUntilKeyPressed(StringResource.Message.Input.PRESS_ENTER_TO_STOP_SUBSCRIPTION)
                            subscriberJob.cancel()
                        }
                    }

                    ClientMenuOptions.REMOVE_QUEUE -> service.withQueueList { queues ->
                        queues.selectQueue().also { selectedQueue ->
                            service.remove(selectedQueue.id.toString()).print()
                        }
                    }

                    ClientMenuOptions.EXIT -> notFinished = false
                }
            } catch (t: Throwable) {
                println("${StringResource.Error.GENERIC}: ${t.message}")
            }
        }
    }
}

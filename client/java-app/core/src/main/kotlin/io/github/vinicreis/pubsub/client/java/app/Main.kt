package io.github.vinicreis.pubsub.client.java.app

import io.github.vinicreis.pubsub.client.core.grpc.service.SubscriberServiceGRPC
import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.config.getServerInfo
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.ClientMenuOptions
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.selectMenuOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.common.withChannelList
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.getMessage
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.print
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.post.selectChannel
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish.getChannelData
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
                    ClientMenuOptions.LIST_CHANNELS -> service.list().print()
                    ClientMenuOptions.PUBLISH_CHANNEL -> service.publish(getChannelData()).print()
                    ClientMenuOptions.POST_MESSAGE -> service.withChannelList { availableChannels ->
                        availableChannels.selectChannel { selectedChannel ->
                            service.post(selectedChannel.id.toString(), getMessage()).print()
                        }
                    }

                    ClientMenuOptions.SUBSCRIBE_CHANNEL -> service.withChannelList { availableChannels ->
                        availableChannels.selectChannel { selectedChannel ->
                            uiScope.launch {
                                service.subscribe(selectedChannel.id.toString()).collectAndPrint()
                            }.also { job -> subscriberJobs.add(job) }
                        }
                    }

                    ClientMenuOptions.REMOVE_CHANNEL -> service.withChannelList { availableChannels ->
                        availableChannels.selectChannel { selectedChannel ->
                            service.remove(selectedChannel.id.toString()).print()
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

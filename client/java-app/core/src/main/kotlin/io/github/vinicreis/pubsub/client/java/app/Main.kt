package io.github.vinicreis.pubsub.client.java.app

import io.github.vinicreis.pubsub.client.core.grpc.service.SubscriberServiceGRPC
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.config.getServerInfo
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.ClientMenuOptions
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu.selectMenuOption
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.list.show
import io.github.vinicreis.pubsub.client.java.app.ui.cli.step.operation.publish.getChannelData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() {
    var notFinished = true
    val serverInfo = getServerInfo()
    val service = SubscriberServiceGRPC(
        serverInfo = serverInfo,
        coroutineContext = Dispatchers.IO,
    )
    val uiScope = CoroutineScope(Dispatchers.Default)

    while (notFinished) {
        uiScope.launch {
            when(selectMenuOption()) {
                ClientMenuOptions.LIST_CHANNELS -> service.list().show()
                ClientMenuOptions.PUBLISH_CHANNEL -> service.publish(getChannelData())
                ClientMenuOptions.POST_MESSAGE -> TODO()
                ClientMenuOptions.SUBSCRIBE_CHANNEL -> TODO()
                ClientMenuOptions.EXIT -> notFinished = false
            }
        }
    }
}

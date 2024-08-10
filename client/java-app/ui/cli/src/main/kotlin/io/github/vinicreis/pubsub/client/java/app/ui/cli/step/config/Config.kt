package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.config

import io.github.vinicreis.pubsub.client.core.model.ServerInfo
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.getInput

fun getServerInfo(): ServerInfo {
    val host = getInput("Address", "localhost")
    val port = getInput("Port", "10090").toInt()

    return ServerInfo(host, port)
}

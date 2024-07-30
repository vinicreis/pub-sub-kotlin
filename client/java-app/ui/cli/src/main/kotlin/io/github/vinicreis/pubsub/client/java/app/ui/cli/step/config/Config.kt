package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.config

import io.github.vinicreis.pubsub.client.core.model.ServerInfo
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.getInput
import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.notNullable

suspend fun getServerInfo(): ServerInfo {
    val host = getInput("Address:", "localhost")
    val port = getInput("Port:", "10090").toInt().notNullable { "Server port cannot be null" }

    return ServerInfo(host, port)
}

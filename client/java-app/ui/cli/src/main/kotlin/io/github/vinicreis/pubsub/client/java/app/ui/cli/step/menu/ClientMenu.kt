package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu

import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.selectOption

enum class ClientMenuOptions(val message: String) {
    LIST_CHANNELS("List channels"),
    PUBLISH_CHANNEL("Publish a channel"),
    POST_MESSAGE("Post a message on channel"),
    SUBSCRIBE_CHANNEL("Subscribe to a channel"),
    REMOVE_CHANNEL("Remove a channel"),
    EXIT("Exit");

    companion object {
        fun fromOrdinal(ordinal: Int): ClientMenuOptions? = entries.getOrNull(ordinal)
    }
}

suspend fun selectMenuOption(): ClientMenuOptions {
    return selectOption(
        message = "Select an action to run on client",
        options = ClientMenuOptions.entries.map { it.message },
        defaultIndex = ClientMenuOptions.EXIT.ordinal,
    ).let { selectedIndex -> selectedIndex?.let { ClientMenuOptions.fromOrdinal(it) } ?: ClientMenuOptions.EXIT }
}

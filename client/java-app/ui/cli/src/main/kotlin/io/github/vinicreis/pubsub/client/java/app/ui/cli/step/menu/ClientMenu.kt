package io.github.vinicreis.pubsub.client.java.app.ui.cli.step.menu

import io.github.vinicreis.pubsub.client.java.app.ui.cli.components.selectOption

enum class ClientMenuOptions(val message: String) {
    LIST_QUEUES("List queues"),
    PUBLISH_QUEUE("Publish queue"),
    POST_MESSAGE("Post a message on queue"),
    SUBSCRIBE_QUEUE("Subscribe to queue"),
    REMOVE_QUEUE("Remove queue"),
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

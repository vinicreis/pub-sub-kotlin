package io.github.vinicreis.pubsub.client.java.app.ui.cli.components

import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

internal fun printDivider() {
    println(StringResource.ViewComponent.DIVIDER)
}

fun <E> List<E>.print(
    printElement: E.() -> Unit,
) {
    printDivider()
    when(size) {
        0 -> println("No elements found...")
        1 -> first().printElement()
        else -> forEachIndexed { i, element ->
            element.printElement()

            if(i < lastIndex) printDivider()
        }
    }
    printDivider()
}

internal fun clear() {
    print("\u001b[H\u001b[2J")
}

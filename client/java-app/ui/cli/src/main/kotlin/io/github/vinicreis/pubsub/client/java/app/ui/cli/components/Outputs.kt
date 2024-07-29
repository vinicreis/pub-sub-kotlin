package io.github.vinicreis.pubsub.client.java.app.ui.cli.components

import io.github.vinicreis.pubsub.client.java.app.ui.cli.resource.StringResource

fun printDivider() {
    println()
    println(StringResource.ViewComponent.DIVIDER)
    println()
}

fun <E> List<E>.print(
    printElement: E.() -> Unit,
) {
    when(size) {
        0 -> println("No elements found...")
        1 -> first().printElement()
        else -> forEachIndexed { i, element ->
            element.printElement()

            if(i < lastIndex) printDivider()
        }
    }
}

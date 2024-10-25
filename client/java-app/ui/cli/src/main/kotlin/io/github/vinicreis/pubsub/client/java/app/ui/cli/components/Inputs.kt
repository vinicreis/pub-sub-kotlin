package io.github.vinicreis.pubsub.client.java.app.ui.cli.components

internal fun getInputOrNull(): String? {
    return readlnOrNull()
}

internal fun getInputOrNull(message: String, default: String? = null): String? {
    print(message)
    default?.also { print(" [\"$it\"]") }
    print(": ")

    return getInputOrNull()?.ifBlank { default }
}

internal fun getInput(message: String, default: String): String {
    print(message)
    print(" [\"$default\"]")
    print(": ")

    return getInputOrNull()?.ifBlank { default } ?: default
}

internal fun selectOption(message: String, options: List<String>, defaultIndex: Int? = null): Int? {
    println(message)

    options.forEachIndexed { index, option ->
        print("\t${index + 1} - $option")
        if(defaultIndex == index) {
            print(" [DEFAULT]")
        }
        println()
    }

    return getInputOrNull("Selected option")?.toIntOrNull()?.dec() ?: defaultIndex
}

internal fun <T> T?.notNullable(lazyMessage: (() -> String)?): T {
    requireNotNull(
        value = this,
        lazyMessage = lazyMessage ?: { "Value cannot be null" }
    )

    return this
}

fun stopUntilKeyPressed(message: String, clear: Boolean = true) {
    if(clear) clear()
    println(message)
    readln()
}

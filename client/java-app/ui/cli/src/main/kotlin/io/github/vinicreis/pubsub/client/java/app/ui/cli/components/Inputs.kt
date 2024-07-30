package io.github.vinicreis.pubsub.client.java.app.ui.cli.components

internal suspend fun getInputOrNull(): String? {
    return readlnOrNull()?.ifBlank { null }
}

internal suspend inline fun getInputOrNull(message: String, default: String? = null): String? {
    print(message)
    default?.also { print(" [\"$it\"]") }
    print(": ")

    return getInputOrNull() ?: default
}

internal suspend inline fun getInput(message: String, default: String): String {
    print(message)
    print(" [\"$default\"]")
    print(": ")

    return getInputOrNull() ?: default
}

internal suspend fun selectOption(message: String, options: List<String>, defaultIndex: Int? = null): Int? {
    println(message)

    options.forEachIndexed { index, option ->
        print("${index + 1} - $option")
        if(defaultIndex == index) {
            print(" [DEFAULT]")
        }
        println()
    }

    return getInputOrNull()?.toIntOrNull()?.dec() ?: defaultIndex
}

internal fun <T> T?.notNullable(lazyMessage: (() -> String)?): T {
    requireNotNull(
        value = this,
        lazyMessage = lazyMessage ?: { "Value cannot be null" }
    )

    return this
}

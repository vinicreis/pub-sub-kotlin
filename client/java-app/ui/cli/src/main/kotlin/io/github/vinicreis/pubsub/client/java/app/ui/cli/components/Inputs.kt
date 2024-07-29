package io.github.vinicreis.pubsub.client.java.app.ui.cli.components

internal suspend inline fun <reified T> getInput(): T? {
    return readlnOrNull() as? T?
}

internal suspend inline fun <reified T> getInput(default: T): T {
    return readlnOrNull() as? T? ?: default
}

internal suspend inline fun <reified T> getInput(message: String, default: T): T {
    print(message)
    default?.also { option -> print(" [\"$option\"]") }
    print(": ")

    return getInput(default = default) ?: default
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

    return getInput<Int>() ?: defaultIndex
}

internal fun <T> T?.notNullable(lazyMessage: (() -> String)?): T {
    requireNotNull(
        value = this,
        lazyMessage = lazyMessage ?: { "Value cannot be null" }
    )

    return this
}

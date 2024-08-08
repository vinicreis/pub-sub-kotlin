package io.github.vinicreis.pubsub.client.core.model

data class ServerInfo(
    val address: String,
    val port: Int,
) {
    override fun toString(): String = "$address:$port"
}

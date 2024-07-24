package io.github.vinicreis.pubsub.server.data.postgres.model

internal interface Credentials {
    val URI: String
    val DRIVER: String
    val USER: String
    val PASSWORD: String
}

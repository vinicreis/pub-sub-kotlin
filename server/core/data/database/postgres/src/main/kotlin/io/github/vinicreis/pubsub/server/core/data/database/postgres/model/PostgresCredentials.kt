package io.github.vinicreis.pubsub.server.core.data.database.postgres.model

internal object PostgresCredentials : Credentials {
    private val PROTOCOL: String = System.getProperty("database.protocol") ?: error("Property \"database.protocol\" has to be defined")
    private val DOMAIN: String = System.getProperty("database.domain") ?: error("Property \"database.domain\" has to be defined")
    private val PORT: String = System.getProperty("database.port") ?: error("Property \"database.port\" has to be defined")
    private val NAME: String = System.getProperty("database.name") ?: error("Property \"database.name\" has to be defined")
    override val URI: String = "jdbc:$PROTOCOL://$DOMAIN:$PORT/$NAME"
    override val DRIVER: String = System.getProperty("database.driver") ?: error("Property \"database.driver\" has to be defined")
    override val USER: String = System.getProperty("database.user") ?: error("Property \"database.user\" has to be defined")
    override val PASSWORD: String = System.getProperty("database.password") ?: error("Property \"database.password\" has to be defined")
}

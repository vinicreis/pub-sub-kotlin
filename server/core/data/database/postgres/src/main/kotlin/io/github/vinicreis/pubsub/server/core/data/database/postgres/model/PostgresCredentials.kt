package io.github.vinicreis.pubsub.server.core.data.database.postgres.model

internal object PostgresCredentials : Credentials {
    private const val PROTOCOL: String = "postgresql"
    private const val DOMAIN: String = "db"
    private val PORT: String = System.getenv("DB_PORT") ?: error("Property \"database.port\" has to be defined")
    private val NAME: String = System.getenv("DB_NAME") ?: error("Property \"database.name\" has to be defined")
    override val DRIVER: String = "org.postgresql.Driver"
    override val URI: String = "jdbc:$PROTOCOL://$DOMAIN:$PORT/$NAME"
    override val USER: String = System.getenv("DB_USER") ?: error("Property \"database.user\" has to be defined")
    override val PASSWORD: String = System.getenv("DB_PASSWORD") ?: error("Property \"database.password\" has to be defined")
}

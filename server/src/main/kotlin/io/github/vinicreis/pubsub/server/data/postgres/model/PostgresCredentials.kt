package io.github.vinicreis.pubsub.server.data.postgres.model

internal object PostgresCredentials : Credentials {
    private val PROTOCOL: String = System.getProperty("database.protocol", "")
    private val DOMAIN: String = System.getProperty("database.domain", "")
    private val PORT: String = System.getProperty("database.port", "")
    private val NAME: String = System.getProperty("database.name", "")
    override val URI: String = "jdbc:$PROTOCOL://$DOMAIN:$PORT/$NAME"
    override val DRIVER: String = System.getProperty("database.driver", "")
    override val USER: String = System.getProperty("database.user", "")
    override val PASSWORD: String = System.getProperty("database.password", "")
}

package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.fixture

import io.github.vinicreis.pubsub.server.core.data.database.postgres.migration.createMissingTablesAndObjects
import io.github.vinicreis.pubsub.server.core.data.database.postgres.model.Credentials
import io.github.vinicreis.pubsub.server.core.data.database.postgres.script.setLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

internal object DatabaseFixture {
    private object H2Credentials : Credentials {
        const val DATABASE_NAME: String = "test"
        override val URI: String = "jdbc:h2:mem:$DATABASE_NAME;DB_CLOSE_DELAY=-1"
        override val DRIVER: String = "org.h2.Driver"
        override val USER: String = "test"
        override val PASSWORD: String = "test"
    }

    private fun connect() {
        Database.connect(
            url = H2Credentials.URI,
            driver = H2Credentials.DRIVER,
            user = H2Credentials.USER,
            password = H2Credentials.PASSWORD
        )

        transaction { setLogger() }
    }

    fun up() {
        connect()

        transaction { createMissingTablesAndObjects() }
    }

    fun down() {
        transaction { SchemaUtils.dropDatabase(H2Credentials.DATABASE_NAME) }
    }
}
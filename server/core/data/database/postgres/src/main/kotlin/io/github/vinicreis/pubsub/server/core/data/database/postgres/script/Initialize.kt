package io.github.vinicreis.pubsub.server.core.data.database.postgres.script

import io.github.vinicreis.pubsub.server.core.data.database.postgres.migration.createMissingTablesAndObjects
import io.github.vinicreis.pubsub.server.core.data.database.postgres.model.PostgresCredentials
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

fun Database.Companion.initializePostgres() {
    connect(
        url = PostgresCredentials.URI,
        driver = PostgresCredentials.DRIVER,
        user = PostgresCredentials.USER,
        password = PostgresCredentials.PASSWORD,
    )

    transaction {
        setLogger()
        createMissingTablesAndObjects()
    }
}

internal fun Transaction.setLogger() {
    addLogger(StdOutSqlLogger)
}

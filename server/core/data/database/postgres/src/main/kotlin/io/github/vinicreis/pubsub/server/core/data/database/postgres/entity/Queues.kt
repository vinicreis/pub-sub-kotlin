package io.github.vinicreis.pubsub.server.core.data.database.postgres.entity

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ExpressionAlias
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.count
import java.util.*

internal object Queues : IdTable<UUID>() {
    override val tableName: String = "queues"

    override val id: Column<EntityID<UUID>> = uuid("id").entityId()
    val code: Column<String> = varchar("code", 255).uniqueIndex()
    val name: Column<String> = varchar("name", 255)
    val type: Column<Type> = enumerationByName("type", 16, Type::class)
    val pendingMessagesCount: ExpressionAlias<Long>
        get() = TextMessages.id.count().alias("pending_messages")

    override val primaryKey: PrimaryKey = PrimaryKey(id, name = "PK_Queue_ID")

    enum class Type {
        SIMPLE,
        MULTIPLE,
    }
}

package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import java.util.*

internal object Channels : IdTable<UUID>() {
    override val tableName: String = "channels"

    override val id: Column<EntityID<UUID>> = uuid("id").entityId()
    val code: Column<String> = varchar("code", 255).uniqueIndex()
    val name: Column<String> = varchar("name", 255)
    val type: Column<Type> = enumerationByName("type", 16, Type::class)

    override val primaryKey: PrimaryKey = PrimaryKey(id, name = "PK_Channel_ID")

    enum class Type {
        SIMPLE,
        MULTIPLE,
    }
}

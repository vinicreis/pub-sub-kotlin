package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import java.util.*

object Messages : IdTable<UUID>() {
    override val id: Column<EntityID<UUID>> = uuid("id").entityId()
    val channel: Column<EntityID<UUID>> = reference("channel", Channels)
    val content: Column<String> = varchar("content", Int.MAX_VALUE)
    val createdAt: Column<Long> = long("created_at")

    override val tableName: String = "messages"
    override val primaryKey: PrimaryKey = PrimaryKey(id, name = "PK_Message_ID")
}

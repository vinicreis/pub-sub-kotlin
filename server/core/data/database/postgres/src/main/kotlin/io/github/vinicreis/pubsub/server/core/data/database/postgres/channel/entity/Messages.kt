package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity

import io.github.vinicreis.pubsub.server.core.model.data.Message
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import java.util.*

internal object Messages : IdTable<UUID>() {
    override val tableName: String = "messages"

    override val id: Column<EntityID<UUID>> = uuid("id").entityId()
    val channel: Column<EntityID<UUID>> = reference("channel_id", Channels.id)
    val content: Column<String> = varchar("content", Message.MAX_CONTENT_LENGTH)
    val createdAt: Column<Long> = long("created_at")

    override val primaryKey: PrimaryKey = PrimaryKey(id, name = "PK_Message_ID")
}

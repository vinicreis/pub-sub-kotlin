package io.github.vinicreis.pubsub.server.core.data.database.postgres.entity

import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.util.*

internal object TextMessages : IdTable<UUID>() {
    override val tableName: String = "text_messages"

    override val id: Column<EntityID<UUID>> = uuid("id").entityId()
    val queueId: Column<UUID> = uuid("queue_id").references(Queues.id, onDelete = ReferenceOption.CASCADE)
    val content: Column<String> = varchar("content", TextMessage.MAX_CONTENT_LENGTH)
    val createdAt: Column<LocalDateTime> = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    override val primaryKey: PrimaryKey = PrimaryKey(id, name = "PK_Message_ID")
}

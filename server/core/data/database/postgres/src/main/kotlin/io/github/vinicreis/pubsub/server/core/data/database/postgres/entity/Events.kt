package io.github.vinicreis.pubsub.server.core.data.database.postgres.entity

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

object Events : IdTable<UUID>() {
    override val tableName: String = "events"

    override val id: Column<EntityID<UUID>> = uuid("id").entityId()
    val type: Column<Type> = enumerationByName("type", 50, Type::class)
    val queueId: Column<UUID> = uuid("queue_id")
    val textMessageId: Column<EntityID<UUID>?> = optReference("text_message_id", TextMessages.id, onDelete = ReferenceOption.CASCADE)
    val createdAt: Column<LocalDateTime> = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    enum class Type {
        TEXT_MESSAGE_RECEIVED,
        QUEUE_ADDED,
        QUEUE_REMOVED,
    }
}

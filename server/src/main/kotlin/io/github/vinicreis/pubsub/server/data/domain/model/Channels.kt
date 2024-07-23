package io.github.vinicreis.pubsub.server.data.domain.model

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.postgresql.util.PGobject
import java.util.*

class PGEnum<T : Enum<T>>(name: String, value: T?) : PGobject() {
    init {
        this.value = value?.name
        this.type = name
    }
}

enum class ChannelType {
    SIMPLE,
    MULTIPLE,
}

object Channels : IdTable<UUID>() {
    private const val CHANNEL_TYPE_COLUMN_NAME = "ChannelType"

    override val id: Column<EntityID<UUID>> = uuid("id").entityId()
    val name: Column<String> = varchar("name", 255)
    val type: Column<ChannelType> = customEnumeration(
        name = "type",
        sql = CHANNEL_TYPE_COLUMN_NAME,
        fromDb = { value -> ChannelType.valueOf(value as String) },
        toDb = { value -> PGEnum(CHANNEL_TYPE_COLUMN_NAME, value) }
    )

    override val primaryKey: PrimaryKey = PrimaryKey(id, name = "PK_Channel_ID")
    override val tableName: String = "channels"
}

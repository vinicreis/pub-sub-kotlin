package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.mapper

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity.Channels
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder

internal val Channel.Type.toEntity: Channels.Type
    get() = when (this) {
        Channel.Type.SIMPLE -> Channels.Type.SIMPLE
        Channel.Type.MULTIPLE -> Channels.Type.MULTIPLE
    }

context(Channels)
infix fun UpdateBuilder<Number>.from(channel: Channel) {
    this[id] = channel.id
    this[code] = channel.code
    this[name] = channel.name
    this[Channels.type] = channel.type.toEntity
}

internal val Channels.Type.asDomain: Channel.Type
    get() = when (this) {
        Channels.Type.SIMPLE -> Channel.Type.SIMPLE
        Channels.Type.MULTIPLE -> Channel.Type.MULTIPLE
    }

val ResultRow.asDomain: Channel get() = Channel(
    id = this[Channels.id].value,
    code = this[Channels.code],
    name = this[Channels.name],
    type = this[Channels.type].asDomain,
)

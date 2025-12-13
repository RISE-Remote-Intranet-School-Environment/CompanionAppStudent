package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les séries
object SeriesNameTable : IntIdTable("series_name") {
    val seriesId = varchar("series_id", 50)
    val yearOptionIds = text("year_option_ids").nullable()
    val formationIds = text("formation_ids").nullable()
}

// DTO pour exposer les séries au front
@Serializable
data class SeriesNameDTO(
    val id: Int,
    val seriesId: String,
    val yearOptionIds: String?,
    val formationIds: String?
)

// mapper ResultRow -> DTO
fun ResultRow.toSeriesNameDTO() = SeriesNameDTO(
    id = this[SeriesNameTable.id].value,
    seriesId = this[SeriesNameTable.seriesId],
    yearOptionIds = this[SeriesNameTable.yearOptionIds],
    formationIds = this[SeriesNameTable.formationIds]
)

// DTO pour écriture (création et update complet)
@Serializable
data class SeriesNameWriteRequest(
    val seriesId: String,
    val yearOptionIds: String? = null,
    val formationIds: String? = null
)

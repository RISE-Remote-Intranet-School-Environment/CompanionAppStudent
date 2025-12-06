package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les options d'année
object YearOptionsTable : IntIdTable("year_options") {
    val yearOptionId = varchar("year_option_id", 50)
    val formationIds = text("formation_ids").nullable()
    val blocId = integer("bloc_id").nullable()
}

// DTO pour exposer les options d'année au front
@Serializable
data class YearOptionDTO(
    val id: Int,
    val yearOptionId: String,
    val formationIds: String?,
    val blocId: Int?
)

// mapper ResultRow -> DTO
fun ResultRow.toYearOptionDTO() = YearOptionDTO(
    id = this[YearOptionsTable.id].value,
    yearOptionId = this[YearOptionsTable.yearOptionId],
    formationIds = this[YearOptionsTable.formationIds],
    blocId = this[YearOptionsTable.blocId]
)

// DTO pour écriture (création et update complet)
@Serializable
data class YearOptionWriteRequest(
    val yearOptionId: String,
    val formationIds: String? = null,
    val blocId: Int? = null
)

// Table pour les années
object YearsTable : IntIdTable("years") {
    val yearId = varchar("year_id", 50)
    val yearNumber = integer("year_number")
}

// DTO pour exposer les années au front
@Serializable
data class YearDTO(
    val id: Int,
    val yearId: String,
    val yearNumber: Int
)

// mapper ResultRow -> DTO
fun ResultRow.toYearDTO() = YearDTO(
    id = this[YearsTable.id].value,
    yearId = this[YearsTable.yearId],
    yearNumber = this[YearsTable.yearNumber]
)

// DTO pour écriture (création et update complet)
@Serializable
data class YearWriteRequest(
    val yearId: String,
    val yearNumber: Int
)


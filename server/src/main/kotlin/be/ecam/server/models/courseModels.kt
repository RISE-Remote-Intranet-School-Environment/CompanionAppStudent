package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

// --- TABLES ---

object FormationTable : IntIdTable("formations") {
    val slug = varchar("slug", 50).uniqueIndex()
    val name = varchar("name", 255)
    val sourceUrl = varchar("source_url", 255)
}

object BlockTable : IntIdTable("blocks") {
    val name = varchar("name", 100)
    val formation = reference("formation_id", FormationTable)
}

object CourseTable : IntIdTable("courses") {
    val code = varchar("code", 50).uniqueIndex()
    val title = varchar("title", 255)
    val credits = integer("credits")
    val periods = varchar("periods", 20)
    val detailsUrl = varchar("details_url", 255)

    val mandatory = bool("mandatory").default(true)
    val bloc = varchar("bloc", 20).nullable()
    val program = varchar("program", 100).nullable()
    val language = varchar("language", 10).nullable()

    val formation = reference("formation_id", FormationTable).nullable()
    val block = reference("block_id", BlockTable).nullable()
}

// --- ENTITIES ---

class Formation(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Formation>(FormationTable)

    var slug by FormationTable.slug
    var name by FormationTable.name
    var sourceUrl by FormationTable.sourceUrl
}

class Block(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Block>(BlockTable)

    var name by BlockTable.name
    var formation by Formation referencedOn BlockTable.formation
}

class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(CourseTable)

    var code by CourseTable.code
    var title by CourseTable.title
    var credits by CourseTable.credits
    var periods by CourseTable.periods
    var detailsUrl by CourseTable.detailsUrl
    var mandatory by CourseTable.mandatory
    var bloc by CourseTable.bloc
    var program by CourseTable.program
    var language by CourseTable.language
    var formation by Formation optionalReferencedOn CourseTable.formation
    var blockRef by Block optionalReferencedOn CourseTable.block
}

// --- DTOs exposés par l'API ---

@Serializable
data class FormationDTO(
    val id: Int,
    val slug: String,
    val name: String
)

@Serializable
data class CourseDTO(
    val id: Int,
    val code: String,
    val title: String,
    val credits: Int,
    val bloc: String?,
    val formationSlug: String?
)


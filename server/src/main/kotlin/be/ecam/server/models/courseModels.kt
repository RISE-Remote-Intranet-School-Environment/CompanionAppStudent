package be.ecam.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


// table
object FormationTable : IntIdTable("formations") {
    // Use the DB column formation_id as the unique identifier (was previously named slug)
    val slug = varchar("formation_id", 255).uniqueIndex()
    val name = varchar("name", 255)
    val sourceUrl = varchar("source_url", 255)
    val imageUrl = varchar("image_url", 1024).nullable()
}

object BlockTable : IntIdTable("blocs") {
    val blocId = varchar("bloc_id", 255).uniqueIndex()
    val name = varchar("name", 255)
    // Optional semicolon-separated formation_ids.
    val formationIds = varchar("formation_ids", 255).nullable()
}

object CourseTable : IntIdTable("courses") {
    val code = varchar("course_id", 50)
    val courseRaccourci = varchar("course_raccourci_id", 50).nullable()
    val title = varchar("title", 255)
    val credits = varchar("credits", 50)
    val periods = varchar("periods", 255).nullable()
    val detailsUrl = varchar("details_url", 255).nullable()

    val mandatory = varchar("mandatory", 50).nullable()
    val bloc = varchar("bloc_id", 255).nullable()
    val language = varchar("language", 10).nullable()

    val formation = varchar("formation_id", 255).nullable()
}


// entity

class Formation(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Formation>(FormationTable)

    var slug by FormationTable.slug
    var name by FormationTable.name
    var sourceUrl by FormationTable.sourceUrl
    var imageUrl by FormationTable.imageUrl
}

class Block(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Block>(BlockTable)

    var blocId by BlockTable.blocId
    var name by BlockTable.name
    var formationIds by BlockTable.formationIds
}

class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(CourseTable)

    var code by CourseTable.code
    var courseRaccourci by CourseTable.courseRaccourci
    var title by CourseTable.title
    var credits by CourseTable.credits
    var periods by CourseTable.periods
    var detailsUrl by CourseTable.detailsUrl
    var mandatory by CourseTable.mandatory
    var bloc by CourseTable.bloc
    var language by CourseTable.language
    var formationId by CourseTable.formation
}



// dto read
@Serializable
data class FormationDTO(
    val id: Int,
    val slug: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class CourseDTO(
    val id: Int,
    val code: String,
    val title: String,
    val credits: Int,
    val periods: String?,
    val bloc: String?,
    @SerialName("details_url") val detailsUrl: String?,
    @SerialName("formation_slug") val formationSlug: String?
)


// dto read
@Serializable
data class BlockDTO(
    val id: Int,
    val name: String,
    @SerialName("formation_ids") val formationIds: List<String>
)


// dto write 
@Serializable
data class FormationWriteRequest(
    val slug: String,
    val name: String,
    @SerialName("source_url") val sourceUrl: String = "",
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class BlockWriteRequest(
    val name: String,
    @SerialName("formation_ids") val formationIds: List<String> = emptyList()
)

@Serializable
data class CourseWriteRequest(
    val code: String,
    val title: String,
    val credits: Int,
    val periods: String,
    @SerialName("details_url") val detailsUrl: String,
    val mandatory: Boolean = true,
    val bloc: String? = null,
    val program: String? = null,
    val language: String? = null,
    @SerialName("formation_id") val formationId: Int? = null,
    @SerialName("block_id") val blockId: Int? = null
)

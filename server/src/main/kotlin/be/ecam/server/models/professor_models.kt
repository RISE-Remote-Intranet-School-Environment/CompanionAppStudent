package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table
object ProfessorsTable : IntIdTable("professors") {

    val professorId = varchar("professor_id", 50).uniqueIndex()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 255).uniqueIndex()
    val roomIds = text("room_ids").nullable()
    val phone = varchar("phone", 50).nullable()
    val speciality = varchar("speciality", 255).nullable()
    val fullName = varchar("full_name", 255).nullable()
    val coursesId = text("courses_id").nullable()
    val photoUrl = text("photo_url").nullable()
    val roleTitle = text("role_title").nullable()
    val roleDetail = text("role_detail").nullable()
    val diplomas = text("diplomas").nullable()
}

// DTO pour exposer les professeurs au front
@Serializable
data class ProfessorDTO(
    val id: Int,
    val professorId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val roomIds: String?,
    val phone: String?,
    val speciality: String?,
    val fullName: String?,
    val coursesId: String?,
    val photoUrl: String?,
    val roleTitle: String?,
    val roleDetail: String?,
    val diplomas: String?
)

// mapper ResultRow -> DTO  
fun ResultRow.toProfessorDTO(): ProfessorDTO =
    ProfessorDTO(
        id = this[ProfessorsTable.id].value,
        professorId = this[ProfessorsTable.professorId],
        firstName = this[ProfessorsTable.firstName],
        lastName = this[ProfessorsTable.lastName],
        email = this[ProfessorsTable.email],
        roomIds = this[ProfessorsTable.roomIds],
        phone = this[ProfessorsTable.phone],
        speciality = this[ProfessorsTable.speciality],
        fullName = this[ProfessorsTable.fullName],
        coursesId = this[ProfessorsTable.coursesId],
        photoUrl = this[ProfessorsTable.photoUrl],
        roleTitle = this[ProfessorsTable.roleTitle],
        roleDetail = this[ProfessorsTable.roleDetail],
        diplomas = this[ProfessorsTable.diplomas]
    )


// DTO pour écriture (création et update complet)
@Serializable
data class ProfessorWriteRequest(
    val professorId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val roomIds: String? = null,
    val phone: String? = null,
    val speciality: String? = null,
    val fullName: String? = null,
    val coursesId: String? = null,
    val photoUrl: String? = null,
    val roleTitle: String? = null,
    val roleDetail: String? = null,
    val diplomas: String? = null
)

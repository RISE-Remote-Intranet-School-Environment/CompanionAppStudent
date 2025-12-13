package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

// table
object ProfessorsTable : IntIdTable("professors") {
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 255).uniqueIndex()
    val office = varchar("office", 100).nullable()
    val phone = varchar("phone", 50).nullable()
    val speciality = varchar("speciality", 255).nullable()
}

// entity
class Professor(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Professor>(ProfessorsTable)

    var firstName by ProfessorsTable.firstName
    var lastName by ProfessorsTable.lastName
    var email by ProfessorsTable.email
    var office by ProfessorsTable.office
    var phone by ProfessorsTable.phone
    var speciality by ProfessorsTable.speciality
}

// dto read
@Serializable
data class ProfessorDTO(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val office: String?,
    val phone: String?,
    val speciality: String?
)

// dto write
@Serializable
data class ProfessorWriteRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val office: String? = null,
    val phone: String? = null,
    val speciality: String? = null
)

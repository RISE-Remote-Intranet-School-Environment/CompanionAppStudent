package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow


// Table pour les étudiants
object StudentsTable : IntIdTable("students") {
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val ecamEmail = varchar("ecam_email", 255).uniqueIndex()
    val studentNumber = varchar("student_number", 50).uniqueIndex() 
    val groupCode = varchar("group_code", 50).nullable()            
}

// DTO pour exposer les étudiants au front
@Serializable
data class StudentDTO(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val ecamEmail: String,
    val studentNumber: String,
    val groupCode: String?
)


// mapper ResultRow -> DTO
fun ResultRow.toStudentDTO(): StudentDTO =
    StudentDTO(
        id = this[StudentsTable.id].value,
        firstName = this[StudentsTable.firstName],
        lastName = this[StudentsTable.lastName],
        ecamEmail = this[StudentsTable.ecamEmail],
        studentNumber = this[StudentsTable.studentNumber],
        groupCode = this[StudentsTable.groupCode]
    )

// DTO pour écriture (création et update complet)
@Serializable
data class StudentWriteRequest(
    val firstName: String,
    val lastName: String,
    val ecamEmail: String,
    val studentNumber: String,
    val groupCode: String? = null
)

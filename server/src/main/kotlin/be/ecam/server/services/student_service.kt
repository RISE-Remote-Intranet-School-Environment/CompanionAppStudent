package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object StudentService {

    //  GET all students
    fun getAllStudents(): List<StudentDTO> = transaction {
        StudentsTable
            .selectAll()
            .map { it.toStudentDTO() }
    }

    //  GET by DB id
    fun getStudentById(id: Int): StudentDTO? = transaction {
        StudentsTable
            .selectAll()
            .where { StudentsTable.id eq id }
            .singleOrNull()
            ?.toStudentDTO()
    }

    //  GET by student number 
    fun getStudentByNumber(studentNumber: String): StudentDTO? = transaction {
        StudentsTable
            .selectAll()
            .where { StudentsTable.studentNumber eq studentNumber }
            .singleOrNull()
            ?.toStudentDTO()
    }

    //  GET by ECAM email
    fun getStudentByEmail(email: String): StudentDTO? = transaction {
        StudentsTable
            .selectAll()
            .where { StudentsTable.ecamEmail eq email }
            .singleOrNull()
            ?.toStudentDTO()
    }

    //  GET by group code 
    fun getStudentsByGroup(groupCode: String): List<StudentDTO> = transaction {
        StudentsTable
            .selectAll()
            .where { StudentsTable.groupCode eq groupCode }
            .map { it.toStudentDTO() }
    }

    //  CREATE
    fun createStudent(req: StudentWriteRequest): StudentDTO = transaction {
        val newId = StudentsTable.insertAndGetId { row ->
            row[StudentsTable.firstName] = req.firstName
            row[StudentsTable.lastName] = req.lastName
            row[StudentsTable.ecamEmail] = req.ecamEmail
            row[StudentsTable.studentNumber] = req.studentNumber
            row[StudentsTable.groupCode] = req.groupCode
        }

        StudentsTable
            .selectAll()
            .where { StudentsTable.id eq newId }
            .single()
            .toStudentDTO()
    }

    // UPDATE
    fun updateStudent(id: Int, req: StudentWriteRequest): StudentDTO? = transaction {
        val updated = StudentsTable.update({ StudentsTable.id eq id }) { row ->
            row[StudentsTable.firstName] = req.firstName
            row[StudentsTable.lastName] = req.lastName
            row[StudentsTable.ecamEmail] = req.ecamEmail
            row[StudentsTable.studentNumber] = req.studentNumber
            row[StudentsTable.groupCode] = req.groupCode
        }

        if (updated == 0) return@transaction null

        StudentsTable
            .selectAll()
            .where { StudentsTable.id eq id }
            .singleOrNull()
            ?.toStudentDTO()
    }

    // DELETE
    fun deleteStudent(id: Int): Boolean = transaction {
        StudentsTable.deleteWhere { StudentsTable.id eq id } > 0
    }
}

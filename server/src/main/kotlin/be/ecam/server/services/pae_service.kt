package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object PaeStudentService {

    //  GET all PAE students
    fun getAllPaeStudents(): List<PaeStudentDTO> = transaction {
        PaeStudentsTable
            .selectAll()
            .map { it.toPaeStudentDTO() }
    }

    //  GET by DB id
    fun getPaeStudentById(id: Int): PaeStudentDTO? = transaction {
        PaeStudentsTable
            .selectAll()
            .where { PaeStudentsTable.id eq id }
            .singleOrNull()
            ?.toPaeStudentDTO()
    }

    //  GET by studentId (numéro ECAM)
    fun getPaeStudentsByStudentId(studentId: Int): List<PaeStudentDTO> = transaction {
        PaeStudentsTable
            .selectAll()
            .where { PaeStudentsTable.studentId eq studentId }
            .map { it.toPaeStudentDTO() }
    }

    //  GET by formation
    fun getPaeStudentsByFormation(formationId: String): List<PaeStudentDTO> = transaction {
        PaeStudentsTable
            .selectAll()
            .where { PaeStudentsTable.formationId eq formationId }
            .map { it.toPaeStudentDTO() }
    }

    //  GET by bloc
    fun getPaeStudentsByBloc(blocId: String): List<PaeStudentDTO> = transaction {
        PaeStudentsTable
            .selectAll()
            .where { PaeStudentsTable.blocId eq blocId }
            .map { it.toPaeStudentDTO() }
    }

    //  CREATE
    fun createPaeStudent(req: PaeStudentWriteRequest): PaeStudentDTO = transaction {
        val newId = PaeStudentsTable.insertAndGetId { row ->
            row[PaeStudentsTable.studentId] = req.studentId
            row[PaeStudentsTable.studentName] = req.studentName
            row[PaeStudentsTable.email] = req.email
            row[PaeStudentsTable.role] = req.role
            row[PaeStudentsTable.program] = req.program
            row[PaeStudentsTable.enrolYear] = req.enrolYear
            row[PaeStudentsTable.formationId] = req.formationId
            row[PaeStudentsTable.blocId] = req.blocId
            row[PaeStudentsTable.courseIds] = req.courseIds
        }

        PaeStudentsTable
            .selectAll()
            .where { PaeStudentsTable.id eq newId }
            .single()
            .toPaeStudentDTO()
    }

    //  UPDATE
    fun updatePaeStudent(id: Int, req: PaeStudentWriteRequest): PaeStudentDTO? = transaction {
        val updated = PaeStudentsTable.update({ PaeStudentsTable.id eq id }) { row ->
            row[PaeStudentsTable.studentId] = req.studentId
            row[PaeStudentsTable.studentName] = req.studentName
            row[PaeStudentsTable.email] = req.email
            row[PaeStudentsTable.role] = req.role
            row[PaeStudentsTable.program] = req.program
            row[PaeStudentsTable.enrolYear] = req.enrolYear
            row[PaeStudentsTable.formationId] = req.formationId
            row[PaeStudentsTable.blocId] = req.blocId
            row[PaeStudentsTable.courseIds] = req.courseIds
        }

        if (updated == 0) return@transaction null

        PaeStudentsTable
            .selectAll()
            .where { PaeStudentsTable.id eq id }
            .singleOrNull()
            ?.toPaeStudentDTO()
    }

    //  DELETE
    fun deletePaeStudent(id: Int): Boolean = transaction {
        PaeStudentsTable.deleteWhere { PaeStudentsTable.id eq id } > 0
    }
}

package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object StudentSubmissionsService {

    // CREATE
    fun createSubmission(req: StudentSubmissionCreateRequest): StudentSubmissionDTO = transaction {
        val now = System.currentTimeMillis()

        val newId = StudentSubmissionsTable.insertAndGetId { row ->
            row[StudentSubmissionsTable.student] = req.studentId
            row[StudentSubmissionsTable.courseId] = req.courseId
            row[StudentSubmissionsTable.sousCourseId] = req.sousCourseId
            row[StudentSubmissionsTable.title] = req.title
            row[StudentSubmissionsTable.fileUrl] = req.fileUrl
            row[StudentSubmissionsTable.mimeType] = req.mimeType
            row[StudentSubmissionsTable.uploadedAt] = now
            row[StudentSubmissionsTable.grade] = null
            row[StudentSubmissionsTable.feedback] = null
        }

        StudentSubmissionsTable
            .selectAll()
            .where { StudentSubmissionsTable.id eq newId }
            .single()
            .toStudentSubmissionDTO()
    }

    // READ
    fun getSubmissionById(id: Int): StudentSubmissionDTO? = transaction {
        StudentSubmissionsTable
            .selectAll()
            .where { StudentSubmissionsTable.id eq id }
            .singleOrNull()
            ?.toStudentSubmissionDTO()
    }

    fun getSubmissionsByStudent(studentId: Int): List<StudentSubmissionDTO> = transaction {
        StudentSubmissionsTable
            .selectAll()
            .where { StudentSubmissionsTable.student eq studentId }
            .map { it.toStudentSubmissionDTO() }
    }

    fun getSubmissionsByCourse(courseId: String): List<StudentSubmissionDTO> = transaction {
        StudentSubmissionsTable
            .selectAll()
            .where { StudentSubmissionsTable.courseId eq courseId }
            .map { it.toStudentSubmissionDTO() }
    }

    fun getSubmissionsBySousCourse(sousCourseId: String): List<StudentSubmissionDTO> = transaction {
        StudentSubmissionsTable
            .selectAll()
            .where { StudentSubmissionsTable.sousCourseId eq sousCourseId }
            .map { it.toStudentSubmissionDTO() }
    }

    // UPDATE
    fun updateSubmission(id: Int, req: StudentSubmissionUpdateRequest): StudentSubmissionDTO? = transaction {
        val updated = StudentSubmissionsTable.update({ StudentSubmissionsTable.id eq id }) { row ->
            req.title?.let { row[StudentSubmissionsTable.title] = it }
            req.grade?.let { row[StudentSubmissionsTable.grade] = it }
            req.feedback?.let { row[StudentSubmissionsTable.feedback] = it }
        }

        if (updated == 0) return@transaction null

        StudentSubmissionsTable
            .selectAll()
            .where { StudentSubmissionsTable.id eq id }
            .singleOrNull()
            ?.toStudentSubmissionDTO()
    }

    // DELETE
    fun deleteSubmission(id: Int): Boolean = transaction {
        StudentSubmissionsTable.deleteWhere { StudentSubmissionsTable.id eq id } > 0
    }
}

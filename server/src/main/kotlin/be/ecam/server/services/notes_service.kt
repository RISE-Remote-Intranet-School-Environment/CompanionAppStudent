package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object NotesStudentService {

    // 🔹 GET all notes
    fun getAllNotes(): List<NotesStudentDTO> = transaction {
        NotesStudentsTable
            .selectAll()
            .map { it.toNotesStudentDTO() }
    }

    // 🔹 GET by DB id
    fun getNoteById(id: Int): NotesStudentDTO? = transaction {
        NotesStudentsTable
            .selectAll()
            .where { NotesStudentsTable.id eq id }
            .singleOrNull()
            ?.toNotesStudentDTO()
    }

    // 🔹 GET by studentId
    fun getNotesByStudentId(studentId: Int): List<NotesStudentDTO> = transaction {
        NotesStudentsTable
            .selectAll()
            .where { NotesStudentsTable.studentId eq studentId }
            .map { it.toNotesStudentDTO() }
    }

    // 🔹 GET by studentId + academicYear
    fun getNotesByStudentAndYear(studentId: Int, academicYear: String): List<NotesStudentDTO> = transaction {
        NotesStudentsTable
            .selectAll()
            .where {
                (NotesStudentsTable.studentId eq studentId) and
                (NotesStudentsTable.academicYear eq academicYear)
            }
            .map { it.toNotesStudentDTO() }
    }

    // 🔹 GET by courseId
    fun getNotesByCourseId(courseId: String): List<NotesStudentDTO> = transaction {
        NotesStudentsTable
            .selectAll()
            .where { NotesStudentsTable.courseId eq courseId }
            .map { it.toNotesStudentDTO() }
    }

    // 🔹 CREATE
    fun createNote(req: NotesStudentWriteRequest): NotesStudentDTO = transaction {
        val newId = NotesStudentsTable.insertAndGetId { row ->
            row[NotesStudentsTable.studentId] = req.studentId
            row[NotesStudentsTable.academicYear] = req.academicYear
            row[NotesStudentsTable.formationId] = req.formationId
            row[NotesStudentsTable.blocId] = req.blocId
            row[NotesStudentsTable.courseId] = req.courseId
            row[NotesStudentsTable.courseTitle] = req.courseTitle
            row[NotesStudentsTable.courseEcts] = req.courseEcts
            row[NotesStudentsTable.coursePeriod] = req.coursePeriod
            row[NotesStudentsTable.courseId1] = req.courseId1
            row[NotesStudentsTable.courseSessionJan] = req.courseSessionJan
            row[NotesStudentsTable.courseSessionJun] = req.courseSessionJun
            row[NotesStudentsTable.courseSessionSep] = req.courseSessionSep
            row[NotesStudentsTable.componentCode] = req.componentCode
            row[NotesStudentsTable.componentTitle] = req.componentTitle
            row[NotesStudentsTable.componentWeight] = req.componentWeight
            row[NotesStudentsTable.componentSessionJan] = req.componentSessionJan
            row[NotesStudentsTable.componentSessionJun] = req.componentSessionJun
            row[NotesStudentsTable.componentSessionSep] = req.componentSessionSep
        }

        NotesStudentsTable
            .selectAll()
            .where { NotesStudentsTable.id eq newId }
            .single()
            .toNotesStudentDTO()
    }

    // 🔹 UPDATE
    fun updateNote(id: Int, req: NotesStudentWriteRequest): NotesStudentDTO? = transaction {
        val updated = NotesStudentsTable.update({ NotesStudentsTable.id eq id }) { row ->
            row[NotesStudentsTable.studentId] = req.studentId
            row[NotesStudentsTable.academicYear] = req.academicYear
            row[NotesStudentsTable.formationId] = req.formationId
            row[NotesStudentsTable.blocId] = req.blocId
            row[NotesStudentsTable.courseId] = req.courseId
            row[NotesStudentsTable.courseTitle] = req.courseTitle
            row[NotesStudentsTable.courseEcts] = req.courseEcts
            row[NotesStudentsTable.coursePeriod] = req.coursePeriod
            row[NotesStudentsTable.courseId1] = req.courseId1
            row[NotesStudentsTable.courseSessionJan] = req.courseSessionJan
            row[NotesStudentsTable.courseSessionJun] = req.courseSessionJun
            row[NotesStudentsTable.courseSessionSep] = req.courseSessionSep
            row[NotesStudentsTable.componentCode] = req.componentCode
            row[NotesStudentsTable.componentTitle] = req.componentTitle
            row[NotesStudentsTable.componentWeight] = req.componentWeight
            row[NotesStudentsTable.componentSessionJan] = req.componentSessionJan
            row[NotesStudentsTable.componentSessionJun] = req.componentSessionJun
            row[NotesStudentsTable.componentSessionSep] = req.componentSessionSep
        }

        if (updated == 0) return@transaction null

        NotesStudentsTable
            .selectAll()
            .where { NotesStudentsTable.id eq id }
            .singleOrNull()
            ?.toNotesStudentDTO()
    }

    // 🔹 DELETE
    fun deleteNote(id: Int): Boolean = transaction {
        NotesStudentsTable.deleteWhere { NotesStudentsTable.id eq id } > 0
    }
}

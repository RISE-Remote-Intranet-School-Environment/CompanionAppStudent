package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object SousCourseService {

    // 🔹 GET all sous-courses
    fun getAllSousCourses(): List<SousCourseDTO> = transaction {
        SousCoursesTable
            .selectAll()
            .map { it.toSousCourseDTO() }
    }

    // 🔹 GET by DB id
    fun getSousCourseById(id: Int): SousCourseDTO? = transaction {
        SousCoursesTable
            .selectAll()
            .where { SousCoursesTable.id eq id }
            .singleOrNull()
            ?.toSousCourseDTO()
    }

    // 🔹 GET by sousCourseId (logique ex: "4EIDB40-TD1")
    fun getSousCoursesBySousCourseId(sousId: String): List<SousCourseDTO> = transaction {
        SousCoursesTable
            .selectAll()
            .where { SousCoursesTable.sousCourseId eq sousId }
            .map { it.toSousCourseDTO() }
    }

    // 🔹 GET by courseId (tous les sous-cours d’un cours principal)
    fun getSousCoursesByCourseId(courseId: String): List<SousCourseDTO> = transaction {
        SousCoursesTable
            .selectAll()
            .where { SousCoursesTable.courseId eq courseId }
            .map { it.toSousCourseDTO() }
    }

    // 🔹 CREATE
    fun createSousCourse(req: SousCourseWriteRequest): SousCourseDTO = transaction {
        val newId = SousCoursesTable.insertAndGetId { row ->
            row[SousCoursesTable.sousCourseId] = req.sousCourseId
            row[SousCoursesTable.courseId] = req.courseId
            row[SousCoursesTable.title] = req.title
            row[SousCoursesTable.hoursQ1] = req.hoursQ1
            row[SousCoursesTable.hoursQ2] = req.hoursQ2
            row[SousCoursesTable.teachersIds] = req.teachersIds
            row[SousCoursesTable.language] = req.language
        }

        SousCoursesTable
            .selectAll()
            .where { SousCoursesTable.id eq newId }
            .single()
            .toSousCourseDTO()
    }

    // 🔹 UPDATE
    fun updateSousCourse(id: Int, req: SousCourseWriteRequest): SousCourseDTO? = transaction {
        val updated = SousCoursesTable.update({ SousCoursesTable.id eq id }) { row ->
            row[SousCoursesTable.sousCourseId] = req.sousCourseId
            row[SousCoursesTable.courseId] = req.courseId
            row[SousCoursesTable.title] = req.title
            row[SousCoursesTable.hoursQ1] = req.hoursQ1
            row[SousCoursesTable.hoursQ2] = req.hoursQ2
            row[SousCoursesTable.teachersIds] = req.teachersIds
            row[SousCoursesTable.language] = req.language
        }

        if (updated == 0) return@transaction null

        SousCoursesTable
            .selectAll()
            .where { SousCoursesTable.id eq id }
            .singleOrNull()
            ?.toSousCourseDTO()
    }

    // 🔹 DELETE
    fun deleteSousCourse(id: Int): Boolean = transaction {
        SousCoursesTable.deleteWhere { SousCoursesTable.id eq id } > 0
    }
}

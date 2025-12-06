package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CourseService {

    // 🔹 GET all courses
    fun getAllCourses(): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .map { it.toCourseDTO() }
    }

    // 🔹 GET by DB id
    fun getCourseById(id: Int): CourseDTO? = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.id eq id }
            .singleOrNull()
            ?.toCourseDTO()
    }

    // 🔹 GET by logical courseId (ex: "4EIDB40")
    fun getCoursesByCourseId(courseId: String): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.courseId eq courseId }
            .map { it.toCourseDTO() }
    }

    // 🔹 GET by raccourci id (ex: "DB", "SOC", etc.)
    fun getCoursesByShortId(shortId: String): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.courseRaccourciId eq shortId }
            .map { it.toCourseDTO() }
    }

    // 🔹 GET by blocId
    fun getCoursesByBlocId(blocId: Int): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.blocId eq blocId }
            .map { it.toCourseDTO() }
    }

    // 🔹 GET by formationId
    fun getCoursesByFormationId(formationId: Int): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.formationId eq formationId }
            .map { it.toCourseDTO() }
    }

    // 🔹 CREATE
    fun createCourse(req: CourseWriteRequest): CourseDTO = transaction {
        val newId = CoursesTable.insertAndGetId { row ->
            row[CoursesTable.courseId] = req.courseId
            row[CoursesTable.courseRaccourciId] = req.courseRaccourciId
            row[CoursesTable.title] = req.title
            row[CoursesTable.credits] = req.credits
            row[CoursesTable.periods] = req.periods
            row[CoursesTable.detailsUrl] = req.detailsUrl
            row[CoursesTable.mandatory] = req.mandatory
            row[CoursesTable.blocId] = req.blocId
            row[CoursesTable.formationId] = req.formationId
            row[CoursesTable.language] = req.language
        }

        CoursesTable
            .selectAll()
            .where { CoursesTable.id eq newId }
            .single()
            .toCourseDTO()
    }

    // 🔹 UPDATE
    fun updateCourse(id: Int, req: CourseWriteRequest): CourseDTO? = transaction {
        val updated = CoursesTable.update({ CoursesTable.id eq id }) { row ->
            row[CoursesTable.courseId] = req.courseId
            row[CoursesTable.courseRaccourciId] = req.courseRaccourciId
            row[CoursesTable.title] = req.title
            row[CoursesTable.credits] = req.credits
            row[CoursesTable.periods] = req.periods
            row[CoursesTable.detailsUrl] = req.detailsUrl
            row[CoursesTable.mandatory] = req.mandatory
            row[CoursesTable.blocId] = req.blocId
            row[CoursesTable.formationId] = req.formationId
            row[CoursesTable.language] = req.language
        }

        if (updated == 0) return@transaction null

        CoursesTable
            .selectAll()
            .where { CoursesTable.id eq id }
            .singleOrNull()
            ?.toCourseDTO()
    }

    // 🔹 DELETE
    fun deleteCourse(id: Int): Boolean = transaction {
        CoursesTable.deleteWhere { CoursesTable.id eq id } > 0
    }
}

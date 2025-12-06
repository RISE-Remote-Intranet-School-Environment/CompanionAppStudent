package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CourseDetailsService {

    // GET all
    fun getAllCourseDetails(): List<CourseDetailsDTO> = transaction {
        CourseDetailsTable
            .selectAll()
            .map { it.toCourseDetailsDTO() }
    }

    // GET by DB id
    fun getCourseDetailsById(id: Int): CourseDetailsDTO? = transaction {
        CourseDetailsTable
            .selectAll()
            .where { CourseDetailsTable.id eq id }
            .singleOrNull()
            ?.toCourseDetailsDTO()
    }

    // GET by logical courseId
    fun getCourseDetailsByCourseId(courseId: String): List<CourseDetailsDTO> = transaction {
        CourseDetailsTable
            .selectAll()
            .where { CourseDetailsTable.courseId eq courseId }
            .map { it.toCourseDetailsDTO() }
    }

    // GET by sousCourseId
    fun getCourseDetailsBySousCourseId(sousCourseId: String): List<CourseDetailsDTO> = transaction {
        CourseDetailsTable
            .selectAll()
            .where { CourseDetailsTable.sousCourseId eq sousCourseId }
            .map { it.toCourseDetailsDTO() }
    }

    // GET by blocId
    fun getCourseDetailsByBlocId(blocId: Int): List<CourseDetailsDTO> = transaction {
        CourseDetailsTable
            .selectAll()
            .where { CourseDetailsTable.blocId eq blocId }
            .map { it.toCourseDetailsDTO() }
    }

    // CREATE
    fun createCourseDetails(req: CourseDetailsWriteRequest): CourseDetailsDTO = transaction {
        val newId = CourseDetailsTable.insertAndGetId { row ->
            row[CourseDetailsTable.courseId] = req.courseId
            row[CourseDetailsTable.responsable] = req.responsable
            row[CourseDetailsTable.sousCourseId] = req.sousCourseId
            row[CourseDetailsTable.teachersRawId] = req.teachersRawId
            row[CourseDetailsTable.formationIds] = req.formationIds
            row[CourseDetailsTable.periods] = req.periods
            row[CourseDetailsTable.hoursQ1] = req.hoursQ1
            row[CourseDetailsTable.hoursQ2] = req.hoursQ2
            row[CourseDetailsTable.contribution] = req.contribution
            row[CourseDetailsTable.learningOutcomes] = req.learningOutcomes
            row[CourseDetailsTable.content] = req.content
            row[CourseDetailsTable.teachingMethods] = req.teachingMethods
            row[CourseDetailsTable.evaluationMethods] = req.evaluationMethods
            row[CourseDetailsTable.courseMaterial] = req.courseMaterial
            row[CourseDetailsTable.bibliography] = req.bibliography
            row[CourseDetailsTable.blocId] = req.blocId
        }

        CourseDetailsTable
            .selectAll()
            .where { CourseDetailsTable.id eq newId }
            .single()
            .toCourseDetailsDTO()
    }

    // UPDATE
    fun updateCourseDetails(id: Int, req: CourseDetailsWriteRequest): CourseDetailsDTO? = transaction {
        val updated = CourseDetailsTable.update({ CourseDetailsTable.id eq id }) { row ->
            row[CourseDetailsTable.courseId] = req.courseId
            row[CourseDetailsTable.responsable] = req.responsable
            row[CourseDetailsTable.sousCourseId] = req.sousCourseId
            row[CourseDetailsTable.teachersRawId] = req.teachersRawId
            row[CourseDetailsTable.formationIds] = req.formationIds
            row[CourseDetailsTable.periods] = req.periods
            row[CourseDetailsTable.hoursQ1] = req.hoursQ1
            row[CourseDetailsTable.hoursQ2] = req.hoursQ2
            row[CourseDetailsTable.contribution] = req.contribution
            row[CourseDetailsTable.learningOutcomes] = req.learningOutcomes
            row[CourseDetailsTable.content] = req.content
            row[CourseDetailsTable.teachingMethods] = req.teachingMethods
            row[CourseDetailsTable.evaluationMethods] = req.evaluationMethods
            row[CourseDetailsTable.courseMaterial] = req.courseMaterial
            row[CourseDetailsTable.bibliography] = req.bibliography
            row[CourseDetailsTable.blocId] = req.blocId
        }

        if (updated == 0) return@transaction null

        CourseDetailsTable
            .selectAll()
            .where { CourseDetailsTable.id eq id }
            .singleOrNull()
            ?.toCourseDetailsDTO()
    }

    // DELETE
    fun deleteCourseDetails(id: Int): Boolean = transaction {
        CourseDetailsTable.deleteWhere { CourseDetailsTable.id eq id } > 0
    }
}

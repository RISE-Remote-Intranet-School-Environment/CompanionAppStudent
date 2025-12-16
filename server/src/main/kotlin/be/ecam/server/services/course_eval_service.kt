package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CourseEvaluationService {

    // GET all
    fun getAllEvaluations(): List<CourseEvaluationDTO> = transaction {
        CourseEvaluationTable
            .selectAll()
            .map { it.toCourseEvaluationDTO() }
    }

    // GET by DB id
    fun getEvaluationById(id: Int): CourseEvaluationDTO? = transaction {
        CourseEvaluationTable
            .selectAll()
            .where { CourseEvaluationTable.id eq id }
            .singleOrNull()
            ?.toCourseEvaluationDTO()
    }

    // GET by evaluatedActivityId 
    fun getEvaluationsByActivityId(activityId: String): List<CourseEvaluationDTO> = transaction {
        CourseEvaluationTable
            .selectAll()
            .where { CourseEvaluationTable.evaluatedActivityId eq activityId }
            .map { it.toCourseEvaluationDTO() }
    }

    // GET by courseId
    fun getEvaluationsByCourseId(courseId: String): List<CourseEvaluationDTO> = transaction {
        CourseEvaluationTable
            .selectAll()
            .where { CourseEvaluationTable.courseId eq courseId }
            .map { it.toCourseEvaluationDTO() }
    }

    // CREATE
    fun createEvaluation(req: CourseEvaluationWriteRequest): CourseEvaluationDTO = transaction {
        val newId = CourseEvaluationTable.insertAndGetId { row ->
            row[CourseEvaluationTable.evaluatedActivityId] = req.evaluatedActivityId
            row[CourseEvaluationTable.courseId] = req.courseId
            row[CourseEvaluationTable.weight] = req.weight
            row[CourseEvaluationTable.typeQ1] = req.typeQ1
            row[CourseEvaluationTable.typeQ2] = req.typeQ2
            row[CourseEvaluationTable.typeQ3] = req.typeQ3
            row[CourseEvaluationTable.sousCourseIds] = req.sousCourseIds
            row[CourseEvaluationTable.teachersIds] = req.teachersIds
        }

        CourseEvaluationTable
            .selectAll()
            .where { CourseEvaluationTable.id eq newId }
            .single()
            .toCourseEvaluationDTO()
    }

    //  UPDATE
    fun updateEvaluation(id: Int, req: CourseEvaluationWriteRequest): CourseEvaluationDTO? = transaction {
        val updated = CourseEvaluationTable.update({ CourseEvaluationTable.id eq id }) { row ->
            row[CourseEvaluationTable.evaluatedActivityId] = req.evaluatedActivityId
            row[CourseEvaluationTable.courseId] = req.courseId
            row[CourseEvaluationTable.weight] = req.weight
            row[CourseEvaluationTable.typeQ1] = req.typeQ1
            row[CourseEvaluationTable.typeQ2] = req.typeQ2
            row[CourseEvaluationTable.typeQ3] = req.typeQ3
            row[CourseEvaluationTable.sousCourseIds] = req.sousCourseIds
            row[CourseEvaluationTable.teachersIds] = req.teachersIds
        }

        if (updated == 0) return@transaction null

        CourseEvaluationTable
            .selectAll()
            .where { CourseEvaluationTable.id eq id }
            .singleOrNull()
            ?.toCourseEvaluationDTO()
    }

    //  DELETE
    fun deleteEvaluation(id: Int): Boolean = transaction {
        CourseEvaluationTable.deleteWhere { CourseEvaluationTable.id eq id } > 0
    }
}

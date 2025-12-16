package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CourseResourcesService {

    fun getAllResources(): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .selectAll()
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTO() }
    }

    fun getResourceById(id: Int): CourseResourceDTO? = transaction {
        CourseResourcesTable
            .selectAll()
            .where { CourseResourcesTable.id eq id }
            .singleOrNull()
            ?.toCourseResourceDTO()
    }

    fun getResourcesByProfessor(professorId: Int): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .selectAll()
            .where { CourseResourcesTable.professor eq professorId }
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTO() }
    }

    fun getResourcesByCourse(courseId: String): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .selectAll()
            .where { CourseResourcesTable.courseId eq courseId }
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTO() }
    }

    fun getResourcesBySousCourse(sousCourseId: String): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .selectAll()
            .where { CourseResourcesTable.sousCourseId eq sousCourseId }
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTO() }
    }

    fun createResource(req: CourseResourceCreateRequest): CourseResourceDTO = transaction {
        val now = System.currentTimeMillis()

        val newId = CourseResourcesTable.insertAndGetId { row ->
            row[CourseResourcesTable.professor] = req.professorId
            row[CourseResourcesTable.courseId] = req.courseId
            row[CourseResourcesTable.sousCourseId] = req.sousCourseId
            row[CourseResourcesTable.title] = req.title
            row[CourseResourcesTable.description] = req.description
            row[CourseResourcesTable.resourceType] = req.resourceType
            row[CourseResourcesTable.resourceUrl] = req.resourceUrl
            row[CourseResourcesTable.thumbnailUrl] = req.thumbnailUrl
            row[CourseResourcesTable.durationSeconds] = req.durationSeconds
            row[CourseResourcesTable.uploadedAt] = now
        }

        CourseResourcesTable
            .selectAll()
            .where { CourseResourcesTable.id eq newId }
            .single()
            .toCourseResourceDTO()
    }

    fun updateResource(id: Int, req: CourseResourceUpdateRequest): CourseResourceDTO? = transaction {
        val updated = CourseResourcesTable.update({ CourseResourcesTable.id eq id }) { row ->
            req.title?.let { row[CourseResourcesTable.title] = it }
            req.description?.let { row[CourseResourcesTable.description] = it }
            req.resourceType?.let { row[CourseResourcesTable.resourceType] = it }
            req.resourceUrl?.let { row[CourseResourcesTable.resourceUrl] = it }
            req.thumbnailUrl?.let { row[CourseResourcesTable.thumbnailUrl] = it }
            req.durationSeconds?.let { row[CourseResourcesTable.durationSeconds] = it }
        }

        if (updated == 0) return@transaction null

        CourseResourcesTable
            .selectAll()
            .where { CourseResourcesTable.id eq id }
            .singleOrNull()
            ?.toCourseResourceDTO()
    }

    fun deleteResource(id: Int): Boolean = transaction {
        CourseResourcesTable.deleteWhere { CourseResourcesTable.id eq id } > 0
    }
}

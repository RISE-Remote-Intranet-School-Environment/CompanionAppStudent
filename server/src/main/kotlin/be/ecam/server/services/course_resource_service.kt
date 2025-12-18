package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CourseResourcesService {

    fun getAllResources(): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.INNER, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .slice(CourseResourcesTable.columns + ProfessorsTable.professorId)
            .selectAll()
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTOWithProfessorCode() }
    }

    fun getResourceById(id: Int): CourseResourceDTO? = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.INNER, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .slice(CourseResourcesTable.columns + ProfessorsTable.professorId)
            .select { CourseResourcesTable.id eq id }
            .singleOrNull()
            ?.toCourseResourceDTOWithProfessorCode()
    }

    fun getResourcesByProfessor(professorId: Int): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.INNER, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .slice(CourseResourcesTable.columns + ProfessorsTable.professorId)
            .select { CourseResourcesTable.professor eq professorId }
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTOWithProfessorCode() }
    }

    fun getResourcesByCourse(courseId: String): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.INNER, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .slice(CourseResourcesTable.columns + ProfessorsTable.professorId)
            .select { CourseResourcesTable.courseId eq courseId }
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTOWithProfessorCode() }
    }

    fun getResourcesBySousCourse(sousCourseId: String): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.INNER, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .slice(CourseResourcesTable.columns + ProfessorsTable.professorId)
            .select { CourseResourcesTable.sousCourseId eq sousCourseId }
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTOWithProfessorCode() }
    }

    fun createResource(req: CourseResourceCreateRequest): CourseResourceDTO = transaction {
        val profRow = ProfessorsTable
            .select { ProfessorsTable.professorId eq req.professorId }
            .singleOrNull()
            ?: throw IllegalArgumentException("Professor code not found")

        val profPk = profRow[ProfessorsTable.id]

        val now = System.currentTimeMillis()

        val newId = CourseResourcesTable.insertAndGetId { row ->
            row[CourseResourcesTable.professor] = profPk
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
            .join(ProfessorsTable, JoinType.INNER, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .slice(CourseResourcesTable.columns + ProfessorsTable.professorId)
            .select { CourseResourcesTable.id eq newId }
            .single()
            .toCourseResourceDTOWithProfessorCode()
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
            .join(ProfessorsTable, JoinType.INNER, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .slice(CourseResourcesTable.columns + ProfessorsTable.professorId)
            .select { CourseResourcesTable.id eq id }
            .singleOrNull()
            ?.toCourseResourceDTOWithProfessorCode()
    }

    fun deleteResource(id: Int): Boolean = transaction {
        CourseResourcesTable.deleteWhere { CourseResourcesTable.id eq id } > 0
    }
}

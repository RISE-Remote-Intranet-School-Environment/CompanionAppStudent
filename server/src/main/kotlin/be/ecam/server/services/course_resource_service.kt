package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CourseResourcesService {

    fun getAllResources(): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.LEFT, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .selectAll()
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTOWithProfessorCode() }
    }

    fun getResourceById(id: Int): CourseResourceDTO? = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.LEFT, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .selectAll()
            .where { CourseResourcesTable.id eq id }
            .singleOrNull()
            ?.toCourseResourceDTOWithProfessorCode()
    }

    fun getResourcesByProfessor(professorId: Int): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.LEFT, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .selectAll()
            .where { CourseResourcesTable.professor eq professorId }
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTOWithProfessorCode() }
    }

    fun getResourcesByCourse(courseId: String): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.LEFT, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .selectAll()
            .where { CourseResourcesTable.courseId eq courseId }
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTOWithProfessorCode() }
    }

    fun getResourcesBySousCourse(sousCourseId: String): List<CourseResourceDTO> = transaction {
        CourseResourcesTable
            .join(ProfessorsTable, JoinType.LEFT, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .selectAll()
            .where { CourseResourcesTable.sousCourseId eq sousCourseId }
            .orderBy(CourseResourcesTable.uploadedAt, SortOrder.DESC)
            .map { it.toCourseResourceDTOWithProfessorCode() }
    }

    fun createResource(req: CourseResourceCreateRequest): CourseResourceDTO = transaction {
        val profPk: Int? = req.professorId?.let { id ->
            ProfessorsTable
                .selectAll()
                .where { ProfessorsTable.id eq id }
                .singleOrNull()
                ?.get(ProfessorsTable.id)?.value
                ?: throw IllegalArgumentException("Professor not found")
        }

        val now = System.currentTimeMillis()

        val newId = CourseResourcesTable.insertAndGetId { row ->
            row[CourseResourcesTable.professor] = profPk
            row[CourseResourcesTable.courseId] = req.courseId
            row[CourseResourcesTable.sousCourseId] = req.sousCourseId
            row[CourseResourcesTable.title] = req.title
            row[CourseResourcesTable.type] = req.type
            row[CourseResourcesTable.url] = req.url
            row[CourseResourcesTable.uploadedAt] = now
        }

        CourseResourcesTable
            .join(ProfessorsTable, JoinType.LEFT, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
            .selectAll()
            .where { CourseResourcesTable.id eq newId }
            .single()
            .toCourseResourceDTOWithProfessorCode()
    }

    fun updateResource(id: Int, req: CourseResourceUpdateRequest): CourseResourceDTO? = transaction {
        val updatedCount = CourseResourcesTable.update({ CourseResourcesTable.id eq id }) { row ->
            req.title?.let { row[CourseResourcesTable.title] = it }
            req.type?.let { row[CourseResourcesTable.type] = it }
            req.url?.let { row[CourseResourcesTable.url] = it }
        }

        if (updatedCount > 0) {
            CourseResourcesTable
                .join(ProfessorsTable, JoinType.LEFT, onColumn = CourseResourcesTable.professor, otherColumn = ProfessorsTable.id)
                .selectAll()
                .where { CourseResourcesTable.id eq id }
                .singleOrNull()
                ?.toCourseResourceDTOWithProfessorCode()
        } else {
            null
        }
    }

    fun deleteResource(id: Int): Boolean = transaction {
        CourseResourcesTable.deleteWhere { CourseResourcesTable.id eq id } > 0
    }
}

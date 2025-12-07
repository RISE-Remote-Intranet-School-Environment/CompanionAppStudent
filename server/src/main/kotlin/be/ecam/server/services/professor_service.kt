package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object ProfessorService {

    //  GET all professors
    fun getAllProfessors(): List<ProfessorDTO> = transaction {
        ProfessorsTable
            .selectAll()
            .map { it.toProfessorDTO() }
    }

    //  GET by DB id
    fun getProfessorById(id: Int): ProfessorDTO? = transaction {
        ProfessorsTable
            .selectAll()
            .where { ProfessorsTable.id eq id }
            .singleOrNull()
            ?.toProfessorDTO()
    }

    //  GET by logical professorId (ex: "P123")
    fun getProfessorByProfessorId(professorId: String): ProfessorDTO? = transaction {
        ProfessorsTable
            .selectAll()
            .where { ProfessorsTable.professorId eq professorId }
            .singleOrNull()
            ?.toProfessorDTO()
    }

    //  GET by email
    fun getProfessorByEmail(email: String): ProfessorDTO? = transaction {
        ProfessorsTable
            .selectAll()
            .where { ProfessorsTable.email eq email }
            .singleOrNull()
            ?.toProfessorDTO()
    }

    //  GET by speciality (tous les profs d’un domaine)
    fun getProfessorsBySpeciality(speciality: String): List<ProfessorDTO> = transaction {
        ProfessorsTable
            .selectAll()
            .where { ProfessorsTable.speciality eq speciality }
            .map { it.toProfessorDTO() }
    }

    //  CREATE
    fun createProfessor(req: ProfessorWriteRequest): ProfessorDTO = transaction {
        val newId = ProfessorsTable.insertAndGetId { row ->
            row[ProfessorsTable.professorId] = req.professorId
            row[ProfessorsTable.firstName] = req.firstName
            row[ProfessorsTable.lastName] = req.lastName
            row[ProfessorsTable.email] = req.email
            row[ProfessorsTable.roomIds] = req.roomIds
            row[ProfessorsTable.phone] = req.phone
            row[ProfessorsTable.speciality] = req.speciality
            row[ProfessorsTable.fullName] = req.fullName
        }

        ProfessorsTable
            .selectAll()
            .where { ProfessorsTable.id eq newId }
            .single()
            .toProfessorDTO()
    }

    //  UPDATE
    fun updateProfessor(id: Int, req: ProfessorWriteRequest): ProfessorDTO? = transaction {
        val updated = ProfessorsTable.update({ ProfessorsTable.id eq id }) { row ->
            row[ProfessorsTable.professorId] = req.professorId
            row[ProfessorsTable.firstName] = req.firstName
            row[ProfessorsTable.lastName] = req.lastName
            row[ProfessorsTable.email] = req.email
            row[ProfessorsTable.roomIds] = req.roomIds
            row[ProfessorsTable.phone] = req.phone
            row[ProfessorsTable.speciality] = req.speciality
            row[ProfessorsTable.fullName] = req.fullName
        }

        if (updated == 0) return@transaction null

        ProfessorsTable
            .selectAll()
            .where { ProfessorsTable.id eq id }
            .singleOrNull()
            ?.toProfessorDTO()
    }

    //  DELETE
    fun deleteProfessor(id: Int): Boolean = transaction {
        ProfessorsTable.deleteWhere { ProfessorsTable.id eq id } > 0
    }
}

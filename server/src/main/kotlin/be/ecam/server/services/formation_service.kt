package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object FormationService {

    // 🔹 GET all formations
    fun getAllFormations(): List<FormationDTO> = transaction {
        FormationsTable
            .selectAll()
            .map { it.toFormationDTO() }
    }

    // 🔹 GET by DB id
    fun getFormationById(id: Int): FormationDTO? = transaction {
        FormationsTable
            .selectAll()
            .where { FormationsTable.id eq id }
            .singleOrNull()
            ?.toFormationDTO()
    }

    // 🔹 GET by logical formationId (ex: "3BE", "4IT")
    fun getFormationByFormationId(formationId: String): List<FormationDTO> = transaction {
        FormationsTable
            .selectAll()
            .where { FormationsTable.formationId eq formationId }
            .map { it.toFormationDTO() }
    }

    // 🔹 CREATE
    fun createFormation(req: FormationWriteRequest): FormationDTO = transaction {
        val newId = FormationsTable.insertAndGetId { row ->
            row[FormationsTable.formationId] = req.formationId
            row[FormationsTable.name] = req.name
            row[FormationsTable.sourceUrl] = req.sourceUrl
            row[FormationsTable.imageUrl] = req.imageUrl
        }

        FormationsTable
            .selectAll()
            .where { FormationsTable.id eq newId }
            .single()
            .toFormationDTO()
    }

    // 🔹 UPDATE
    fun updateFormation(id: Int, req: FormationWriteRequest): FormationDTO? = transaction {
        // ⚠️ ici il faut passer UN LAMBDA pour le where
        val updated = FormationsTable.update({ FormationsTable.id eq id }) { row ->
            row[FormationsTable.formationId] = req.formationId
            row[FormationsTable.name] = req.name
            row[FormationsTable.sourceUrl] = req.sourceUrl
            row[FormationsTable.imageUrl] = req.imageUrl
        }

        if (updated == 0) return@transaction null

        FormationsTable
            .selectAll()
            .where { FormationsTable.id eq id }
            .singleOrNull()
            ?.toFormationDTO()
    }

    // 🔹 DELETE
    fun deleteFormation(id: Int): Boolean = transaction {
        // ⚠️ En Exposed, on utilise deleteWhere sur une Table
        val deleted = FormationsTable.deleteWhere { FormationsTable.id eq id }
        deleted != 0
    }
}

package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object BlocService {

    //  Lire tous les blocs
    fun getAllBlocs(): List<BlocDTO> = transaction {
        BlocsTable
            .selectAll()
            .map { it.toBlocDTO() }
    }

    //  Lire un bloc par ID (clé primaire)
    fun getBlocById(id: Int): BlocDTO? = transaction {
        BlocsTable
            .selectAll()
            .where { BlocsTable.id eq id }
            .singleOrNull()
            ?.toBlocDTO()
    }

    //  Lire les blocs par blocId logique (ex: "B1")
    fun getBlocByBlocId(blocId: String): List<BlocDTO> = transaction {
        BlocsTable
            .selectAll()
            .where { BlocsTable.blocId eq blocId }
            .map { it.toBlocDTO() }
    }

    //  Créer un bloc
    fun createBloc(req: BlocWriteRequest): BlocDTO = transaction {
        val newId = BlocsTable.insertAndGetId { row ->
            row[blocId] = req.blocId
            row[name] = req.name
            row[formationIds] = req.formationIds
        }

        BlocsTable
            .selectAll()
            .where { BlocsTable.id eq newId }
            .single()
            .toBlocDTO()
    }

    //  Mettre à jour un bloc
    fun updateBloc(id: Int, req: BlocWriteRequest): BlocDTO? = transaction {
        val updatedCount = BlocsTable.update({ BlocsTable.id eq id }) { row ->
            row[blocId] = req.blocId
            row[name] = req.name
            row[formationIds] = req.formationIds
        }

        if (updatedCount == 0) return@transaction null

        BlocsTable
            .selectAll()
            .where { BlocsTable.id eq id }
            .singleOrNull()
            ?.toBlocDTO()
    }

    //  Supprimer un bloc
    fun deleteBloc(id: Int): Boolean = transaction {
        val deleted = BlocsTable.deleteWhere { BlocsTable.id eq id }
        deleted > 0
    }
}

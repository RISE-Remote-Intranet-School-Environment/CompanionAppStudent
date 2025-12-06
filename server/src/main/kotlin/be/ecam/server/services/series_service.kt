package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object SeriesNameService {

    // 🔹 GET all
    fun getAllSeries(): List<SeriesNameDTO> = transaction {
        SeriesNameTable
            .selectAll()
            .map { it.toSeriesNameDTO() }
    }

    // 🔹 GET by DB id
    fun getSeriesById(id: Int): SeriesNameDTO? = transaction {
        SeriesNameTable
            .selectAll()
            .where { SeriesNameTable.id eq id }
            .singleOrNull()
            ?.toSeriesNameDTO()
    }

    // 🔹 GET by logical id "S1", "G1", etc.
    fun getSeriesBySeriesId(seriesId: String): List<SeriesNameDTO> = transaction {
        SeriesNameTable
            .selectAll()
            .where { SeriesNameTable.seriesId eq seriesId }
            .map { it.toSeriesNameDTO() }
    }

    // 🔹 CREATE
    fun createSeries(req: SeriesNameWriteRequest): SeriesNameDTO = transaction {
        val newId = SeriesNameTable.insertAndGetId { row ->
            row[SeriesNameTable.seriesId] = req.seriesId
            row[SeriesNameTable.yearOptionIds] = req.yearOptionIds
            row[SeriesNameTable.formationIds] = req.formationIds
        }

        SeriesNameTable
            .selectAll()
            .where { SeriesNameTable.id eq newId }
            .single()
            .toSeriesNameDTO()
    }

    // 🔹 UPDATE
    fun updateSeries(id: Int, req: SeriesNameWriteRequest): SeriesNameDTO? = transaction {
        val updated = SeriesNameTable.update({ SeriesNameTable.id eq id }) { row ->
            row[SeriesNameTable.seriesId] = req.seriesId
            row[SeriesNameTable.yearOptionIds] = req.yearOptionIds
            row[SeriesNameTable.formationIds] = req.formationIds
        }

        if (updated == 0) return@transaction null

        SeriesNameTable
            .selectAll()
            .where { SeriesNameTable.id eq id }
            .singleOrNull()
            ?.toSeriesNameDTO()
    }

    // 🔹 DELETE
    fun deleteSeries(id: Int): Boolean = transaction {
        SeriesNameTable.deleteWhere { SeriesNameTable.id eq id } > 0
    }
}

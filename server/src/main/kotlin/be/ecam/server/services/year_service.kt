package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

// =====================
//  YEARS
// =====================
object YearService {

    // 🔹 GET all
    fun getAllYears(): List<YearDTO> = transaction {
        YearsTable
            .selectAll()
            .map { it.toYearDTO() }
    }

    // 🔹 GET by DB id
    fun getYearById(id: Int): YearDTO? = transaction {
        YearsTable
            .selectAll()
            .where { YearsTable.id eq id }
            .singleOrNull()
            ?.toYearDTO()
    }

    // 🔹 GET by logical id (ex: "Y1")
    fun getYearByYearId(yearId: String): List<YearDTO> = transaction {
        YearsTable
            .selectAll()
            .where { YearsTable.yearId eq yearId }
            .map { it.toYearDTO() }
    }

    // 🔹 CREATE
    fun createYear(req: YearWriteRequest): YearDTO = transaction {
        val newId = YearsTable.insertAndGetId { row ->
            row[YearsTable.yearId] = req.yearId
            row[YearsTable.yearNumber] = req.yearNumber
        }

        YearsTable
            .selectAll()
            .where { YearsTable.id eq newId }
            .single()
            .toYearDTO()
    }

    // 🔹 UPDATE
    fun updateYear(id: Int, req: YearWriteRequest): YearDTO? = transaction {
        val updated = YearsTable.update({ YearsTable.id eq id }) { row ->
            row[YearsTable.yearId] = req.yearId
            row[YearsTable.yearNumber] = req.yearNumber
        }

        if (updated == 0) return@transaction null

        YearsTable
            .selectAll()
            .where { YearsTable.id eq id }
            .singleOrNull()
            ?.toYearDTO()
    }

    // 🔹 DELETE
    fun deleteYear(id: Int): Boolean = transaction {
        YearsTable.deleteWhere { YearsTable.id eq id } > 0
    }
}

// =====================
//  YEAR OPTIONS
// =====================
object YearOptionService {

    // 🔹 GET all
    fun getAllYearOptions(): List<YearOptionDTO> = transaction {
        YearOptionsTable
            .selectAll()
            .map { it.toYearOptionDTO() }
    }

    // 🔹 GET by DB id
    fun getYearOptionById(id: Int): YearOptionDTO? = transaction {
        YearOptionsTable
            .selectAll()
            .where { YearOptionsTable.id eq id }
            .singleOrNull()
            ?.toYearOptionDTO()
    }

    // 🔹 GET by logical id (ex: "Y1-OPT1")
    fun getYearOptionsByYearOptionId(yearOptionId: String): List<YearOptionDTO> = transaction {
        YearOptionsTable
            .selectAll()
            .where { YearOptionsTable.yearOptionId eq yearOptionId }
            .map { it.toYearOptionDTO() }
    }

    // 🔹 GET by blocId
    fun getYearOptionsByBlocId(blocId: Int): List<YearOptionDTO> = transaction {
        YearOptionsTable
            .selectAll()
            .where { YearOptionsTable.blocId eq blocId }
            .map { it.toYearOptionDTO() }
    }

    // 🔹 CREATE
    fun createYearOption(req: YearOptionWriteRequest): YearOptionDTO = transaction {
        val newId = YearOptionsTable.insertAndGetId { row ->
            row[YearOptionsTable.yearOptionId] = req.yearOptionId
            row[YearOptionsTable.formationIds] = req.formationIds
            row[YearOptionsTable.blocId] = req.blocId
        }

        YearOptionsTable
            .selectAll()
            .where { YearOptionsTable.id eq newId }
            .single()
            .toYearOptionDTO()
    }

    // 🔹 UPDATE
    fun updateYearOption(id: Int, req: YearOptionWriteRequest): YearOptionDTO? = transaction {
        val updated = YearOptionsTable.update({ YearOptionsTable.id eq id }) { row ->
            row[YearOptionsTable.yearOptionId] = req.yearOptionId
            row[YearOptionsTable.formationIds] = req.formationIds
            row[YearOptionsTable.blocId] = req.blocId
        }

        if (updated == 0) return@transaction null

        YearOptionsTable
            .selectAll()
            .where { YearOptionsTable.id eq id }
            .singleOrNull()
            ?.toYearOptionDTO()
    }

    // 🔹 DELETE
    fun deleteYearOption(id: Int): Boolean = transaction {
        YearOptionsTable.deleteWhere { YearOptionsTable.id eq id } > 0
    }
}

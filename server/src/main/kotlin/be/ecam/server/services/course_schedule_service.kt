package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CourseScheduleService {

    // 🔹 GET all schedules
    fun getAllSchedules(): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .map { it.toCourseScheduleDTO() }
    }

    // 🔹 GET by DB id
    fun getScheduleById(id: Int): CourseScheduleDTO? = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.id eq id }
            .singleOrNull()
            ?.toCourseScheduleDTO()
    }

    // 🔹 GET by week number
    fun getSchedulesByWeek(week: Int): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.week eq week }
            .map { it.toCourseScheduleDTO() }
    }

    // 🔹 GET by date (ex: "2025-03-14")
    fun getSchedulesByDate(date: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.date eq date }
            .map { it.toCourseScheduleDTO() }
    }

    // 🔹 GET by year option (ex: "Y4INFOSEC")
    fun getSchedulesByYearOption(yearOptionId: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.yearOptionId eq yearOptionId }
            .map { it.toCourseScheduleDTO() }
    }

    // 🔹 GET by group number (ex: "G1", "A", "B")
    fun getSchedulesByGroup(groupNo: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.groupNo eq groupNo }
            .map { it.toCourseScheduleDTO() }
    }

    // 🔹 GET by course raccourci id (ex: "DB", "SOC")
    fun getSchedulesByRaccourci(courseRaccourciId: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.courseRaccourciId eq courseRaccourciId }
            .map { it.toCourseScheduleDTO() }
    }

    // 🔹 CREATE
    fun createSchedule(req: CourseScheduleWriteRequest): CourseScheduleDTO = transaction {
        val newId = CourseScheduleTable.insertAndGetId { row ->
            row[CourseScheduleTable.week] = req.week
            row[CourseScheduleTable.yearOptionId] = req.yearOptionId
            row[CourseScheduleTable.groupNo] = req.groupNo
            row[CourseScheduleTable.seriesJson] = req.seriesJson
            row[CourseScheduleTable.date] = req.date
            row[CourseScheduleTable.dayName] = req.dayName
            row[CourseScheduleTable.startTime] = req.startTime
            row[CourseScheduleTable.endTime] = req.endTime
            row[CourseScheduleTable.courseRaccourciId] = req.courseRaccourciId
            row[CourseScheduleTable.title] = req.title
            row[CourseScheduleTable.teachersJson] = req.teachersJson
            row[CourseScheduleTable.roomIds] = req.roomIds
            row[CourseScheduleTable.sousCourseId] = req.sousCourseId
        }

        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.id eq newId }
            .single()
            .toCourseScheduleDTO()
    }

    // 🔹 UPDATE
    fun updateSchedule(id: Int, req: CourseScheduleWriteRequest): CourseScheduleDTO? = transaction {
        val updated = CourseScheduleTable.update({ CourseScheduleTable.id eq id }) { row ->
            row[CourseScheduleTable.week] = req.week
            // 🔧 ici j’ai corrigé `req.year.yearOptionId` → `req.yearOptionId`
            row[CourseScheduleTable.yearOptionId] = req.yearOptionId
            row[CourseScheduleTable.groupNo] = req.groupNo
            row[CourseScheduleTable.seriesJson] = req.seriesJson
            row[CourseScheduleTable.date] = req.date
            row[CourseScheduleTable.dayName] = req.dayName
            row[CourseScheduleTable.startTime] = req.startTime
            row[CourseScheduleTable.endTime] = req.endTime
            row[CourseScheduleTable.courseRaccourciId] = req.courseRaccourciId
            row[CourseScheduleTable.title] = req.title
            row[CourseScheduleTable.teachersJson] = req.teachersJson
            row[CourseScheduleTable.roomIds] = req.roomIds
            row[CourseScheduleTable.sousCourseId] = req.sousCourseId
        }

        if (updated == 0) return@transaction null

        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.id eq id }
            .singleOrNull()
            ?.toCourseScheduleDTO()
    }

    // 🔹 DELETE
    fun deleteSchedule(id: Int): Boolean = transaction {
        CourseScheduleTable.deleteWhere { CourseScheduleTable.id eq id } > 0
    }
}

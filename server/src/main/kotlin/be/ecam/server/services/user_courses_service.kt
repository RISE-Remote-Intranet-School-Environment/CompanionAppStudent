package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object UserCoursesService {

    fun getCoursesForUser(userId: Int): List<String> = transaction {
        UserCoursesTable
            .selectAll()
            .where { UserCoursesTable.userId eq userId }
            .map { it[UserCoursesTable.courseId] }
    }

    fun addCourse(userId: Int, courseId: String): Boolean = transaction {
        val exists = UserCoursesTable
            .selectAll()
            .where { (UserCoursesTable.userId eq userId) and (UserCoursesTable.courseId eq courseId.lowercase()) }
            .count() > 0

        if (exists) return@transaction false

        UserCoursesTable.insert {
            it[UserCoursesTable.userId] = userId
            it[UserCoursesTable.courseId] = courseId.lowercase()
            it[addedAt] = System.currentTimeMillis()
        }
        true
    }

    fun addCourses(userId: Int, courseIds: List<String>): Int = transaction {
        var added = 0
        courseIds.forEach { courseId ->
            val exists = UserCoursesTable
                .selectAll()
                .where { (UserCoursesTable.userId eq userId) and (UserCoursesTable.courseId eq courseId.lowercase()) }
                .count() > 0

            if (!exists) {
                UserCoursesTable.insert {
                    it[UserCoursesTable.userId] = userId
                    it[UserCoursesTable.courseId] = courseId.lowercase()
                    it[addedAt] = System.currentTimeMillis()
                }
                added++
            }
        }
        added
    }

    fun removeCourse(userId: Int, courseId: String): Boolean = transaction {
        UserCoursesTable.deleteWhere {
            (UserCoursesTable.userId eq userId) and (UserCoursesTable.courseId eq courseId.lowercase())
        } > 0
    }

    fun clearCourses(userId: Int): Int = transaction {
        UserCoursesTable.deleteWhere { UserCoursesTable.userId eq userId }
    }

    fun setCourses(userId: Int, courseIds: List<String>): Int = transaction {
        UserCoursesTable.deleteWhere { UserCoursesTable.userId eq userId }
        
        courseIds.distinct().forEach { courseId ->
            UserCoursesTable.insert {
                it[UserCoursesTable.userId] = userId
                it[UserCoursesTable.courseId] = courseId.lowercase()
                it[addedAt] = System.currentTimeMillis()
            }
        }
        courseIds.size
    }
}
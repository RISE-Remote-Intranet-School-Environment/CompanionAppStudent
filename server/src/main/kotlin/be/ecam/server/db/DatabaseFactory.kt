package be.ecam.server.db

import at.favre.lib.crypto.bcrypt.BCrypt
import be.ecam.server.models.*
import be.ecam.server.services.guessCourseIcon
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File

object DatabaseFactory {

    fun connect() {
        // Dossier data
        val dbFolder = File("data")
        if (!dbFolder.exists()) {
            dbFolder.mkdirs()
            println("Created DB folder: ${dbFolder.absolutePath}")
        }

        val dbFile = File(dbFolder, "app.db")
        val url = "jdbc:sqlite:${dbFile.absolutePath}"

        Database.connect(url, driver = "org.sqlite.JDBC")
        println("SQLite DB = $url")

        transaction {
            //  CREATE TABLES IF NOT EXISTS
            SchemaUtils.create(
                UsersTable,
                StudentsTable,
                ProfessorsTable,

                FormationsTable,
                BlocsTable,
                YearsTable,
                YearOptionsTable,
                SeriesNameTable,

                CoursesTable,
                CourseDetailsTable,
                CourseEvaluationTable,
                SousCoursesTable,

                CalendarEventsTable,
                CourseScheduleTable,

                RoomsTable,

                PaeStudentsTable,
                NotesStudentsTable,
                StudentSubmissionsTable,
                CourseResourcesTable, 

                RefreshTokensTable
            )

            println(
                "Schema synced: " +
                    "Users, Students, Professors, " +
                    "Formations, Blocs, Years, YearOptions, SeriesName, " +
                    "Courses, CourseDetails, CourseEvaluation, SousCourses, " +
                    "CalendarEvents, CourseSchedule, Rooms, " +
                    "PaeStudents, NotesStudents, StudentSubmissions, CourseResources"
            )

            val iconReady = ensureCoursesIconColumn()
            if (iconReady) {
                backfillCourseIcons()
            } else {
                println("courses.icon column still missing; skipping backfill")
            }

            // Seed initial data
            //DatabaseSeeder.seedAll()

            //  DEFAULT ADMIN USER  
            
            val userCount = UsersTable.selectAll().count()
            if (userCount == 0L) {
                val hashedPassword = BCrypt
                    .withDefaults()
                    .hashToString(12, "1234".toCharArray())

                UsersTable.insert { row ->
                    row[username] = "admin"
                    row[email] = "admin@example.com"
                    row[passwordHash] = hashedPassword
                    row[firstName] = "Admin"
                    row[lastName] = "ECAM"
                    row[role] = UserRole.ADMIN       
                    row[avatarUrl] = null
                    row[professorId] = null
                    row[studentId] = null
                }

                println("Default admin user created (email=admin@example.com, pwd=1234)")
            }
            
        }
    }

    private fun ensureCoursesIconColumn(): Boolean {
        val hasIcon = TransactionManager.current().exec("PRAGMA table_info(courses)") { rs ->
            generateSequence {
                if (rs.next()) rs.getString("name") else null
            }.any { it.equals("icon", ignoreCase = true) }
        } ?: false

        if (!hasIcon) {
            return try {
                TransactionManager.current().exec("ALTER TABLE courses ADD COLUMN icon TEXT")
                println("Added icon column to courses")
                true
            } catch (t: Throwable) {
                println("Failed to add icon column: ${t.message}")
                false
            }
        }
        return true
    }

    private fun backfillCourseIcons() {
        val missing = CoursesTable
            .selectAll()
            .map { Triple(it[CoursesTable.id].value, it[CoursesTable.title], it[CoursesTable.icon]) }
            .filter { it.third.isNullOrBlank() }

        missing.forEach { (id, title, _) ->
            val icon = guessCourseIcon(title)
            CoursesTable.update({ CoursesTable.id eq id }) {
                it[CoursesTable.icon] = icon
            }
        }

        if (missing.isNotEmpty()) println("Backfilled ${missing.size} course icons")
    }
}

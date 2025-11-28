package be.ecam.server.db

import be.ecam.server.models.AdminTable
import be.ecam.server.models.FormationTable
import be.ecam.server.models.BlockTable
import be.ecam.server.models.CourseTable
import be.ecam.server.models.CalendarEventsTable
import be.ecam.server.models.CourseScheduleTable
import be.ecam.server.models.CourseDetailsTable
import be.ecam.server.models.ProfessorsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import be.ecam.server.models.Admin
import org.jetbrains.exposed.sql.selectAll
import at.favre.lib.crypto.bcrypt.BCrypt


object DatabaseFactory {

    fun connect() {
        // we force the creation of a "data" folder in the working directory
        val dbFolder = File("data")
        if (!dbFolder.exists()) {
            dbFolder.mkdirs()
            println(" Created DB folder: ${dbFolder.absolutePath}")
        }

        val dbFile = File(dbFolder, "app.db")
        val url = "jdbc:sqlite:${dbFile.absolutePath}"

        Database.connect(url, driver = "org.sqlite.JDBC")
        println("SQLite DB = $url")

        // Create tables if not exists
        transaction {
            SchemaUtils.create(
                AdminTable,
                FormationTable,
                BlockTable,
                CourseTable,
                CalendarEventsTable,
                CourseScheduleTable,
                CourseDetailsTable,
                ProfessorsTable
            )
            println("Schema synced (Admin, Formation, Block, Course, CalendarEvents, CourseSchedule, CourseDetails, Professors tables)")


        // autocreation admin 
            val adminCount = AdminTable.selectAll().count()
            if (adminCount == 0L) {
                val a = Admin.new {
                    username = "admin"
                    email = "admin@example.com"
                    password = BCrypt.withDefaults().hashToString(12, "1234".toCharArray())
                }
                println("Default admin created with id=${a.id.value}")
        }
    }
}

}

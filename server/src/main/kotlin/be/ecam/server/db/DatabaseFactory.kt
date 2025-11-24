package be.ecam.server.db

import be.ecam.server.models.AdminTable
import be.ecam.server.models.FormationTable
import be.ecam.server.models.BlockTable
import be.ecam.server.models.CourseTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {

    fun connect() {
        // On force la DB dans data/app.db
        val dbFolder = File("data")
        if (!dbFolder.exists()) {
            dbFolder.mkdirs()
            println(" Created DB folder: ${dbFolder.absolutePath}")
        }

        val dbFile = File(dbFolder, "app.db")
        val url = "jdbc:sqlite:${dbFile.absolutePath}"

        Database.connect(url, driver = "org.sqlite.JDBC")
        println("SQLite DB = $url")

        // Création / mise à jour des tables
        transaction {
            SchemaUtils.create(
                AdminTable,
                FormationTable,
                BlockTable,
                CourseTable
            )
            println("Schema synced (Admin, Formation, Block, Course)")
        }
    }
}

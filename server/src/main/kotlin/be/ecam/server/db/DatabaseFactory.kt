package be.ecam.server.db
import be.ecam.server.models.AdminTable // import the AdminTable schema
import org.jetbrains.exposed.sql.Database // import executed SQL statements to the console
import org.jetbrains.exposed.sql.SchemaUtils // import for schema management
import org.jetbrains.exposed.sql.transactions.TransactionManager // import for managing transactions
import org.jetbrains.exposed.sql.transactions.transaction // import for transaction blocks
// import other tables as needed
import be.ecam.server.models.FormationTable
import be.ecam.server.models.BlockTable
import be.ecam.server.models.CourseTable
import be.ecam.server.models.CalendarEventsTable

import java.io.File
import java.sql.Connection 

object DatabaseFactory {

    // Connect to the db SQLlite 
    fun connect(url: String) {
        // If it's a file-based SQLite DB, ensure the directory exists
        if (url.startsWith("jdbc:sqlite:")) {
            val rawPath = url.removePrefix("jdbc:sqlite:")
            File(rawPath).parentFile?.mkdirs()
        }

        // Establish the db connection
        Database.connect(
            url = url,
            driver = "org.sqlite.JDBC"
        )
        // debug
        println("Exposed SQL logging enabled")

        
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        println("Connected to DB : $url")
    }

    // Migrate the schema (create/update tables)
    fun migrate() {
        transaction {
            SchemaUtils.create(
                AdminTable, // create the admin table
                FormationTable,
                BlockTable,
                CourseTable,
                CalendarEventsTable
            )  
        }
        println("Schema up-to-date (admins, formations, blocks, courses, calendar events).")
    }

}

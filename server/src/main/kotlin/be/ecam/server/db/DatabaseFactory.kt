package be.ecam.server.db
import be.ecam.server.models.AdminTable // import the AdminTable schema
import org.jetbrains.exposed.sql.Database // import executed SQL statements to the console
import org.jetbrains.exposed.sql.SchemaUtils // import for schema management
import org.jetbrains.exposed.sql.transactions.TransactionManager // import for managing transactions
import org.jetbrains.exposed.sql.transactions.transaction // import for transaction blocks

import java.io.File
import java.sql.Connection 

object DatabaseFactory {

    // Connect to the db
    fun connect(url: String, user: String?, password: String?) {
        val realUrl = url  // resolveUrl

        // Ensure directory exists for SQLite files
        if (realUrl.startsWith("jdbc:sqlite:")) {
            val rawPath = realUrl.removePrefix("jdbc:sqlite:")
            File(rawPath).parentFile?.mkdirs()
        }

        // Establish the db connection
        Database.connect(
            url = realUrl,
            driver = "org.sqlite.JDBC",
            user = user ?: "",
            password = password ?: ""
        )

        
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        println("Connected to DB : $realUrl")
    }

    // Migrate the schema (create/update tables)
    fun migrate() {
        transaction {
            SchemaUtils.create(AdminTable)  // initialize Admin table
        }
        println("Schema up-to-date")
    }
}

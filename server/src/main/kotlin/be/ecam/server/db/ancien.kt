// package be.ecam.server.db

// import at.favre.lib.crypto.bcrypt.BCrypt
// import be.ecam.server.models.*
// import org.jetbrains.exposed.sql.Database
// import org.jetbrains.exposed.sql.SchemaUtils
// import org.jetbrains.exposed.sql.insert
// import org.jetbrains.exposed.sql.selectAll
// import org.jetbrains.exposed.sql.transactions.transaction
// import java.io.File

// object DatabaseFactory {

//     fun connect() {
//         // we force the creation of a "data" folder in the working directory
//         val dbFolder = File("data")
//         if (!dbFolder.exists()) {
//             dbFolder.mkdirs()
//             println(" Created DB folder: ${dbFolder.absolutePath}")
//         }

//         val dbFile = File(dbFolder, "app.db")
//         val url = "jdbc:sqlite:${dbFile.absolutePath}"

//         Database.connect(url, driver = "org.sqlite.JDBC")
//         println("SQLite DB = $url")

//         transaction {
//             // Create tables if not exists
//             SchemaUtils.create(
//                 UsersTable,
//                 FormationTable,
//                 BlockTable,
//                 CourseTable,
//                 CalendarEventsTable,
//                 CourseScheduleTable,
//                 CourseDetailsTable,
//                 ProfessorsTable
//             )
//             println(
//                 "Schema synced (Users, Formation, Block, Course, " +
//                     "CalendarEvents, CourseSchedule, CourseDetails, Professors tables)"
//             )

//             // --- autocreation admin user ---
//             val userCount = UsersTable.selectAll().count()
//             if (userCount == 0L) {
//                 val hashedPassword = BCrypt
//                     .withDefaults()
//                     .hashToString(12, "1234".toCharArray())

//                 UsersTable.insert { row ->
//                     row[UsersTable.username] = "admin"
//                     row[UsersTable.email] = "admin@example.com"
//                     row[UsersTable.passwordHash] = hashedPassword
//                     row[UsersTable.firstName] = "Admin"
//                     row[UsersTable.lastName] = "ECAM"
//                     row[UsersTable.role] = UserRole.ADMIN.name
//                     row[UsersTable.avatarUrl] = null
//                 }

//                 println("Default admin user created with email=admin@example.com and username=admin (pwd=1234)")
//             }
//         }
//     }
// }

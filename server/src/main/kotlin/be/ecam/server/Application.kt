package be.ecam.server

import be.ecam.common.Greeting
import be.ecam.common.api.HelloResponse
import be.ecam.common.api.ScheduleItem
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
// added imports for new routes
import io.ktor.server.routing.post
// added import for authroutes
import be.ecam.server.routes.authRoutes
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.io.File
import java.nio.file.Paths



fun main(args: Array<String>) {
    // Start Ktor with configuration from application.conf (HTTP)
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) { json() }

    // --- New: lire la config et connecter la DB au démarrage ---
    val config = environment.config
    val dbUrl = config.property("db.url").getString()
    val dbUser = config.propertyOrNull("db.user")?.getString()
    val dbPass = config.propertyOrNull("db.password")?.getString()
    be.ecam.server.db.DatabaseFactory.connect(dbUrl, dbUser, dbPass)
    be.ecam.server.db.DatabaseFactory.migrate()
    // ---

    


    // Serve static WASM app if present and expose simple API
    routing {
        // Robust resolver for the WASM app output directory (Option A)
        fun wasmCandidates(): List<File> {
            val override = System.getProperty("wasm.dir")?.trim().takeUnless { it.isNullOrEmpty() }
                ?: System.getenv("WASM_DIR")?.trim().takeUnless { it.isNullOrEmpty() }

            val paths = mutableListOf<String>()
            if (override != null) paths += override

            // Project-root relative (works if working dir is repo root)
            paths += listOf(
                "composeApp/build/dist/wasmJs/productionExecutable",
                "composeApp/build/dist/wasmJs/developmentExecutable"
            )
            // Server-module relative (works if working dir is repoRoot/server)
            paths += listOf(
                "../composeApp/build/dist/wasmJs/productionExecutable",
                "../composeApp/build/dist/wasmJs/developmentExecutable"
            )

            return paths.map { Paths.get(it).toFile() }
        }

        val wasmOut = wasmCandidates().firstOrNull { it.exists() }
        println(
            "Working dir: ${System.getProperty("user.dir")} | Using WASM dir: ${wasmOut?.absolutePath ?: "<not found>"}"
        )

        if (wasmOut != null) {
            staticFiles("/", wasmOut, index = "index.html")
        }

        get("/favicon.png") {
            respondFavIcon()
        }
        get("/favicon.ico") {
            respondFavIcon()
        }

        // API endpoints kept under /api to avoid collision with index.html routing
        // Serve index.html at root when WASM output exists; otherwise fall back to legacy greeting
        if (wasmOut == null) {
            get("/") {
                call.respondText("Ktor: ${Greeting().greet()}")
            }
        }

        route("/api") {
            get("/") {
                call.respondText("Ktor: ${Greeting().greet()}")
            }
            get("/hello") {
                call.respond(HelloResponse(message = "Hello from Ktor server"))
            }

            // --- road debug db access ---
            get("/debug/admins/count") {
                val n = org.jetbrains.exposed.sql.transactions.transaction {
                    be.ecam.server.models.Admin.all().count()
                }
                call.respondText("Admins count: $n")
            }

            post("/debug/admins/seed") {
                val id = org.jetbrains.exposed.sql.transactions.transaction {
                    val a = be.ecam.server.models.Admin.new {
                        username = "admin"
                        email = "admin@example.com"
                        password = "1234" // just for debug
                    }
                    a.id.value
                }
                call.respondText("Seeded admin with ID: $id")
            }

            // -------------------------------------------------

            get("/schedule") {
                // Generate example items for multiple dates until end of 2025
                val schedule = mutableMapOf<String, List<ScheduleItem>>()

                // A couple of fixed examples around current timeframe
                schedule["2025-09-30"] = listOf(ScheduleItem("Team sync"), ScheduleItem("Release planning"))
                schedule["2025-10-01"] = listOf(ScheduleItem("Code review"))

                // Add examples for each remaining month of 2025
                for (month in 1..12) {
                    val first = java.time.LocalDate.of(2025, month, 1)
                    val mid = first.withDayOfMonth(minOf(15, first.lengthOfMonth()))
                    val last = first.withDayOfMonth(first.lengthOfMonth())
                    schedule.putIfAbsent(first.toString(), listOf(ScheduleItem("Kickoff ${first.month.name.lowercase().replaceFirstChar { it.titlecase() }}")))
                    schedule.putIfAbsent(mid.toString(), listOf(ScheduleItem("Mid-month check"), ScheduleItem("Demo prep")))
                    schedule.putIfAbsent(last.toString(), listOf(ScheduleItem("Retrospective")))
                }

                // Also sprinkle weekly examples on Mondays in Q4 2025
                var d = java.time.LocalDate.of(2025, 10, 1)
                while (!d.isAfter(java.time.LocalDate.of(2025, 12, 31))) {
                    if (d.dayOfWeek == java.time.DayOfWeek.MONDAY) {
                        schedule.putIfAbsent(d.toString(), listOf(ScheduleItem("Weekly planning")))
                    }
                    d = d.plusDays(1)
                }

                call.respond(schedule)
            }
        }
    }
}
suspend fun RoutingContext.respondFavIcon() {
    val bytes = this::class.java.classLoader.getResourceAsStream("favicon.png")?.readBytes()
    if (bytes == null) {
        call.respondText("Not found", status = HttpStatusCode.NotFound)
    } else {
        call.respondBytes(bytes, contentType = ContentType.Image.PNG)
    }

     
} // elle manquait la fermeture


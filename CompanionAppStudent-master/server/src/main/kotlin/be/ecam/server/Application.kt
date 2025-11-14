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
}

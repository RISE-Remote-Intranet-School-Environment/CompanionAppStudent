package be.ecam.server

import be.ecam.common.Greeting
import be.ecam.common.api.HelloResponse
import be.ecam.common.api.ScheduleItem
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.default
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post

// added import for authroutes
import be.ecam.server.routes.authRoutes
// added import for adminroutes
import be.ecam.server.routes.adminRoutes
// added import for calendarroutes
import be.ecam.server.routes.calendarRoutes
// added import for catalogroutes
import be.ecam.server.routes.catalogRoutes
// added import for professorroutes
import be.ecam.server.routes.professorRoutes

// add callloging import
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import org.slf4j.event.Level

import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.routing.options

import java.io.File
import java.nio.file.Paths

// import JWT auth
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import be.ecam.server.security.JwtConfig
import be.ecam.server.security.JwtService



fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {

    install(CallLogging) {
        level = Level.INFO
        filter { call -> true }
    }

    install(ContentNegotiation) { json() }

    // Allow the web app (served from the same host) to call the API with JSON/Authorization headers.
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }

    // install JWT authentication
    install(Authentication) {
        jwt("jwt") {
            realm = JwtConfig.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(JwtConfig.secret))
                    .withIssuer(JwtConfig.issuer)
                    .withAudience(JwtConfig.audience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("id").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }

    // connect to the database

    be.ecam.server.db.DatabaseFactory.connect()   

    // seed the formations from JSON automatically (idempotent, does not duplicate courses)
    be.ecam.server.services.CatalogService.seedFormationsFromJson()

    // seed the calendar events from JSON automatically (idempotent, does not duplicate events)
    be.ecam.server.services.CalendarService.seedCalendarEventsFromJson()

    // seed the course schedule from JSON automatically (idempotent, does not duplicate schedule entries)
    be.ecam.server.services.CalendarService.seedCourseScheduleFromJson()

    // seed the course details from JSON automatically (idempotent, does not duplicate details)
    be.ecam.server.services.CatalogService.seedCourseDetailsFromJson()

    // seed the professors from JSON automatically (idempotent, does not duplicate professors)
    //be.ecam.server.services.ProfessorService.seedProfessorsFromJson()

    // ----------------------------------------------------------

    routing {

        fun wasmCandidates(): List<File> {
            val override = System.getProperty("wasm.dir")?.trim().takeUnless { it.isNullOrEmpty() }
                ?: System.getenv("WASM_DIR")?.trim().takeUnless { it.isNullOrEmpty() }

            val paths = mutableListOf<String>()
            if (override != null) paths += override

            paths += listOf(
                "composeApp/build/dist/wasmJs/productionExecutable",
                "composeApp/build/dist/wasmJs/developmentExecutable"
            )
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

        get("/favicon.png") {
            respondFavIcon()
        }
        get("/favicon.ico") {
            respondFavIcon()
        }

        if (wasmOut == null) {
            get("/") {
                call.respondText("Ktor: ${Greeting().greet()}")
            }
        } else {
            staticFiles("/", wasmOut) {
                default("index.html")
            }
        }

        route("/api") {
            // Respond to preflight requests so the browser will proceed with POST/GET.
            options("{...}") {
                call.respond(HttpStatusCode.OK)
            }

            get("/") {
                call.respondText("Ktor: ${Greeting().greet()}")
            }
            get("/hello") {
                call.respond(HelloResponse(message = "Hello from Ktor server"))
            }

            // --- road debug db access ---
            // we 
            // get("/debug/admins/count") {
            //     val n = org.jetbrains.exposed.sql.transactions.transaction {
            //         be.ecam.server.models.Admin.all().count()
            //     }
            //     call.respondText("Admins count: $n")
            // }

            post("/debug/admins/seed") {
                val id = org.jetbrains.exposed.sql.transactions.transaction {
                    val a = be.ecam.server.models.Admin.new {
                        username = "admin"
                        email = "admin@example.com"
                        password = "1234"
                    }
                    a.id.value
                }
                call.respondText("Seeded admin with ID: $id")
            }

            // Auth routes (register/login)
            authRoutes()

            // catalog routes (formations, blocks, courses)
            catalogRoutes()

            // calendar routes (events)
            calendarRoutes()

            // professor routes (professors)
            professorRoutes()

            // route protected with JWT
            authenticate("jwt") {
                adminRoutes()
            }

            get("/schedule") {
                val schedule = mutableMapOf<String, List<ScheduleItem>>()

                schedule["2025-09-30"] = listOf(ScheduleItem("Team sync"), ScheduleItem("Release planning"))
                schedule["2025-10-01"] = listOf(ScheduleItem("Code review"))

                for (month in 1..12) {
                    val first = java.time.LocalDate.of(2025, month, 1)
                    val mid = first.withDayOfMonth(minOf(15, first.lengthOfMonth()))
                    val last = first.withDayOfMonth(first.lengthOfMonth())
                    schedule.putIfAbsent(first.toString(), listOf(ScheduleItem("Kickoff ${first.month.name.lowercase().replaceFirstChar { it.titlecase() }}")))
                    schedule.putIfAbsent(mid.toString(), listOf(ScheduleItem("Mid-month check"), ScheduleItem("Demo prep")))
                    schedule.putIfAbsent(last.toString(), listOf(ScheduleItem("Retrospective")))
                }

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

package be.ecam.server

import be.ecam.server.db.DatabaseFactory
import be.ecam.server.routes.*
import be.ecam.server.security.JwtConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.event.Level
import io.ktor.server.plugins.autohead.*
import io.ktor.server.http.content.*



fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {

    // logging
    install(CallLogging) {
        level = Level.INFO
        filter { true }
    }

    install(AutoHeadResponse)

    // json serialization
    install(ContentNegotiation) {
        json()
    }

    // cors
    install(CORS) {
        // Dev local
        allowHost("localhost:8080")
        allowHost("127.0.0.1:8080")
        allowHost("localhost:28088")
        allowHost("127.0.0.1:28088")
        
        // Production
        allowHost("clacoxygen.msrl.be", schemes = listOf("https"))
        
        // Headers et méthodes
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-User-Id")
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Head) 
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowCredentials = true
    }

    // jwt authentication
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm

            verifier(
                JWT
                    .require(Algorithm.HMAC256(JwtConfig.secret))
                    .withIssuer(JwtConfig.issuer)
                    .withAudience(JwtConfig.audience)
                    .build()
            )

            validate { cred ->
                val id = cred.payload.getClaim("id").asInt()
                val role = cred.payload.getClaim("role").asString()

                if (id != null && !role.isNullOrBlank()) {
                    JWTPrincipal(cred.payload)
                } else {
                    null
                }
            }
        }
    }

    // Database connection
    DatabaseFactory.connect()

    // Seeds - Décommenter pour peupler la base
    // CatalogService.seedFormationsFromJson()
    CalendarService.seedCourseScheduleFromJson()  // <-- Activer ce seed
    // CatalogService.seedCourseDetailsFromJson()
    // ProfessorService.seedProfessorsFromJson()

    // routes
    routing {

        // Servir les fichiers statiques (comme auth-callback.html)
        staticResources("/", "static")

        // Routes publiques simples
        get("/") {
            call.respondText(
                "Backend CompanionAppStudent is running.",
                ContentType.Text.Plain
            )
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, "OK")
        }

        // routes API
        route("/api") {

            // Routes publiques
            get("/hello") {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Hello from Companion backend"))
            }
            get("/schedule") {
                call.respond(HttpStatusCode.OK, emptyMap<String, List<String>>())
            }
            imageProxyRoutes()
            authRoutes()
            microsoftAuthRoutes()

            //  Routes protégées (JWT) 
            authenticate("auth-jwt") {

                formationRoutes()
                blocRoutes()
                yearRoutes()
                yearOptionRoutes()
                seriesNameRoutes()
                roomRoutes()
                courseRoutes()
                courseDetailsRoutes()
                courseEvaluationRoutes()
                courseScheduleRoutes()
                sousCourseRoutes()
                calendarRoutes()
                notesStudentRoutes()
                professorRoutes()
                studentRoutes()
                userRoutes()
                paeStudentRoutes()
                studentSubmissionsRoutes()
                courseResourcesRoutes()
            }
        }
    }
}

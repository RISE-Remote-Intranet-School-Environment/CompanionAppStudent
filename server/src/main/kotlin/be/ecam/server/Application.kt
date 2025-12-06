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

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {

    // ====================
    //       LOGGING
    // ====================
    install(CallLogging) {
        level = Level.INFO
        filter { true }
    }

    // ====================
    //        JSON
    // ====================
    install(ContentNegotiation) {
        json()
    }

    // ====================
    //        CORS
    // ====================
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    // ====================
    //     AUTH / JWT
    // ====================
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

    // ====================
    //    DATABASE INIT
    // ====================
    DatabaseFactory.connect()

    // (optionnel) Seeds désactivés pour l’instant
    // CatalogService.seedFormationsFromJson()
    // CalendarService.seedCalendarEventsFromJson()
    // CalendarService.seedCourseScheduleFromJson()
    // CatalogService.seedCourseDetailsFromJson()
    // ProfessorService.seedProfessorsFromJson()

    // ====================
    //        ROUTING
    // ====================
    routing {

        // --- Routes publiques simples ---
        get("/") {
            call.respondText(
                "Backend CompanionAppStudent is running.",
                ContentType.Text.Plain
            )
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, "OK")
        }

        // ================================
        //           API / AUTH
        // ================================
        route("/api") {

            // --- Auth : public (register + login) ---
            authRoutes()
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
            yearOptionRoutes()

            

            // --- Tout le reste : protégé JWT ---
            authenticate("auth-jwt") {

                // Users + profils
                userRoutes()
                studentRoutes()
                professorRoutes()
                paeStudentRoutes()
                notesStudentRoutes()

                // Catalogue ECAM
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

                // Si tu as aussi des routes calendrier / annonces :
                calendarRoutes()
                //announcementsRoutes()
            }
        }
    }
}

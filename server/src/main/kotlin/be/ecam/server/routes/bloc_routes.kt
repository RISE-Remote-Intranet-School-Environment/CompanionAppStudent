package be.ecam.server.routes

import be.ecam.server.models.BlocWriteRequest
import be.ecam.server.services.BlocService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.blocRoutes() {

    route("/blocs") {

       
        get {
            call.respond(BlocService.getAllBlocs())
        }

        
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val bloc = BlocService.getBlocById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Bloc not found")

            call.respond(bloc)
        }

        
        get("by-code/{blocId}") {
            val blocId = call.parameters["blocId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "blocId missing")

            call.respond(BlocService.getBlocByBlocId(blocId))
        }

        
        post {
            val req = call.receive<BlocWriteRequest>()
            val created = BlocService.createBloc(req)
            call.respond(HttpStatusCode.Created, created)
        }

        
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<BlocWriteRequest>()
            val updated = BlocService.updateBloc(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Bloc not found")

            call.respond(updated)
        }

        
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = BlocService.deleteBloc(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Bloc not found")
        }
    }
}

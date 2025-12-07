package be.ecam.server.routes

import be.ecam.server.models.NotesStudentWriteRequest
import be.ecam.server.services.NotesStudentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.notesStudentRoutes() {

    route("/notes-students") {

        
        get {
            call.respond(NotesStudentService.getAllNotes())
        }

      
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val note = NotesStudentService.getNoteById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Note not found")

            call.respond(note)
        }

        
        get("by-student/{studentId}") {
            val studentId = call.parameters["studentId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid studentId")

            call.respond(NotesStudentService.getNotesByStudentId(studentId))
        }

       
        get("by-student-year/{studentId}/{academicYear}") {
            val studentId = call.parameters["studentId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid studentId")

            val academicYear = call.parameters["academicYear"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "academicYear missing")

            call.respond(NotesStudentService.getNotesByStudentAndYear(studentId, academicYear))
        }

 
        get("by-course/{courseId}") {
            val courseId = call.parameters["courseId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "courseId missing")

            call.respond(NotesStudentService.getNotesByCourseId(courseId))
        }


        post {
            val req = call.receive<NotesStudentWriteRequest>()
            val created = NotesStudentService.createNote(req)
            call.respond(HttpStatusCode.Created, created)
        }


        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<NotesStudentWriteRequest>()
            val updated = NotesStudentService.updateNote(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Note not found")

            call.respond(updated)
        }

        
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = NotesStudentService.deleteNote(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Note not found")
        }
    }
}

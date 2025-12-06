package be.ecam.server.routes

import be.ecam.server.models.StudentWriteRequest
import be.ecam.server.services.StudentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentRoutes() {

    route("/students") {

        // 🔹 GET /api/students
        get {
            call.respond(StudentService.getAllStudents())
        }

        // 🔹 GET /api/students/{id}
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val student = StudentService.getStudentById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Student not found")

            call.respond(student)
        }

        // 🔹 GET /api/students/by-number/{studentNumber}
        get("by-number/{studentNumber}") {
            val studentNumber = call.parameters["studentNumber"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing studentNumber")

            val student = StudentService.getStudentByNumber(studentNumber)
            call.respond(student ?: emptyMap<String, String>())
        }

        // 🔹 GET /api/students/by-email/{email}
        get("by-email/{email}") {
            val email = call.parameters["email"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing email")

            val student = StudentService.getStudentByEmail(email)
            call.respond(student ?: emptyMap<String, String>())
        }

        // 🔹 GET /api/students/by-group/{groupCode}
        get("by-group/{groupCode}") {
            val groupCode = call.parameters["groupCode"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing groupCode")

            call.respond(StudentService.getStudentsByGroup(groupCode))
        }

        // 🔹 POST /api/students
        post {
            val req = call.receive<StudentWriteRequest>()
            val created = StudentService.createStudent(req)
            call.respond(HttpStatusCode.Created, created)
        }

        // 🔹 PUT /api/students/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<StudentWriteRequest>()
            val updated = StudentService.updateStudent(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Student not found")

            call.respond(updated)
        }

        // 🔹 DELETE /api/students/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = StudentService.deleteStudent(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Student not found")
        }
    }
}

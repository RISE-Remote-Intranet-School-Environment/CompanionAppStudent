package be.ecam.server.routes

import be.ecam.server.models.CourseDetailsWriteRequest
import be.ecam.server.services.CourseDetailsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseDetailsRoutes() {

    route("/course-details") {

        
        get {
            call.respond(CourseDetailsService.getAllCourseDetails())
        }

        
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val details = CourseDetailsService.getCourseDetailsById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "CourseDetails not found")

            call.respond(details)
        }

        
        get("by-course/{courseId}") {
            val courseId = call.parameters["courseId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "courseId missing")

            call.respond(CourseDetailsService.getCourseDetailsByCourseId(courseId))
        }

        
        get("by-sous-course/{sousCourseId}") {
            val sousCourseId = call.parameters["sousCourseId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "sousCourseId missing")

            call.respond(CourseDetailsService.getCourseDetailsBySousCourseId(sousCourseId))
        }

        
        get("by-bloc/{blocId}") {
            val blocId = call.parameters["blocId"]?.toString()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "blocId invalid")

            call.respond(CourseDetailsService.getCourseDetailsByBlocId(blocId))
        }

        
        post {
            val req = call.receive<CourseDetailsWriteRequest>()
            val created = CourseDetailsService.createCourseDetails(req)
            call.respond(HttpStatusCode.Created, created)
        }

        
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<CourseDetailsWriteRequest>()
            val updated = CourseDetailsService.updateCourseDetails(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "CourseDetails not found")

            call.respond(updated)
        }

        
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = CourseDetailsService.deleteCourseDetails(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "CourseDetails not found")
        }
    }
}

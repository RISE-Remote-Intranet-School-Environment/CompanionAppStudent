// package be.ecam.server.routes

// import be.ecam.server.models.CalendarEventWriteRequest
// import be.ecam.server.services.CalendarService
// import io.ktor.http.*
// import io.ktor.server.application.*
// import io.ktor.server.request.*
// import io.ktor.server.response.*
// import io.ktor.server.routing.*

// fun Route.calendarRoutes() {

//     // ---------- EVENTS (READ) ----------

//     // get all events
//     get("/calendar/events") {
//         call.respond(CalendarService.getAllEvents())
//     }

//     // get events for group
//     get("/calendar/group/{groupCode}") {
//         val groupCode = call.parameters["groupCode"]
//             ?: return@get call.respond(HttpStatusCode.BadRequest, "groupCode manquant")

//         call.respond(CalendarService.getEventsForGroup(groupCode))
//     }

//     // get events for owner (ex: GLOBAL, ECAM, etc.)
//     get("/calendar/owner/{ownerRef}") {
//         val ownerRef = call.parameters["ownerRef"]
//             ?: return@get call.respond(HttpStatusCode.BadRequest, "ownerRef manquant")

//         call.respond(CalendarService.getEventsForOwner(ownerRef))
//     }

//     // ---------- EVENTS (CRUD ADMIN) ----------

//     post("/calendar/events") {
//         val req = call.receive<CalendarEventWriteRequest>()
//         val event = CalendarService.createEvent(req)
//         call.respond(HttpStatusCode.Created, event)
//     }

//     put("/calendar/events/{id}") {
//         val id = call.parameters["id"]?.toIntOrNull()
//             ?: return@put call.respond(HttpStatusCode.BadRequest, "ID invalide")

//         val req = call.receive<CalendarEventWriteRequest>()
//         val updated = CalendarService.updateEvent(id, req)
//             ?: return@put call.respond(HttpStatusCode.NotFound, "Événement introuvable")

//         call.respond(updated)
//     }

//     delete("/calendar/events/{id}") {
//         val id = call.parameters["id"]?.toIntOrNull()
//             ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID invalide")

//         val ok = CalendarService.deleteEvent(id)
//         if (ok) call.respond(HttpStatusCode.NoContent)
//         else call.respond(HttpStatusCode.NotFound, "Événement introuvable")
//     }

//     // ---------- COURSE SCHEDULE ROUTES ----------

//     // all schedule
//     get("/calendar/schedule") {
//         call.respond(CalendarService.getAllCourseSchedule())
//     }

//     // By week
//     get("/calendar/schedule/week/{week}") {
//         val week = call.parameters["week"]?.toIntOrNull()
//             ?: return@get call.respond(HttpStatusCode.BadRequest, "week invalide")

//         call.respond(CalendarService.getScheduleForWeek(week))
//     }

//     // By year
//     get("/calendar/schedule/year/{year}") {
//         val year = call.parameters["year"]
//             ?: return@get call.respond(HttpStatusCode.BadRequest, "year manquant")

//         call.respond(CalendarService.getScheduleForYear(year))
//     }

//     // By year + group
//     get("/calendar/schedule/year/{year}/group/{group}") {
//         val year = call.parameters["year"]
//             ?: return@get call.respond(HttpStatusCode.BadRequest, "year manquant")
//         val group = call.parameters["group"]?.toIntOrNull()
//             ?: return@get call.respond(HttpStatusCode.BadRequest, "group invalide")

//         call.respond(CalendarService.getScheduleForYearAndGroup(year, group))
//     }

//     // By course code
//     get("/calendar/schedule/course/{code}") {
//         val code = call.parameters["code"]
//             ?: return@get call.respond(HttpStatusCode.BadRequest, "code manquant")

//         call.respond(CalendarService.getScheduleForCourse(code))
//     }

//     // ---------- DEBUG / SEEDING ----------

//     get("/calendar/debug/seed/events") {
//         try {
//             CalendarService.seedCalendarEventsFromJson()
//             call.respondText("Calendar events imported from ecam_calendar_events_2025_2026.json")
//         } catch (e: Throwable) {
//             e.printStackTrace()
//             call.respond(
//                 HttpStatusCode.InternalServerError,
//                 "Error in seedCalendarEventsFromJson: ${e.message}"
//             )
//         }
//     }

//     get("/calendar/debug/seed/schedule") {
//         try {
//             CalendarService.seedCourseScheduleFromJson()
//             call.respondText("Course schedule imported from ecam_calendar_courses_schedule_2025.json")
//         } catch (e: Throwable) {
//             e.printStackTrace()
//             call.respond(
//                 HttpStatusCode.InternalServerError,
//                 "Error in seedCourseScheduleFromJson: ${e.message}"
//             )
//         }
//     }
// }

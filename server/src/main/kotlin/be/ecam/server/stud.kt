// // // package be.ecam.server.models

// // // import kotlinx.serialization.Serializable
// // // import org.jetbrains.exposed.dao.IntEntity
// // // import org.jetbrains.exposed.dao.IntEntityClass
// // // import org.jetbrains.exposed.dao.id.EntityID
// // // import org.jetbrains.exposed.dao.id.IntIdTable

// // // // Table "students" (infos spécifiques étudiant)
// // // object StudentsTable : IntIdTable("students") {
// // //     val firstName = varchar("first_name", 100)
// // //     val lastName = varchar("last_name", 100)
// // //     val ecamEmail = varchar("ecam_email", 255).uniqueIndex()
// // //     val studentNumber = varchar("student_number", 50).uniqueIndex() // ex: 21252
// // //     val groupCode = varchar("group_code", 50).nullable()            // ex: 3BE, 4IT, etc.
// // // }

// // // // Entité DAO Exposed (comme Professor)
// // // class Student(id: EntityID<Int>) : IntEntity(id) {
// // //     companion object : IntEntityClass<Student>(StudentsTable)

// // //     var firstName by StudentsTable.firstName
// // //     var lastName by StudentsTable.lastName
// // //     var ecamEmail by StudentsTable.ecamEmail
// // //     var studentNumber by StudentsTable.studentNumber
// // //     var groupCode by StudentsTable.groupCode
// // // }

// // // // DTO pour exposer les étudiants au front
// // // @Serializable
// // // data class StudentDTO(
// // //     val id: Int,
// // //     val firstName: String,
// // //     val lastName: String,
// // //     val ecamEmail: String,
// // //     val studentNumber: String,
// // //     val groupCode: String?
// // // )

// // // // DTO pour créer un étudiant
// // // @Serializable
// // // data class StudentCreateRequest(
// // //     val firstName: String,
// // //     val lastName: String,
// // //     val ecamEmail: String,
// // //     val studentNumber: String,
// // //     val groupCode: String? = null
// // // )

// // // // DTO pour update partiel d’un étudiant
// // // @Serializable
// // // data class StudentUpdateRequest(
// // //     val firstName: String? = null,
// // //     val lastName: String? = null,
// // //     val ecamEmail: String? = null,
// // //     val studentNumber: String? = null,
// // //     val groupCode: String? = null
// // // )

// // // // Helper de mapping Entité -> DTO
// // // fun Student.toDTO(): StudentDTO =
// // //     StudentDTO(
// // //         id = this.id.value,
// // //         firstName = this.firstName,
// // //         lastName = this.lastName,
// // //         ecamEmail = this.ecamEmail,
// // //         studentNumber = this.studentNumber,
// // //         groupCode = this.groupCode
// // //     )

    

// // package be.ecam.server.routes

// // import be.ecam.server.models.CourseDetailsWriteRequest
// // import be.ecam.server.services.CourseDetailsService
// // import io.ktor.http.*
// // import io.ktor.server.application.*
// // import io.ktor.server.request.*
// // import io.ktor.server.response.*
// // import io.ktor.server.routing.*

// // fun Route.courseDetailsRoutes() {

// //     route("/course-details") {

// //         // 🔹 GET /api/course-details
// //         get {
// //             call.respond(CourseDetailsService.getAllCourseDetails())
// //         }

// //         // 🔹 GET /api/course-details/{id}  (id DB)
// //         get("{id}") {
// //             val id = call.parameters["id"]?.toIntOrNull()
// //                 ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

// //             val details = CourseDetailsService.getCourseDetailsById(id)
// //                 ?: return@get call.respond(HttpStatusCode.NotFound, "Course details not found")

// //             call.respond(details)
// //         }

// //         // 🔹 GET /api/course-details/by-course/{courseId}
// //         get("by-course/{courseId}") {
// //             val courseId = call.parameters["courseId"]
// //                 ?: return@get call.respond(HttpStatusCode.BadRequest, "courseId missing")

// //             call.respond(CourseDetailsService.getCourseDetailsByCourseId(courseId))
// //         }

// //         // 🔹 GET /api/course-details/by-sous-course/{sousCourseId}
// //         get("by-sous-course/{sousCourseId}") {
// //             val sousCourseId = call.parameters["sousCourseId"]
// //                 ?: return@get call.respond(HttpStatusCode.BadRequest, "sousCourseId missing")

// //             call.respond(CourseDetailsService.getCourseDetailsBySousCourseId(sousCourseId))
// //         }

// //         // 🔹 GET /api/course-details/by-bloc/{blocId}
// //         get("by-bloc/{blocId}") {
// //             val blocId = call.parameters["blocId"]?.toIntOrNull()
// //                 ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid blocId")

// //             call.respond(CourseDetailsService.getCourseDetailsByBlocId(blocId))
// //         }

// //         // 🔹 POST /api/course-details
// //         post {
// //             val req = call.receive<CourseDetailsWriteRequest>()
// //             val created = CourseDetailsService.createCourseDetails(req)
// //             call.respond(HttpStatusCode.Created, created)
// //         }

// //         // 🔹 PUT /api/course-details/{id}
// //         put("{id}") {
// //             val id = call.parameters["id"]?.toIntOrNull()
// //                 ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

// //             val req = call.receive<CourseDetailsWriteRequest>()
// //             val updated = CourseDetailsService.updateCourseDetails(id, req)
// //                 ?: return@put call.respond(HttpStatusCode.NotFound, "Course details not found")

// //             call.respond(updated)
// //         }

// //         // 🔹 DELETE /api/course-details/{id}
// //         delete("{id}") {
// //             val id = call.parameters["id"]?.toIntOrNull()
// //                 ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

// //             val ok = CourseDetailsService.deleteCourseDetails(id)
// //             if (ok) call.respond(HttpStatusCode.NoContent)
// //             else call.respond(HttpStatusCode.NotFound, "Course details not found")
// //         }
// //     }
// // }


// package be.ecam.server.services

// import be.ecam.server.models.*
// import kotlinx.serialization.Serializable
// import kotlinx.serialization.json.Json
// import org.jetbrains.exposed.sql.*
// import org.jetbrains.exposed.sql.SqlExpressionBuilder.and
// import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
// import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
// import org.jetbrains.exposed.sql.transactions.transaction

// object CourseDetailsService {

//     // ---------- JSON model pour le seed ----------

//     @Serializable
//     private data class CourseDetailsJson(
//         val course_id: String,
//         val responsable: String? = null,
//         val sous_course_id: String? = null,
//         val teachers_raw_id: String? = null,
//         val formation_ids: String? = null,
//         val periods: Int? = null,
//         val hours_q1: Int? = null,
//         val hours_q2: Int? = null,
//         val contribution: String? = null,
//         val learning_outcomes: String? = null,
//         val content: String? = null,
//         val teaching_methods: String? = null,
//         val evaluation_methods: String? = null,
//         val course_material: String? = null,
//         val bibliography: String? = null,
//         val bloc_id: Int? = null
//     )

//     private val json = Json { ignoreUnknownKeys = true }

//     // ---------- SEED depuis JSON ----------

//     fun seedCourseDetailsFromJson() {
//         val resource = CourseDetailsService::class.java.classLoader
//             .getResource("files/ecam_courses_details_2025_2026.json")
//             // 🔁 adapte le nom de fichier si besoin
//             ?: error("Resource 'files/ecam_courses_details_2025_2026.json' not found in classpath")

//         val text = resource.readText()
//         val records = json.decodeFromString<List<CourseDetailsJson>>(text)

//         transaction {
//             records.forEach { r ->

//                 // Critère d’unicité (idempotent) : (course_id, sous_course_id)
//                 val criteria =
//                     if (r.sous_course_id != null) {
//                         (CourseDetailsTable.courseId eq r.course_id) and
//                                 (CourseDetailsTable.sousCourseId eq r.sous_course_id)
//                     } else {
//                         (CourseDetailsTable.courseId eq r.course_id) and
//                                 CourseDetailsTable.sousCourseId.isNull()
//                     }

//                 val exists = CourseDetailsTable
//                     .select { criteria }
//                     .any()

//                 if (!exists) {
//                     CourseDetailsTable.insert { row ->
//                         row[courseId] = r.course_id
//                         row[responsable] = r.responsable
//                         row[sousCourseId] = r.sous_course_id
//                         row[teachersRawId] = r.teachers_raw_id
//                         row[formationIds] = r.formation_ids
//                         row[periods] = r.periods
//                         row[hoursQ1] = r.hours_q1
//                         row[hoursQ2] = r.hours_q2
//                         row[contribution] = r.contribution
//                         row[learningOutcomes] = r.learning_outcomes
//                         row[content] = r.content
//                         row[teachingMethods] = r.teaching_methods
//                         row[evaluationMethods] = r.evaluation_methods
//                         row[courseMaterial] = r.course_material
//                         row[bibliography] = r.bibliography
//                         row[blocId] = r.bloc_id
//                     }
//                 }
//             }
//         }
//     }

//     // ---------- READ ----------

//     fun getAllCourseDetails(): List<CourseDetailsDTO> = transaction {
//         CourseDetailsTable
//             .selectAll()
//             .map { it.toCourseDetailsDTO() }
//     }

//     fun getCourseDetailsById(id: Int): CourseDetailsDTO? = transaction {
//         CourseDetailsTable
//             .select { CourseDetailsTable.id eq id }
//             .singleOrNull()
//             ?.toCourseDetailsDTO()
//     }

//     fun getCourseDetailsByCourseId(courseId: String): List<CourseDetailsDTO> = transaction {
//         CourseDetailsTable
//             .select { CourseDetailsTable.courseId eq courseId }
//             .map { it.toCourseDetailsDTO() }
//     }

//     // ---------- CREATE ----------

//     fun createCourseDetails(req: CourseDetailsWriteRequest): CourseDetailsDTO = transaction {
//         val newId = CourseDetailsTable.insertAndGetId { row ->
//             row[courseId] = req.courseId
//             row[responsable] = req.responsable
//             row[sousCourseId] = req.sousCourseId
//             row[teachersRawId] = req.teachersRawId
//             row[formationIds] = req.formationIds
//             row[periods] = req.periods
//             row[hoursQ1] = req.hoursQ1
//             row[hoursQ2] = req.hoursQ2
//             row[contribution] = req.contribution
//             row[learningOutcomes] = req.learningOutcomes
//             row[content] = req.content
//             row[teachingMethods] = req.teachingMethods
//             row[evaluationMethods] = req.evaluationMethods
//             row[courseMaterial] = req.courseMaterial
//             row[bibliography] = req.bibliography
//             row[blocId] = req.blocId
//         }

//         CourseDetailsTable
//             .select { CourseDetailsTable.id eq newId }
//             .single()
//             .toCourseDetailsDTO()
//     }

//     // ---------- UPDATE ----------

//     fun updateCourseDetails(id: Int, req: CourseDetailsWriteRequest): CourseDetailsDTO? = transaction {
//         val updatedCount = CourseDetailsTable.update({ CourseDetailsTable.id eq id }) { row ->
//             row[courseId] = req.courseId
//             row[responsable] = req.responsable
//             row[sousCourseId] = req.sousCourseId
//             row[teachersRawId] = req.teachersRawId
//             row[formationIds] = req.formationIds
//             row[periods] = req.periods
//             row[hoursQ1] = req.hoursQ1
//             row[hoursQ2] = req.hoursQ2
//             row[contribution] = req.contribution
//             row[learningOutcomes] = req.learningOutcomes
//             row[content] = req.content
//             row[teachingMethods] = req.teachingMethods
//             row[evaluationMethods] = req.evaluationMethods
//             row[courseMaterial] = req.courseMaterial
//             row[bibliography] = req.bibliography
//             row[blocId] = req.blocId
//         }

//         if (updatedCount == 0) return@transaction null

//         CourseDetailsTable
//             .select { CourseDetailsTable.id eq id }
//             .singleOrNull()
//             ?.toCourseDetailsDTO()
//     }

//     // ---------- DELETE ----------

//     fun deleteCourseDetails(id: Int): Boolean = transaction {
//         val deleted = CourseDetailsTable.deleteWhere { CourseDetailsTable.id eq id }
//         deleted > 0
//     }
// }
// package be.ecam.server.services

// import be.ecam.server.models.*
// import org.jetbrains.exposed.sql.*
// import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
// import org.jetbrains.exposed.sql.transactions.transaction

// object CourseDetailsService {

//     // 🔹 GET all details
//     fun getAllCourseDetails(): List<CourseDetailsDTO> = transaction {
//         CourseDetailsTable
//             .selectAll()
//             .map { it.toCourseDetailsDTO() }
//     }

//     // 🔹 GET by DB id
//     fun getCourseDetailsById(id: Int): CourseDetailsDTO? = transaction {
//         CourseDetailsTable
//             .select { CourseDetailsTable.id eq id }
//             .singleOrNull()
//             ?.toCourseDetailsDTO()
//     }

//     // 🔹 GET by logical courseId (ex: "4EIDB40")
//     fun getCourseDetailsByCourseId(courseId: String): List<CourseDetailsDTO> = transaction {
//         CourseDetailsTable
//             .select { CourseDetailsTable.courseId eq courseId }
//             .map { it.toCourseDetailsDTO() }
//     }

//     // 🔹 GET by sousCourseId (ex: sous-partie d’un cours)
//     fun getCourseDetailsBySousCourseId(sousCourseId: String): List<CourseDetailsDTO> = transaction {
//         CourseDetailsTable
//             .select { CourseDetailsTable.sousCourseId eq sousCourseId }
//             .map { it.toCourseDetailsDTO() }
//     }

//     // 🔹 GET by blocId
//     fun getCourseDetailsByBlocId(blocId: Int): List<CourseDetailsDTO> = transaction {
//         CourseDetailsTable
//             .select { CourseDetailsTable.blocId eq blocId }
//             .map { it.toCourseDetailsDTO() }
//     }

//     // 🔹 CREATE
//     fun createCourseDetails(req: CourseDetailsWriteRequest): CourseDetailsDTO = transaction {
//         val newId = CourseDetailsTable.insertAndGetId { row ->
//             row[courseId] = req.courseId
//             row[responsable] = req.responsable
//             row[sousCourseId] = req.sousCourseId
//             row[teachersRawId] = req.teachersRawId
//             row[formationIds] = req.formationIds
//             row[periods] = req.periods
//             row[hoursQ1] = req.hoursQ1
//             row[hoursQ2] = req.hoursQ2
//             row[contribution] = req.contribution
//             row[learningOutcomes] = req.learningOutcomes
//             row[content] = req.content
//             row[teachingMethods] = req.teachingMethods
//             row[evaluationMethods] = req.evaluationMethods
//             row[courseMaterial] = req.courseMaterial
//             row[bibliography] = req.bibliography
//             row[blocId] = req.blocId
//         }

//         CourseDetailsTable
//             .select { CourseDetailsTable.id eq newId }
//             .single()
//             .toCourseDetailsDTO()
//     }

//     // 🔹 UPDATE
//     fun updateCourseDetails(id: Int, req: CourseDetailsWriteRequest): CourseDetailsDTO? = transaction {
//         val updated = CourseDetailsTable.update({ CourseDetailsTable.id eq id }) { row ->
//             row[courseId] = req.courseId
//             row[responsable] = req.responsable
//             row[sousCourseId] = req.sousCourseId
//             row[teachersRawId] = req.teachersRawId
//             row[formationIds] = req.formationIds
//             row[periods] = req.periods
//             row[hoursQ1] = req.hoursQ1
//             row[hoursQ2] = req.hoursQ2
//             row[contribution] = req.contribution
//             row[learningOutcomes] = req.learningOutcomes
//             row[content] = req.content
//             row[teachingMethods] = req.teachingMethods
//             row[evaluationMethods] = req.evaluationMethods
//             row[courseMaterial] = req.courseMaterial
//             row[bibliography] = req.bibliography
//             row[blocId] = req.blocId
//         }

//         if (updated == 0) return@transaction null

//         CourseDetailsTable
//             .select { CourseDetailsTable.id eq id }
//             .singleOrNull()
//             ?.toCourseDetailsDTO()
//     }

//     // 🔹 DELETE
//     fun deleteCourseDetails(id: Int): Boolean = transaction {
//         CourseDetailsTable.deleteWhere { CourseDetailsTable.id eq id } > 0
//     }
// }

package be.ecam.companion.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class UserCoursesRepository(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String
) {
    
    suspend fun getMyCourses(token: String): List<String> {
        return try {
            val response = client.get("${baseUrlProvider()}/api/my-courses") {
                header(HttpHeaders.Authorization, "Bearer ${token.trim().removeSurrounding("\"")}")
            }
            if (response.status.isSuccess()) {
                val body: MyCoursesResponse = response.body()
                body.courses
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Erreur getMyCourses: ${e.message}")
            emptyList()
        }
    }

    suspend fun addCourse(token: String, courseId: String): Boolean {
        return try {
            val response = client.post("${baseUrlProvider()}/api/my-courses") {
                header(HttpHeaders.Authorization, "Bearer ${token.trim().removeSurrounding("\"")}")
                contentType(ContentType.Application.Json)
                setBody(AddCourseRequest(courseId))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Erreur addCourse: ${e.message}")
            false
        }
    }

    suspend fun removeCourse(token: String, courseId: String): Boolean {
        return try {
            val response = client.delete("${baseUrlProvider()}/api/my-courses/$courseId") {
                header(HttpHeaders.Authorization, "Bearer ${token.trim().removeSurrounding("\"")}")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Erreur removeCourse: ${e.message}")
            false
        }
    }

    suspend fun setCourses(token: String, courseIds: List<String>): Boolean {
        return try {
            val response = client.put("${baseUrlProvider()}/api/my-courses") {
                header(HttpHeaders.Authorization, "Bearer ${token.trim().removeSurrounding("\"")}")
                contentType(ContentType.Application.Json)
                setBody(SetCoursesRequest(courseIds))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Erreur setCourses: ${e.message}")
            false
        }
    }
}

@Serializable
private data class MyCoursesResponse(val courses: List<String>)

@Serializable
private data class AddCourseRequest(val courseId: String)

@Serializable
private data class SetCoursesRequest(val courseIds: List<String>)
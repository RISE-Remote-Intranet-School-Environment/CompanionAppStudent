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
                // Sauvegarder dans le cache
                CacheHelper.save(CacheKeys.USER_COURSES, body.courses)
                // Signaler que le réseau fonctionne
                ConnectivityState.reportSuccess()
                body.courses
            } else {
                ConnectivityState.reportNetworkError("Erreur my-courses: ${response.status}")
                loadCachedCourses()
            }
        } catch (e: Exception) {
            println("Erreur getMyCourses: ${e.message}, chargement du cache...")
            ConnectivityState.reportNetworkError(e.message)
            loadCachedCourses()
        }
    }

    private fun loadCachedCourses(): List<String> {
        return CacheHelper.load<List<String>>(CacheKeys.USER_COURSES) ?: emptyList()
    }

    suspend fun addCourse(token: String, courseId: String): Boolean {
        return try {
            val response = client.post("${baseUrlProvider()}/api/my-courses") {
                header(HttpHeaders.Authorization, "Bearer ${token.trim().removeSurrounding("\"")}")
                contentType(ContentType.Application.Json)
                setBody(AddCourseRequest(courseId))
            }
            if (response.status.isSuccess()) {
                ConnectivityState.reportSuccess()
                true
            } else {
                ConnectivityState.reportNetworkError("Erreur addCourse: ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("Erreur addCourse: ${e.message}")
            ConnectivityState.reportNetworkError(e.message)
            false
        }
    }

    suspend fun removeCourse(token: String, courseId: String): Boolean {
        return try {
            val response = client.delete("${baseUrlProvider()}/api/my-courses/$courseId") {
                header(HttpHeaders.Authorization, "Bearer ${token.trim().removeSurrounding("\"")}")
            }
            if (response.status.isSuccess()) {
                ConnectivityState.reportSuccess()
                true
            } else {
                ConnectivityState.reportNetworkError("Erreur removeCourse: ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("Erreur removeCourse: ${e.message}")
            ConnectivityState.reportNetworkError(e.message)
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
            if (response.status.isSuccess()) {
                ConnectivityState.reportSuccess()
                true
            } else {
                ConnectivityState.reportNetworkError("Erreur setCourses: ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("Erreur setCourses: ${e.message}")
            ConnectivityState.reportNetworkError(e.message)
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
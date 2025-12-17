package be.ecam.common.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import companion.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

class CourseResourceRepository {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun getAllResources(): CourseResourcesMap {
        val bytes = Res.readBytes("files/ecam_courses_resources_2025.json")
        val jsonString = bytes.decodeToString()

        return json.decodeFromString(jsonString)
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun getResourcesForCourse(courseCode: String): List<CourseResource> {
        val all = getAllResources()
        return all[courseCode] ?: emptyList()
    }
}

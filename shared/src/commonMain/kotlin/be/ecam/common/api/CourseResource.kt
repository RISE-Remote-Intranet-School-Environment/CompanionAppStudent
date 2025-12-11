package be.ecam.common.api

import kotlinx.serialization.Serializable

@Serializable
data class CourseResource(
    val title: String,
    val type: String,
    val url: String
)

typealias CourseResourcesMap = Map<String, List<CourseResource>>


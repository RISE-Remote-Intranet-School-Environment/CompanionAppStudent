package be.ecam.companion.data

import kotlinx.serialization.Serializable

@Serializable
data class OrganizedActivity(
    val code: String,
    val title: String,
    val hours_Q1: String? = null,
    val hours_Q2: String? = null,
    val teachers: List<String> = emptyList(),
    val language: String? = null
)

@Serializable
data class EvaluatedActivity(
    val code: String,
    val title: String,
    val weight: String? = null,
    val type_Q1: String? = null,
    val type_Q2: String? = null,
    val type_Q3: String? = null,
    val teachers: List<String> = emptyList(),
    val language: String? = null,
    val linked_activities: List<String> = emptyList()
)

@Serializable
data class CourseDetail(
    val code: String,
    val title: String,
    val credits: String? = null,
    val hours: String? = null,
    val mandatory: Boolean = false,
    val bloc: String? = null,
    val program: String? = null,
    val responsable: String? = null,
    val language: String? = null,
    val organized_activities: List<OrganizedActivity> = emptyList(),
    val evaluated_activities: List<EvaluatedActivity> = emptyList(),
    val sections: Map<String, String> = emptyMap()
)

package be.ecam.common.api

import kotlinx.serialization.Serializable

@Serializable
data class HelloResponse(val message: String)

@Serializable
data class ScheduleItem(val title: String)

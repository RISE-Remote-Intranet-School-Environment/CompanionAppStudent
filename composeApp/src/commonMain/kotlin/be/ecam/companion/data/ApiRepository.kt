package be.ecam.companion.data

import be.ecam.common.api.HelloResponse
import be.ecam.common.api.ScheduleItem

interface ApiRepository {
    suspend fun getHello(): String
    suspend fun fetchHello(): HelloResponse
    suspend fun fetchSchedule(): Map<String, List<ScheduleItem>>
}

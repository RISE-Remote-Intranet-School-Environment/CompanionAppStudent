package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CourseScheduleService {

    //  GET all schedules
    fun getAllSchedules(): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .map { it.toCourseScheduleDTO() }
    }

    //  GET by DB id
    fun getScheduleById(id: Int): CourseScheduleDTO? = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.id eq id }
            .singleOrNull()
            ?.toCourseScheduleDTO()
    }

    
    fun getSchedulesByWeek(week: Int): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.week eq week }
            .map { it.toCourseScheduleDTO() }
    }

    
    fun getSchedulesByDate(date: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.date eq date }
            .map { it.toCourseScheduleDTO() }
    }


    fun getSchedulesByYearOption(yearOptionId: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.yearOptionId eq yearOptionId }
            .map { it.toCourseScheduleDTO() }
    }

    
    fun getSchedulesByGroup(groupNo: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.groupNo eq groupNo }
            .map { it.toCourseScheduleDTO() }
    }


    fun getSchedulesByRaccourci(courseRaccourciId: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.courseRaccourciId eq courseRaccourciId }
            .map { it.toCourseScheduleDTO() }
    }

    //  CREATE
    fun createSchedule(req: CourseScheduleWriteRequest): CourseScheduleDTO = transaction {
        val newId = CourseScheduleTable.insertAndGetId { row ->
            row[CourseScheduleTable.week] = req.week
            row[CourseScheduleTable.yearOptionId] = req.yearOptionId
            row[CourseScheduleTable.groupNo] = req.groupNo
            row[CourseScheduleTable.seriesJson] = req.seriesJson
            row[CourseScheduleTable.date] = req.date
            row[CourseScheduleTable.dayName] = req.dayName
            row[CourseScheduleTable.startTime] = req.startTime
            row[CourseScheduleTable.endTime] = req.endTime
            row[CourseScheduleTable.courseRaccourciId] = req.courseRaccourciId
            row[CourseScheduleTable.title] = req.title
            row[CourseScheduleTable.teachersJson] = req.teachersJson
            row[CourseScheduleTable.roomIds] = req.roomIds
            row[CourseScheduleTable.sousCourseId] = req.sousCourseId
        }

        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.id eq newId }
            .single()
            .toCourseScheduleDTO()
    }

    //  UPDATE
    fun updateSchedule(id: Int, req: CourseScheduleWriteRequest): CourseScheduleDTO? = transaction {
        val updated = CourseScheduleTable.update({ CourseScheduleTable.id eq id }) { row ->
            row[CourseScheduleTable.week] = req.week
            row[CourseScheduleTable.yearOptionId] = req.yearOptionId
            row[CourseScheduleTable.groupNo] = req.groupNo
            row[CourseScheduleTable.seriesJson] = req.seriesJson
            row[CourseScheduleTable.date] = req.date
            row[CourseScheduleTable.dayName] = req.dayName
            row[CourseScheduleTable.startTime] = req.startTime
            row[CourseScheduleTable.endTime] = req.endTime
            row[CourseScheduleTable.courseRaccourciId] = req.courseRaccourciId
            row[CourseScheduleTable.title] = req.title
            row[CourseScheduleTable.teachersJson] = req.teachersJson
            row[CourseScheduleTable.roomIds] = req.roomIds
            row[CourseScheduleTable.sousCourseId] = req.sousCourseId
        }

        if (updated == 0) return@transaction null

        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.id eq id }
            .singleOrNull()
            ?.toCourseScheduleDTO()
    }

    //  DELETE
    fun deleteSchedule(id: Int): Boolean = transaction {
        CourseScheduleTable.deleteWhere { CourseScheduleTable.id eq id } > 0
    }

    //  GET all filtered schedules
    fun getAllFiltered(
        yearOptionId: String? = null,
        seriesId: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): List<CourseScheduleDTO> = transaction {
        var query = CourseScheduleTable.selectAll()
        
        // Filtre par yearOptionId
        yearOptionId?.let {
            query = query.andWhere { CourseScheduleTable.yearOptionId eq it }
        }
        
        // Filtre par date de début
        startDate?.let {
            query = query.andWhere { CourseScheduleTable.date greaterEq it }
        }
        
        // Filtre par date de fin
        endDate?.let {
            query = query.andWhere { CourseScheduleTable.date lessEq it }
        }
        
        val results = query.orderBy(CourseScheduleTable.date to SortOrder.ASC).toList()
        
        // Filtre par seriesId (nécessite parsing du JSON)
        if (seriesId != null) {
            results.filter { row ->
                val seriesJson = row[CourseScheduleTable.seriesJson] ?: "[]"
                seriesJson.contains(seriesId, ignoreCase = true)
            }.map { it.toCourseScheduleDTO() }
        } else {
            results.map { it.toCourseScheduleDTO() }
        }
    }

    fun getAll(): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .orderBy(CourseScheduleTable.date to SortOrder.ASC)
            .map { it.toCourseScheduleDTO() }
    }

    fun getById(id: Int): CourseScheduleDTO? = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.id eq id }
            .singleOrNull()
            ?.toCourseScheduleDTO()
    }

    fun getByDate(date: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.date eq date }
            .orderBy(CourseScheduleTable.startTime to SortOrder.ASC)
            .map { it.toCourseScheduleDTO() }
    }

    fun getByYearOption(yearOptionId: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.yearOptionId eq yearOptionId }
            .orderBy(CourseScheduleTable.date to SortOrder.ASC)
            .map { it.toCourseScheduleDTO() }
    }

    fun getByCourse(courseId: String): List<CourseScheduleDTO> = transaction {
        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.courseRaccourciId eq courseId }
            .orderBy(CourseScheduleTable.date to SortOrder.ASC)
            .map { it.toCourseScheduleDTO() }
    }

    fun create(req: CourseScheduleWriteRequest): CourseScheduleDTO = transaction {
        val newId = CourseScheduleTable.insertAndGetId { row ->
            row[week] = req.week
            row[yearOptionId] = req.yearOptionId
            row[groupNo] = req.groupNo
            row[seriesJson] = req.seriesJson
            row[date] = req.date
            row[dayName] = req.dayName
            row[startTime] = req.startTime
            row[endTime] = req.endTime
            row[courseRaccourciId] = req.courseRaccourciId
            row[title] = req.title
            row[teachersJson] = req.teachersJson
            row[roomIds] = req.roomIds
            row[sousCourseId] = req.sousCourseId
        }

        CourseScheduleTable
            .selectAll()
            .where { CourseScheduleTable.id eq newId }
            .single()
            .toCourseScheduleDTO()
    }

    fun delete(id: Int): Boolean = transaction {
        CourseScheduleTable.deleteWhere { CourseScheduleTable.id eq id } > 0
    }

    /**
     * Récupère l'horaire personnalisé d'un étudiant via son email.
     * 
     * Chaîne de liaison:
     * pae_students.course_ids (ex: "4eial40")
     *     → sous_course.course_id (ex: "4eial40")
     *     → sous_course.sous_course_id (ex: "AL4T-T1-2017")
     *     → course_schedule.sous_course_id (ex: "AL4T-T1-2017")
     */
    fun getScheduleForStudent(email: String): List<CourseScheduleDTO> = transaction {
        // 1. Trouver l'étudiant PAE par email
        val paeStudent = PaeStudentsTable
            .selectAll()
            .where { PaeStudentsTable.email.lowerCase() eq email.lowercase() }
            .firstOrNull()
            ?: run {
                println("⚠️ Aucun PAE trouvé pour email: $email")
                return@transaction emptyList()
            }

        // 2. Extraire les IDs de cours du PAE (ex: "4eial40;4eiam40")
        val rawCourseIds = paeStudent[PaeStudentsTable.courseIds]
        if (rawCourseIds.isNullOrBlank()) {
            println("⚠️ PAE trouvé mais course_ids vide pour: $email")
            return@transaction emptyList()
        }

        val paeCourseIds = rawCourseIds
            .split(Regex("[,;|]"))
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (paeCourseIds.isEmpty()) {
            println("⚠️ Aucun cours parsé depuis course_ids: $rawCourseIds")
            return@transaction emptyList()
        }

        println("📚 PAE courses pour $email: $paeCourseIds")

        // 3. Trouver les sous_course_id correspondants via la table sous_course
        // sous_course.course_id = "4eial40" → sous_course.sous_course_id = "AL4T-T1-2017"
        val sousCourseIds = SousCoursesTable
            .selectAll()
            .where { SousCoursesTable.courseId.lowerCase() inList paeCourseIds }
            .map { it[SousCoursesTable.sousCourseId] }
            .distinct()

        println("📚 SousCourse IDs trouvés: $sousCourseIds (${sousCourseIds.size})")

        // 4. Extraire aussi les préfixes (ex: "AL4T" depuis "AL4T-T1-2017") pour matcher course_raccourci_id
        val raccourciPrefixes = sousCourseIds
            .mapNotNull { it.split("-").firstOrNull()?.uppercase() }
            .distinct()

        println("📚 Raccourci prefixes extraits: $raccourciPrefixes")

        // 5. Récupérer les horaires qui matchent
        if (sousCourseIds.isEmpty() && raccourciPrefixes.isEmpty()) {
            println("⚠️ Aucune correspondance trouvée pour les cours PAE")
            return@transaction emptyList()
        }

        val schedules = CourseScheduleTable
            .selectAll()
            .where {
                val conditions = mutableListOf<Op<Boolean>>()
                
                // Match exact sur sous_course_id
                if (sousCourseIds.isNotEmpty()) {
                    conditions.add(CourseScheduleTable.sousCourseId inList sousCourseIds)
                }
                
                // Match sur le préfixe raccourci (ex: AL4T)
                if (raccourciPrefixes.isNotEmpty()) {
                    conditions.add(CourseScheduleTable.courseRaccourciId.upperCase() inList raccourciPrefixes)
                }
                
                conditions.reduce { acc, op -> acc or op }
            }
            .orderBy(CourseScheduleTable.date to SortOrder.ASC)
            .map { it.toCourseScheduleDTO() }

        println("📅 ${schedules.size} séances trouvées pour $email")
        schedules
    }
}

// Extension pour convertir ResultRow en DTO
private fun ResultRow.toCourseScheduleDTO() = CourseScheduleDTO(
    id = this[CourseScheduleTable.id].value,
    week = this[CourseScheduleTable.week],
    yearOptionId = this[CourseScheduleTable.yearOptionId],
    groupNo = this[CourseScheduleTable.groupNo],
    seriesJson = this[CourseScheduleTable.seriesJson],
    date = this[CourseScheduleTable.date],
    dayName = this[CourseScheduleTable.dayName],
    startTime = this[CourseScheduleTable.startTime],
    endTime = this[CourseScheduleTable.endTime],
    courseRaccourciId = this[CourseScheduleTable.courseRaccourciId],
    title = this[CourseScheduleTable.title],
    teachersJson = this[CourseScheduleTable.teachersJson],
    roomIds = this[CourseScheduleTable.roomIds],
    sousCourseId = this[CourseScheduleTable.sousCourseId]
)

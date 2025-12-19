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
     * Gère les PAE multiples (plusieurs lignes pour le même email).
     */
    fun getScheduleForStudent(email: String): List<CourseScheduleDTO> = transaction {
        // 1. Trouver TOUTES les entrées PAE pour cet email (peut avoir plusieurs lignes)
        val paeEntries = PaeStudentsTable
            .selectAll()
            .where { PaeStudentsTable.email.lowerCase() eq email.lowercase() }
            .toList()

        if (paeEntries.isEmpty()) {
            println("⚠️ Aucun PAE trouvé pour email: $email")
            return@transaction emptyList()
        }

        // 2. Fusionner tous les course_ids de toutes les entrées PAE
        val paeCourseIds = paeEntries
            .mapNotNull { it[PaeStudentsTable.courseIds] }
            .flatMap { rawIds -> 
                rawIds.split(Regex("[,;|]"))
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
            }
            .distinct()  // 🔥 DÉDUPLIQUER les IDs de cours

        if (paeCourseIds.isEmpty()) {
            println("⚠️ PAE trouvé mais course_ids vide pour: $email")
            return@transaction emptyList()
        }

        println("📚 PAE courses pour $email: ${paeCourseIds.size} cours distincts")

        // 3. Trouver les sous_course_id correspondants
        val sousCourseIds = SousCoursesTable
            .selectAll()
            .where { SousCoursesTable.courseId.lowerCase() inList paeCourseIds }
            .map { it[SousCoursesTable.sousCourseId] }
            .distinct()

        println("📚 SousCourse IDs trouvés: ${sousCourseIds.size}")

        // 4. Extraire les préfixes raccourcis
        val raccourciPrefixes = sousCourseIds
            .mapNotNull { it.split("-").firstOrNull()?.uppercase() }
            .distinct()

        println("📚 Raccourci prefixes: ${raccourciPrefixes.size}")

        if (sousCourseIds.isEmpty() && raccourciPrefixes.isEmpty()) {
            println("⚠️ Aucune correspondance trouvée pour les cours PAE")
            return@transaction emptyList()
        }

        // 5. Récupérer les horaires SANS DOUBLONS
        val schedules = CourseScheduleTable
            .selectAll()
            .where {
                val conditions = mutableListOf<Op<Boolean>>()
                
                if (sousCourseIds.isNotEmpty()) {
                    conditions.add(CourseScheduleTable.sousCourseId inList sousCourseIds)
                }
                
                if (raccourciPrefixes.isNotEmpty()) {
                    conditions.add(CourseScheduleTable.courseRaccourciId.upperCase() inList raccourciPrefixes)
                }
                
                conditions.reduce { acc, op -> acc or op }
            }
            .orderBy(CourseScheduleTable.date to SortOrder.ASC)
            .distinctBy { it[CourseScheduleTable.id] }  // 🔥 DISTINCT par ID
            .map { it.toCourseScheduleDTO() }
            .distinctBy { schedule ->  // 🔥 DÉDUPLIQUER aussi par contenu unique
                "${schedule.date}-${schedule.startTime}-${schedule.endTime}-${schedule.courseRaccourciId}-${schedule.title}"
            }

        println("📅 ${schedules.size} séances distinctes trouvées pour $email")
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

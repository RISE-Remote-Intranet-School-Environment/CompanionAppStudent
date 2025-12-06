// package be.ecam.server.services

// import be.ecam.server.models.*
// import org.jetbrains.exposed.sql.*
// import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
// import org.jetbrains.exposed.sql.transactions.transaction

// object CatalogService {

//     // =========================
//     // FORMATIONS
//     // =========================

//     fun getAllFormations(): List<FormationDTO> = transaction {
//         FormationsTable
//             .selectAll()
//             .map { it.toFormationDTO() }
//     }

//     fun getFormationById(id: Int): FormationDTO? = transaction {
//         FormationsTable
//             .select { FormationsTable.id eq id }
//             .singleOrNull()
//             ?.toFormationDTO()
//     }

//     fun getFormationByCode(formationId: String): FormationDTO? = transaction {
//         FormationsTable
//             .select { FormationsTable.formationId eq formationId }
//             .singleOrNull()
//             ?.toFormationDTO()
//     }

//     fun createFormation(req: FormationWriteRequest): FormationDTO = transaction {
//         val newId = FormationsTable.insertAndGetId { row ->
//             row[formationId] = req.formationId
//             row[name] = req.name
//             // adapte selon ton modèle réel :
//             row[program] = req.program
//             row[sourceUrl] = req.sourceUrl
//             row[imageUrl] = req.imageUrl
//         }.value

//         FormationsTable
//             .select { FormationsTable.id eq newId }
//             .single()
//             .toFormationDTO()
//     }

//     fun updateFormation(id: Int, req: FormationWriteRequest): FormationDTO? = transaction {
//         val updated = FormationsTable.update({ FormationsTable.id eq id }) { row ->
//             row[formationId] = req.formationId
//             row[name] = req.name
//             row[program] = req.program
//             row[sourceUrl] = req.sourceUrl
//             row[imageUrl] = req.imageUrl
//         }
//         if (updated == 0) return@transaction null

//         FormationsTable
//             .select { FormationsTable.id eq id }
//             .singleOrNull()
//             ?.toFormationDTO()
//     }

//     fun deleteFormation(id: Int): Boolean = transaction {
//         FormationsTable.deleteWhere { FormationsTable.id eq id } > 0
//     }

//     // =========================
//     // BLOCS
//     // =========================

//     fun getAllBlocs(): List<BlocDTO> = transaction {
//         BlocsTable
//             .selectAll()
//             .map { it.toBlocDTO() }
//     }

//     fun getBlocById(id: Int): BlocDTO? = transaction {
//         BlocsTable
//             .select { BlocsTable.id eq id }
//             .singleOrNull()
//             ?.toBlocDTO()
//     }

//     fun getBlocByCode(blocCode: String): BlocDTO? = transaction {
//         BlocsTable
//             .select { BlocsTable.blocId eq blocCode }
//             .singleOrNull()
//             ?.toBlocDTO()
//     }

//     /**
//      * Blocs liés à une formation donnée.
//      * On suppose que BlocsTable.formationIds contient une liste de codes (CSV ou similaire).
//      */
//     fun getBlocsForFormation(formationCode: String): List<BlocDTO> = transaction {
//         BlocsTable
//             .select { BlocsTable.formationIds like "%$formationCode%" }
//             .map { it.toBlocDTO() }
//     }

//     fun createBloc(req: BlocWriteRequest): BlocDTO = transaction {
//         val newId = BlocsTable.insertAndGetId { row ->
//             row[blocId] = req.blocId
//             row[name] = req.name
//             row[formationIds] = req.formationIds
//         }.value

//         BlocsTable
//             .select { BlocsTable.id eq newId }
//             .single()
//             .toBlocDTO()
//     }

//     fun updateBloc(id: Int, req: BlocWriteRequest): BlocDTO? = transaction {
//         val updated = BlocsTable.update({ BlocsTable.id eq id }) { row ->
//             row[blocId] = req.blocId
//             row[name] = req.name
//             row[formationIds] = req.formationIds
//         }
//         if (updated == 0) return@transaction null

//         BlocsTable
//             .select { BlocsTable.id eq id }
//             .singleOrNull()
//             ?.toBlocDTO()
//     }

//     fun deleteBloc(id: Int): Boolean = transaction {
//         BlocsTable.deleteWhere { BlocsTable.id eq id } > 0
//     }

//     // =========================
//     // COURSES
//     // =========================

//     fun getAllCourses(): List<CourseDTO> = transaction {
//         CoursesTable
//             .selectAll()
//             .map { it.toCourseDTO() }
//     }

//     fun getCourseById(id: Int): CourseDTO? = transaction {
//         CoursesTable
//             .select { CoursesTable.id eq id }
//             .singleOrNull()
//             ?.toCourseDTO()
//     }

//     fun getCourseByCode(courseCode: String): CourseDTO? = transaction {
//         CoursesTable
//             .select { CoursesTable.courseCode eq courseCode }
//             .singleOrNull()
//             ?.toCourseDTO()
//     }

//     fun getCoursesForBloc(blocCode: String): List<CourseDTO> = transaction {
//         CoursesTable
//             .select { CoursesTable.blocId eq blocCode }
//             .map { it.toCourseDTO() }
//     }

//     fun getCoursesForFormation(formationCode: String): List<CourseDTO> = transaction {
//         CoursesTable
//             .select { CoursesTable.formationId eq formationCode }
//             .map { it.toCourseDTO() }
//     }

//     fun createCourse(req: CourseWriteRequest): CourseDTO = transaction {
//         val newId = CoursesTable.insertAndGetId { row ->
//             row[courseCode] = req.courseCode
//             row[title] = req.title
//             row[credits] = req.credits
//             row[formationId] = req.formationId
//             row[blocId] = req.blocId
//             row[periods] = req.periods
//             row[detailsUrl] = req.detailsUrl
//             row[language] = req.language
//             row[program] = req.program
//             row[mandatory] = req.mandatory
//         }.value

//         CoursesTable
//             .select { CoursesTable.id eq newId }
//             .single()
//             .toCourseDTO()
//     }

//     fun updateCourse(id: Int, req: CourseWriteRequest): CourseDTO? = transaction {
//         val updated = CoursesTable.update({ CoursesTable.id eq id }) { row ->
//             row[courseCode] = req.courseCode
//             row[title] = req.title
//             row[credits] = req.credits
//             row[formationId] = req.formationId
//             row[blocId] = req.blocId
//             row[periods] = req.periods
//             row[detailsUrl] = req.detailsUrl
//             row[language] = req.language
//             row[program] = req.program
//             row[mandatory] = req.mandatory
//         }
//         if (updated == 0) return@transaction null

//         CoursesTable
//             .select { CoursesTable.id eq id }
//             .singleOrNull()
//             ?.toCourseDTO()
//     }

//     fun deleteCourse(id: Int): Boolean = transaction {
//         CoursesTable.deleteWhere { CoursesTable.id eq id } > 0
//     }

//     // =========================
//     // COURSE DETAILS (FICHES)
//     // =========================

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

//     fun getCourseDetailsByCourseId(courseId: String): CourseDetailsDTO? = transaction {
//         CourseDetailsTable
//             .select { CourseDetailsTable.courseId eq courseId }
//             .singleOrNull()
//             ?.toCourseDetailsDTO()
//     }

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
//         }.value

//         CourseDetailsTable
//             .select { CourseDetailsTable.id eq newId }
//             .single()
//             .toCourseDetailsDTO()
//     }

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

//     fun deleteCourseDetails(id: Int): Boolean = transaction {
//         CourseDetailsTable.deleteWhere { CourseDetailsTable.id eq id } > 0
//     }

//     // =========================
//     // YEAR OPTIONS
//     // =========================

//     fun getAllYearOptions(): List<YearOptionDTO> = transaction {
//         YearOptionsTable
//             .selectAll()
//             .map { it.toYearOptionDTO() }
//     }

//     fun getYearOptionById(id: Int): YearOptionDTO? = transaction {
//         YearOptionsTable
//             .select { YearOptionsTable.id eq id }
//             .singleOrNull()
//             ?.toYearOptionDTO()
//     }

//     fun getYearOptionByCode(yearOptionId: String): YearOptionDTO? = transaction {
//         YearOptionsTable
//             .select { YearOptionsTable.yearOptionId eq yearOptionId }
//             .singleOrNull()
//             ?.toYearOptionDTO()
//     }

//     fun getYearOptionsForFormation(formationCode: String): List<YearOptionDTO> = transaction {
//         YearOptionsTable
//             .select { YearOptionsTable.formationIds like "%$formationCode%" }
//             .map { it.toYearOptionDTO() }
//     }

//     fun createYearOption(req: YearOptionWriteRequest): YearOptionDTO = transaction {
//         val newId = YearOptionsTable.insertAndGetId { row ->
//             row[yearOptionId] = req.yearOptionId
//             row[formationIds] = req.formationIds
//             row[blocId] = req.blocId
//         }.value

//         YearOptionsTable
//             .select { YearOptionsTable.id eq newId }
//             .single()
//             .toYearOptionDTO()
//     }

//     fun updateYearOption(id: Int, req: YearOptionWriteRequest): YearOptionDTO? = transaction {
//         val updated = YearOptionsTable.update({ YearOptionsTable.id eq id }) { row ->
//             row[yearOptionId] = req.yearOptionId
//             row[formationIds] = req.formationIds
//             row[blocId] = req.blocId
//         }
//         if (updated == 0) return@transaction null

//         YearOptionsTable
//             .select { YearOptionsTable.id eq id }
//             .singleOrNull()
//             ?.toYearOptionDTO()
//     }

//     fun deleteYearOption(id: Int): Boolean = transaction {
//         YearOptionsTable.deleteWhere { YearOptionsTable.id eq id } > 0
//     }

//     // =========================
//     // SERIES
//     // =========================

//     fun getAllSeries(): List<SeriesDTO> = transaction {
//         SeriesNameTable
//             .selectAll()
//             .map { it.toSeriesDTO() }
//     }

//     fun getSeriesById(id: Int): SeriesDTO? = transaction {
//         SeriesNameTable
//             .select { SeriesNameTable.id eq id }
//             .singleOrNull()
//             ?.toSeriesDTO()
//     }

//     fun getSeriesForYearOption(yearOptionCode: String): List<SeriesDTO> = transaction {
//         SeriesNameTable
//             .select { SeriesNameTable.yearOptionIds like "%$yearOptionCode%" }
//             .map { it.toSeriesDTO() }
//     }

//     fun createSeries(req: SeriesWriteRequest): SeriesDTO = transaction {
//         val newId = SeriesNameTable.insertAndGetId { row ->
//             row[seriesId] = req.seriesId
//             row[yearOptionIds] = req.yearOptionIds
//             row[formationIds] = req.formationIds
//         }.value

//         SeriesNameTable
//             .select { SeriesNameTable.id eq newId }
//             .single()
//             .toSeriesDTO()
//     }

//     fun updateSeries(id: Int, req: SeriesWriteRequest): SeriesDTO? = transaction {
//         val updated = SeriesNameTable.update({ SeriesNameTable.id eq id }) { row ->
//             row[seriesId] = req.seriesId
//             row[yearOptionIds] = req.yearOptionIds
//             row[formationIds] = req.formationIds
//         }
//         if (updated == 0) return@transaction null

//         SeriesNameTable
//             .select { SeriesNameTable.id eq id }
//             .singleOrNull()
//             ?.toSeriesDTO()
//     }

//     fun deleteSeries(id: Int): Boolean = transaction {
//         SeriesNameTable.deleteWhere { SeriesNameTable.id eq id } > 0
//     }

//     // =========================
//     // ROOMS
//     // =========================

//     fun getAllRooms(): List<RoomDTO> = transaction {
//         RoomsTable
//             .selectAll()
//             .map { it.toRoomDTO() }
//     }

//     fun getRoomById(id: Int): RoomDTO? = transaction {
//         RoomsTable
//             .select { RoomsTable.id eq id }
//             .singleOrNull()
//             ?.toRoomDTO()
//     }

//     fun getRoomByCode(roomCode: String): RoomDTO? = transaction {
//         RoomsTable
//             .select { RoomsTable.roomId eq roomCode }
//             .singleOrNull()
//             ?.toRoomDTO()
//     }

//     fun createRoom(req: RoomWriteRequest): RoomDTO = transaction {
//         val newId = RoomsTable.insertAndGetId { row ->
//             row[roomId] = req.roomId
//             row[type] = req.type
//             row[batiment] = req.batiment
//             row[etage] = req.etage
//         }.value

//         RoomsTable
//             .select { RoomsTable.id eq newId }
//             .single()
//             .toRoomDTO()
//     }

//     fun updateRoom(id: Int, req: RoomWriteRequest): RoomDTO? = transaction {
//         val updated = RoomsTable.update({ RoomsTable.id eq id }) { row ->
//             row[roomId] = req.roomId
//             row[type] = req.type
//             row[batiment] = req.batiment
//             row[etage] = req.etage
//         }
//         if (updated == 0) return@transaction null

//         RoomsTable
//             .select { RoomsTable.id eq id }
//             .singleOrNull()
//             ?.toRoomDTO()
//     }

//     fun deleteRoom(id: Int): Boolean = transaction {
//         RoomsTable.deleteWhere { RoomsTable.id eq id } > 0
//     }

//     // =========================
//     // SOUS-COURSES
//     // =========================

//     fun getAllSousCourses(): List<SousCourseDTO> = transaction {
//         SousCoursesTable
//             .selectAll()
//             .map { it.toSousCourseDTO() }
//     }

//     fun getSousCourseById(id: Int): SousCourseDTO? = transaction {
//         SousCoursesTable
//             .select { SousCoursesTable.id eq id }
//             .singleOrNull()
//             ?.toSousCourseDTO()
//     }

//     fun getSousCoursesForCourse(courseId: String): List<SousCourseDTO> = transaction {
//         SousCoursesTable
//             .select { SousCoursesTable.courseId eq courseId }
//             .map { it.toSousCourseDTO() }
//     }

//     fun createSousCourse(req: SousCourseWriteRequest): SousCourseDTO = transaction {
//         val newId = SousCoursesTable.insertAndGetId { row ->
//             row[sousCourseId] = req.sousCourseId
//             row[courseId] = req.courseId
//             row[title] = req.title
//             row[hoursQ1] = req.hoursQ1
//             row[hoursQ2] = req.hoursQ2
//             row[teachersIds] = req.teachersIds
//             row[language] = req.language
//         }.value

//         SousCoursesTable
//             .select { SousCoursesTable.id eq newId }
//             .single()
//             .toSousCourseDTO()
//     }

//     fun updateSousCourse(id: Int, req: SousCourseWriteRequest): SousCourseDTO? = transaction {
//         val updated = SousCoursesTable.update({ SousCoursesTable.id eq id }) { row ->
//             row[sousCourseId] = req.sousCourseId
//             row[courseId] = req.courseId
//             row[title] = req.title
//             row[hoursQ1] = req.hoursQ1
//             row[hoursQ2] = req.hoursQ2
//             row[teachersIds] = req.teachersIds
//             row[language] = req.language
//         }
//         if (updated == 0) return@transaction null

//         SousCoursesTable
//             .select { SousCoursesTable.id eq id }
//             .singleOrNull()
//             ?.toSousCourseDTO()
//     }

//     fun deleteSousCourse(id: Int): Boolean = transaction {
//         SousCoursesTable.deleteWhere { SousCoursesTable.id eq id } > 0
//     }
// }

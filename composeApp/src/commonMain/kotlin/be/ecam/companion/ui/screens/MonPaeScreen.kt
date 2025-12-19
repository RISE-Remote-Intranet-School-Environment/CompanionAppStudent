@file:Suppress("DEPRECATION")

package be.ecam.companion.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.PaeCourse
import be.ecam.companion.data.PaeDatabase
import be.ecam.companion.data.PaeRecord
import be.ecam.companion.data.PaeRepository
import be.ecam.companion.data.PaeStudent
import be.ecam.companion.data.PaeComponent
import be.ecam.companion.data.PaeSessions
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.buildBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

@Serializable
private data class MyCoursesResponse(val courses: List<String>)

// 🔥 NOUVEAU : DTO pour les détails de cours du catalogue
@Serializable
private data class CourseDetailDto(
    val id: Int? = null,
    @SerialName("courseId") val courseId: String,
    @SerialName("courseRaccourciId") val courseRaccourciId: String? = null,
    val title: String? = null,
    val credits: Int? = null,
    val periods: String? = null
)

private const val COURSE_WEIGHT = 2f
private val ECTS_CELL_WIDTH = 60.dp
private val SCORE_CELL_WIDTH = 50.dp
private val jsonIgnoreUnknown = Json { ignoreUnknownKeys = true }

@Composable
fun MonPaeScreen(
    modifier: Modifier = Modifier,
    userIdentifier: String,
    authToken: String? = null,
    onContextChange: (String?) -> Unit = {}
) {
    val settingsRepo = koinInject<SettingsRepository>()
    val httpClient = koinInject<HttpClient>()
    val host = settingsRepo.getServerHost()
    val port = settingsRepo.getServerPort()
    val token = authToken?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
    val detailScrollState = rememberScrollState()
    var loadError by remember { mutableStateOf<String?>(null) }

    // 🔥 Charger les cours sélectionnés manuellement
    var userSelectedCourses by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 🔥 NOUVEAU : Catalogue de cours pour enrichir les infos
    var courseCatalog by remember { mutableStateOf<Map<String, CourseDetailDto>>(emptyMap()) }

    val paeDatabase by produceState<PaeDatabase?>(initialValue = null) {
        loadError = null
        value = try {
            val baseUrl = buildBaseUrl(host, port)
            
            // 🔥 Charger le catalogue de cours
            try {
                val catalogResponse = httpClient.get("$baseUrl/api/courses") {
                    token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                    header(HttpHeaders.Accept, "application/json")
                }
                if (catalogResponse.status.isSuccess()) {
                    val courses: List<CourseDetailDto> = catalogResponse.body()
                    courseCatalog = courses.associateBy { it.courseId.lowercase() }
                }
            } catch (e: Exception) {
                println("Erreur chargement catalogue: ${e.message}")
            }
            
            // Charger les cours sélectionnés manuellement
            try {
                val response = httpClient.get("$baseUrl/api/my-courses") {
                    token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                    header(HttpHeaders.Accept, "application/json")
                }
                if (response.status.isSuccess()) {
                    val body: MyCoursesResponse = response.body()
                    userSelectedCourses = body.courses
                }
            } catch (e: Exception) {
                println("Erreur chargement cours sélectionnés: ${e.message}")
            }
            
            loadPaeFromServer(
                client = httpClient,
                baseUrl = baseUrl,
                token = token
            )
        } catch (t: Throwable) {
            loadError = t.message ?: "Impossible de charger le PAE"
            null
        }
    }

    val students = paeDatabase?.students.orEmpty()

    var selectedStudentId by remember { mutableStateOf<String?>(null) }

    // Initialisation de la sélection
    LaunchedEffect(students, userIdentifier) {
        val matchingStudents = students.filter {
            it.email.equals(userIdentifier, ignoreCase = true) ||
            it.username.equals(userIdentifier, ignoreCase = true)
        }

        if (matchingStudents.isNotEmpty()) {
            selectedStudentId = matchingStudents.first().studentId ?: matchingStudents.first().username
        } else {
            selectedStudentId = "NO_PAE_FOUND"
        }
    }

    // 🔥 CORRECTION : Récupérer TOUS les PAE liés à l'étudiant sélectionné (par email)
    val targetStudents = remember(students, selectedStudentId, userIdentifier) {
        if (selectedStudentId == "NO_PAE_FOUND") {
            return@remember emptyList<PaeStudent>()
        }
        
        val primaryMatch = students.firstOrNull { 
            it.studentId == selectedStudentId || it.username == selectedStudentId 
        }
        
        if (primaryMatch == null) {
            return@remember emptyList<PaeStudent>()
        }

        val targetEmail = primaryMatch.email
        if (targetEmail.isNullOrBlank()) {
            listOf(primaryMatch)
        } else {
            students.filter { student -> 
                student.email?.equals(targetEmail, ignoreCase = true) == true 
            }
        }
    }

    val selectedStudent = targetStudents.firstOrNull()

    val sortedRecords = remember(targetStudents) {
        targetStudents
            .flatMap { it.records }
            .distinctBy { it.catalogYear ?: it.academicYearLabel }
            .sortedByDescending { it.catalogYear ?: it.academicYearLabel ?: "" }
    }

    var selectedCatalogYear by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sortedRecords) {
        if (selectedCatalogYear == null && sortedRecords.isNotEmpty()) {
            selectedCatalogYear = sortedRecords.first().catalogYear ?: sortedRecords.first().academicYearLabel
        }
    }

    val selectedRecord = sortedRecords.firstOrNull { 
        it.catalogYear == selectedCatalogYear || it.academicYearLabel == selectedCatalogYear 
    }

    LaunchedEffect(selectedRecord) {
        onContextChange(selectedRecord?.catalogYear ?: selectedRecord?.academicYearLabel)
    }

    // 🔥 NOUVEAU : Créer un "faux" record PAE à partir des cours sélectionnés
    val manualPaeRecord: PaeRecord? = remember(userSelectedCourses, courseCatalog) {
        if (userSelectedCourses.isEmpty()) return@remember null
        
        val manualCourses = userSelectedCourses.map { courseCode ->
            val catalogInfo = courseCatalog[courseCode.lowercase()]
            PaeCourse(
                code = courseCode,
                title = catalogInfo?.title ?: courseCode,
                ects = catalogInfo?.credits,
                sessions = PaeSessions()
            )
        }
        
        PaeRecord(
            catalogYear = "2024-2025", // Année courante
            academicYearLabel = "Sélection manuelle",
            program = "Cours sélectionnés",
            formationSlug = null,
            block = "Personnel",
            courses = manualCourses
        )
    }

    // 🔥 NOUVEAU : Créer un "faux" étudiant pour l'affichage
    val manualStudent: PaeStudent? = remember(userIdentifier, manualPaeRecord) {
        if (manualPaeRecord == null) return@remember null
        
        PaeStudent(
            studentId = "manual",
            username = userIdentifier.substringBefore("@"),
            email = userIdentifier,
            records = listOf(manualPaeRecord)
        )
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when {
            loadError != null -> Box(
                modifier = Modifier.fillMaxSize(), 
                contentAlignment = Alignment.Center
            ) {
                Text(loadError ?: "Erreur inconnue", color = MaterialTheme.colorScheme.error)
            }
            
            paeDatabase == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            
            // 🔥 MODIFIÉ : Si pas de PAE officiel MAIS des cours sélectionnés -> afficher comme un PAE
            selectedStudentId == "NO_PAE_FOUND" || targetStudents.isEmpty() -> {
                if (manualStudent != null && manualPaeRecord != null) {
                    // 🔥 Afficher le PAE manuel avec le même style que le PAE officiel
                    ManualPaeContent(
                        student = manualStudent,
                        record = manualPaeRecord,
                        scrollState = detailScrollState
                    )
                } else {
                    // Vraiment aucun cours
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                "Aucun cours trouvé",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                "Utilisez la page d'accueil pour sélectionner vos cours.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            selectedStudent == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Sélectionnez un étudiant pour voir son PAE")
            }
            
            else -> {
                // PAE officiel existant - code existant...
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWide = maxWidth > 800.dp
                    if (isWide) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .width(320.dp)
                                    .fillMaxHeight()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                PaeHeaderCard(selectedStudent)
                                YearSelector(
                                    records = sortedRecords,
                                    selectedRecord = selectedRecord,
                                    onSelect = { selectedCatalogYear = it }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(detailScrollState)
                                    .padding(16.dp)
                            ) {
                                PaeDetailPane(
                                    record = selectedRecord,
                                    student = selectedStudent,
                                    onPrev = {
                                        val idx = sortedRecords.indexOf(selectedRecord)
                                        if (idx < sortedRecords.lastIndex) {
                                            selectedCatalogYear = sortedRecords[idx + 1].catalogYear
                                                ?: sortedRecords[idx + 1].academicYearLabel
                                        }
                                    },
                                    onNext = {
                                        val idx = sortedRecords.indexOf(selectedRecord)
                                        if (idx > 0) {
                                            selectedCatalogYear = sortedRecords[idx - 1].catalogYear
                                                ?: sortedRecords[idx - 1].academicYearLabel
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(detailScrollState)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PaeHeaderCard(selectedStudent)
                            YearSelector(
                                records = sortedRecords,
                                selectedRecord = selectedRecord,
                                onSelect = { selectedCatalogYear = it }
                            )
                            PaeDetailPane(
                                record = selectedRecord,
                                student = selectedStudent,
                                onPrev = {
                                    val idx = sortedRecords.indexOf(selectedRecord)
                                    if (idx < sortedRecords.lastIndex) {
                                        selectedCatalogYear = sortedRecords[idx + 1].catalogYear
                                            ?: sortedRecords[idx + 1].academicYearLabel
                                    }
                                },
                                onNext = {
                                    val idx = sortedRecords.indexOf(selectedRecord)
                                    if (idx > 0) {
                                        selectedCatalogYear = sortedRecords[idx - 1].catalogYear
                                            ?: sortedRecords[idx - 1].academicYearLabel
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 🔥 NOUVEAU : Composable pour afficher le PAE manuel (même style que l'officiel)
@Composable
private fun ManualPaeContent(
    student: PaeStudent,
    record: PaeRecord,
    scrollState: androidx.compose.foundation.ScrollState
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 800.dp
        
        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ManualPaeHeaderCard(student)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    ManualPaeDetailPane(record = record)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ManualPaeHeaderCard(student)
                ManualPaeDetailPane(record = record)
            }
        }
    }
}

// 🔥 NOUVEAU : Header card pour PAE manuel
@Composable
private fun ManualPaeHeaderCard(student: PaeStudent) {
    val gradient = Brush.linearGradient(
        listOf(
            Color(0xFFFF9800), // Orange pour distinguer du PAE officiel
            Color(0xFFF57C00)
        )
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
        ) {
            // 🔥 Badge "Non officiel"
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "PAE NON OFFICIEL",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // 🔥 CORRECTION : Utiliser orEmpty() pour éviter les nulls
            Text(
                student.username.orEmpty().ifBlank { "Utilisateur" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // 🔥 CORRECTION : Utiliser orEmpty() pour éviter les nulls
            Text(
                student.email.orEmpty().ifBlank { "Email non disponible" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                "Cours sélectionnés manuellement",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

// 🔥 NOUVEAU : Detail pane pour PAE manuel
@Composable
private fun ManualPaeDetailPane(record: PaeRecord) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats
        ManualStatsStrip(record)
        
        // Table des cours
        CoursesTable(record.courses)
    }
}

// 🔥 NOUVEAU : Stats strip pour PAE manuel
@Composable
private fun ManualStatsStrip(record: PaeRecord) {
    val totalEcts = record.courses.sumOf { it.ects ?: 0 }
    val courseCount = record.courses.size
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 🔥 Utilisation de orEmpty() ou valeurs par défaut pour éviter les null
        ManualStatCard(
            title = "Cours",
            value = courseCount.toString(),
            modifier = Modifier.weight(1f)
        )
        ManualStatCard(
            title = "ECTS",
            value = totalEcts.toString(),
            modifier = Modifier.weight(1f)
        )
        ManualStatCard(
            title = "Type",
            value = "Manuel",
            modifier = Modifier.weight(1f)
        )
    }
}

// 🔥 NOUVEAU : Version locale de StatCard pour éviter les conflits de types
@Composable
private fun ManualStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Serializable
private data class PaeStudentDto(
    val id: Int? = null,
    @SerialName("studentId") val studentId: Int,
    @SerialName("studentName") val studentName: String,
    val email: String,
    val role: String? = null,
    val program: String? = null,
    @SerialName("enrolYear") val enrolYear: Int? = null,
    @SerialName("formationId") val formationId: String? = null,
    @SerialName("blocId") val blocId: String? = null,
    @SerialName("courseIds") val courseIds: String? = null
)

@Serializable
private data class NotesStudentDto(
    val id: Int? = null,
    val studentId: Int,
    val academicYear: String,
    val formationId: String,
    val blocId: String,
    val courseId: String,
    val courseTitle: String,
    val courseEcts: Double,
    val coursePeriod: String,
    val courseId1: String? = null,
    val courseSessionJan: Double? = null,
    val courseSessionJun: Double? = null,
    val courseSessionSep: Double? = null,
    val componentCode: String? = null,
    val componentTitle: String? = null,
    val componentWeight: Double? = null,
    val componentSessionJan: Double? = null,
    val componentSessionJun: Double? = null,
    val componentSessionSep: Double? = null
)

@Serializable
private data class SousCourseDto(
    @SerialName("sousCourseId") val sousCourseId: String,
    @SerialName("courseId") val courseId: String,
    val title: String,
    @SerialName("hoursQ1") val hoursQ1: String,
    @SerialName("hoursQ2") val hoursQ2: String
)

@Serializable
private data class CourseMetaDto(
    @SerialName("courseId") val courseId: String,
    @SerialName("courseRaccourciId") val courseRaccourciId: String? = null,
    val title: String? = null,
    val credits: Int? = null,
    val periods: String? = null
)

@Serializable
private data class BlocMetaDto(
    @SerialName("blocId") val blocId: String,
    val name: String
)

private fun parseYearStart(academicYear: String?): Int? {
    if (academicYear.isNullOrBlank()) return null
    val firstPart = academicYear.split('-', '/', ' ').firstOrNull()?.filter { it.isDigit() }
    return firstPart?.takeIf { it.length >= 4 }?.toIntOrNull()
}

private fun latestAcademicYear(notes: List<NotesStudentDto>, enrolYear: Int?): String? {
    val notedYears = notes.mapNotNull { it.academicYear }
    val latestFromNotes = notedYears.maxWithOrNull(
        compareBy<String> { parseYearStart(it) ?: Int.MIN_VALUE }.thenBy { it }
    )
    return latestFromNotes ?: enrolYear?.toString()
}

suspend fun loadPaeFromServer(
    client: HttpClient,
    baseUrl: String,
    token: String?
): PaeDatabase {
    val bearer = token?.takeIf { it.isNotBlank() }
    val students: List<PaeStudentDto> = client.get("$baseUrl/api/pae-students") {
        bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        header(HttpHeaders.Accept, "application/json")
    }.body<List<PaeStudentDto>>()

    // Charger les notes pour enrichir ECTS/notes/composants
    val notesByStudent: Map<Int, List<NotesStudentDto>> = students.associate { dto ->
        val notes: List<NotesStudentDto> = runCatching {
            client.get("$baseUrl/api/notes-students/by-student/${dto.studentId}") {
                bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                header(HttpHeaders.Accept, "application/json")
            }.body<List<NotesStudentDto>>()
        }.getOrElse { emptyList<NotesStudentDto>() }
        dto.studentId to notes
    }

    // Métadonnées cours et blocs pour enrichir l'affichage
    val coursesMeta: Map<String, CourseMetaDto> = runCatching {
        client.get("$baseUrl/api/courses") {
            bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            header(HttpHeaders.Accept, "application/json")
        }.body<List<CourseMetaDto>>()
    }.getOrElse { emptyList() }.associateBy { it.courseId.lowercase() }

    val sousCoursesByCourse: Map<String, List<SousCourseDto>> = runCatching {
        client.get("$baseUrl/api/sous-courses") {
            bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            header(HttpHeaders.Accept, "application/json")
        }.body<List<SousCourseDto>>()
    }.getOrElse { emptyList() }.groupBy { it.courseId.lowercase() }

    val blocNameById: Map<String, String> = runCatching {
        client.get("$baseUrl/api/blocs") {
            bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            header(HttpHeaders.Accept, "application/json")
        }.body<List<BlocMetaDto>>()
    }.getOrElse { emptyList() }.associate { it.blocId.lowercase() to it.name }

    val mappedStudents = students.map { dto ->
        val notes = notesByStudent[dto.studentId].orEmpty()
        val blocName = dto.blocId?.let { blocNameById[it.lowercase()] ?: it }

        // Construire un record par année de notes si disponibles, sinon fallback enrolYear
        val records: List<PaeRecord> = notes
            .groupBy { it.academicYear }
            .filterKeys { !it.isNullOrBlank() }
            .entries
            .sortedByDescending { parseYearStart(it.key) ?: Int.MIN_VALUE }
            .map { (year, yearNotes) ->
                PaeRecord(
                    program = dto.program,
                    academicYearLabel = year,
                    catalogYear = year,
                    formationSlug = dto.formationId,
                    formationId = null,
                    block = blocName,
                    courses = buildCoursesFromNotes(dto.courseIds, yearNotes, coursesMeta, sousCoursesByCourse)
                )
            }
            .ifEmpty {
                val recordYear = dto.enrolYear?.toString()
                listOf(
                    PaeRecord(
                        program = dto.program,
                        academicYearLabel = recordYear,
                        catalogYear = recordYear,
                        formationSlug = dto.formationId,
                        formationId = null,
                        block = blocName,
                        courses = buildCoursesFromNotes(dto.courseIds, notes, coursesMeta, sousCoursesByCourse)
                    )
                )
            }

        PaeStudent(
            studentName = dto.studentName,
            studentId = dto.studentId.toString(),
            role = dto.role,
            username = dto.email.substringBefore("@"),
            email = dto.email,
            password = null,
            records = records
        )
    }

    return PaeDatabase(students = mappedStudents)
}

private fun buildCoursesFromNotes(
    courseIdsRaw: String?,
    notes: List<NotesStudentDto>,
    courseMeta: Map<String, CourseMetaDto>,
    sousCoursesByCourse: Map<String, List<SousCourseDto>>
): List<PaeCourse> {
    // Start from courseIds list if provided; otherwise derive from notes
    val fromList: Set<String> = courseIdsRaw
        ?.split(';', ',', '|')
        ?.mapNotNull { it.trim().takeIf { it.isNotEmpty() } }
        ?.toSet()
        ?: emptySet()

    val noteCourses: Map<String, List<NotesStudentDto>> = notes.groupBy { it.courseId }

    val allCodes: List<String> = if (fromList.isNotEmpty()) fromList.toList() else noteCourses.keys.toList()

    return allCodes.map { code ->
        val entries = noteCourses[code].orEmpty()
        val meta = courseMeta[code.lowercase()]
        val base = entries.firstOrNull()
        val componentsFromNotes = entries.mapNotNull { entry ->
            entry.componentCode?.takeIf { it.isNotBlank() }?.let {
                PaeComponent(
                    code = it,
                    title = entry.componentTitle,
                    weight = entry.componentWeight?.toString(),
                    sessions = PaeSessions(
                        jan = entry.componentSessionJan?.toString(),
                        jun = entry.componentSessionJun?.toString(),
                        sep = entry.componentSessionSep?.toString()
                    )
                )
            }
        }
        val components = if (componentsFromNotes.isNotEmpty()) {
            componentsFromNotes
        } else {
            sousCoursesByCourse[code.lowercase()].orEmpty().map { sc ->
                val weight = listOf(sc.hoursQ1, sc.hoursQ2)
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")
                    .ifBlank { null }
                PaeComponent(
                    code = sc.sousCourseId,
                    title = sc.title,
                    weight = weight,
                    sessions = PaeSessions()
                )
            }
        }
        PaeCourse(
            code = code,
            title = base?.courseTitle ?: meta?.title,
            ects = base?.courseEcts?.toInt() ?: meta?.credits,
            period = base?.coursePeriod ?: meta?.periods,
            courseId = base?.courseId1?.toIntOrNull(),
            sessions = PaeSessions(
                jan = base?.courseSessionJan?.toString(),
                jun = base?.courseSessionJun?.toString(),
                sep = base?.courseSessionSep?.toString()
            ),
            components = components
        )
    }
}

// Pick the latest available numeric score (sep > jun > jan)
private fun PaeCourse.bestNumericScore(): Double? {
    fun PaeSessions.latestNumeric(): Double? {
        val ordered = listOf(sep, jun, jan)
        return ordered.firstNotNullOfOrNull { it?.toDoubleOrNull() }
    }
    return sessions.latestNumeric() ?: components.firstNotNullOfOrNull { it.sessions.latestNumeric() }
}

@Composable
private fun PaeHeaderCard(student: PaeStudent) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
        )
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = student.studentName ?: "Etudiant",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = student.role ?: student.email.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(label = "Student ID", value = student.studentId ?: "-")
                InfoChip(label = "Email", value = student.email ?: "-")
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    AssistChip(
        onClick = {},
        label = {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = BorderStroke(0.dp, Color.Transparent)
    )
}

@Composable
private fun YearSelector(
    records: List<PaeRecord>,
    selectedRecord: PaeRecord?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Parcours académique", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            records.forEach { record ->
                val isSelected = selectedRecord == record
                ElevatedCard(
                    modifier = Modifier
                        .widthIn(min = 210.dp)
                        .clickable {
                            onSelect(record.catalogYear ?: record.academicYearLabel)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = record.academicYearLabel ?: record.catalogYear ?: "Année inconnue",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = record.program?.let { "Programme $it" } ?: "Programme",
                            style = MaterialTheme.typography.bodySmall
                        )
                        record.block?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaeDetailPane(
    record: PaeRecord?,
    student: PaeStudent,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (record == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text("Aucune année sélectionnée")
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mon programme annuel et mes notes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Précédent") }
                IconButton(onClick = onNext) { Icon(Icons.Default.ArrowForwardIos, contentDescription = "Suivant") }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PaeMetaRow(
                    label = "Nom, Prénom",
                    value = student.studentName ?: "-"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaeMetaRow(
                        modifier = Modifier.weight(1f),
                        label = "Année académique",
                        value = record.academicYearLabel ?: record.catalogYear ?: "-"
                    )
                    PaeMetaRow(
                        modifier = Modifier.weight(1f),
                        label = "Programme",
                        value = record.program ?: "-"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaeMetaRow(
                        modifier = Modifier.weight(1f),
                        label = "Formation",
                        value = record.formationSlug ?: "Non précisé"
                    )
                    PaeMetaRow(
                        modifier = Modifier.weight(1f),
                        label = "Bloc",
                        value = record.block ?: "Ã¢Â€Â”"
                    )
                }
                StatsStrip(record)
                CoursesTable(record.courses)
                LegendBlock()
            }
        }
    }
}

@Composable
private fun PaeMetaRow(modifier: Modifier = Modifier, label: String, value: String) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatsStrip(record: PaeRecord) {
    val totalEcts = record.courses.sumOf { it.ects ?: 0 }
    var weightedSum = 0.0
    var ectsWithScore = 0
    record.courses.forEach { course ->
        val score = course.bestNumericScore()
        if (score != null && course.ects != null) {
            weightedSum += score * course.ects
            ectsWithScore += course.ects
        }
    }
    val average = if (ectsWithScore > 0) weightedSum / ectsWithScore else null
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(title = "Total ECTS", value = totalEcts.toString(), modifier = Modifier.weight(1f))
        StatCard(title = "Moyenne générale", value = average?.let { formatOneDecimal(it) } ?: "-", modifier = Modifier.weight(1f))
        StatCard(
            title = "Nombre de cours",
            value = record.courses.size.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatOneDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()}.0" else rounded.toString()
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CoursesTable(courses: List<PaeCourse>) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Cours", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
            Text("ECTS", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Jan", modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Juin", modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Sept", modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        HorizontalDivider()
        courses.forEach { course ->
            CourseRow(course)
            HorizontalDivider()
        }
    }
}

@Composable
private fun CourseRow(course: PaeCourse) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .weight(COURSE_WEIGHT)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = listOfNotNull(course.code?.uppercase(), course.title).joinToString(" - "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            EctsCell((course.ects ?: 0).toString())
            ScoreBadge(course.sessions.jan)
            ScoreBadge(course.sessions.jun)
            ScoreBadge(course.sessions.sep)
        }
        AnimatedVisibility(visible = course.components.size > 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                course.components.forEach { component ->
                    ComponentRow(component)
                }
            }
        }
    }
}

@Composable
private fun ComponentRow(component: PaeComponent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .weight(COURSE_WEIGHT)
                .padding(start = 8.dp, end = 8.dp)
        ) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = listOfNotNull(component.code, component.title, component.weight).joinToString(" • "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            )
        }
        EctsCell("")
        ScoreBadge(component.sessions.jan)
        ScoreBadge(component.sessions.jun)
        ScoreBadge(component.sessions.sep)
    }
}

@Composable
private fun ScoreBadge(value: String?) {
    val text = value?.ifBlank { "-" } ?: "-"
    val tone = when {
        text.equals("inscrit(e)", ignoreCase = true) -> MaterialTheme.colorScheme.secondaryContainer
        text.toDoubleOrNull()?.let { it >= 10.0 } == true -> MaterialTheme.colorScheme.primaryContainer
        text.toDoubleOrNull() != null -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    }
    Box(
        modifier = Modifier.width(SCORE_CELL_WIDTH),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = tone),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun EctsCell(value: String) {
    Box(
        modifier = Modifier.width(ECTS_CELL_WIDTH),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(value, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LegendBlock() {
    val legends = listOf(
        "P - Examen partiel",
        "Y - Deuxième inscription",
        "R - Report de note de la session précédente",
        "A - Cours retiré",
        "V - Evaluation satisfaisante (la note ne compte pas)",
        "D - Dispense",
        "I - Première inscription",
        "J - Report de note de janvier vers septembre",
        "Z - Note pas encore disponible (à venir)",
        "T - Note résultant d'un test",
        "W - Evaluation non satisfaisante (la note ne compte pas)",
        "X - Note obtenue à l'extérieur"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Légende", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            legends.forEach { item ->
                AssistChip(onClick = {}, label = { Text(item) })
            }
        }
    }
}

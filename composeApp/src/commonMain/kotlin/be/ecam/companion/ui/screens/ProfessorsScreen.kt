package be.ecam.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.http.Url
import be.ecam.companion.data.ProfessorCatalogRepository
import be.ecam.companion.data.Professor
import be.ecam.companion.data.ProfessorDatabase
import be.ecam.companion.data.ProfessorCourse
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.buildBaseUrl
import be.ecam.companion.ui.CourseRef
import be.ecam.companion.ui.CoursesFicheScreen
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
import kotlin.math.absoluteValue

/* --------------------------- SCREEN CONTAINER --------------------------- */

@Composable
fun ProfessorsScreen(
    modifier: Modifier = Modifier,
    resetTrigger: Int = 0,
    authToken: String? = null,
    onContextChange: (String?) -> Unit = {}
) {
    val httpClient = koinInject<HttpClient>()
    val settingsRepo = koinInject<SettingsRepository>()
    val host by settingsRepo.serverHostFlow.collectAsState(settingsRepo.getServerHost())
    val port by settingsRepo.serverPortFlow.collectAsState(settingsRepo.getServerPort())
    val bearerToken = remember(authToken) {
        authToken?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
    }
    val repository = remember(httpClient, host, port, bearerToken) {
        ProfessorCatalogRepository(
            client = httpClient,
            baseUrlProvider = { buildBaseUrl(host, port) },
            authTokenProvider = { bearerToken }
        )
    }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedSpeciality by remember { mutableStateOf<String?>(null) }
    var selectedCourseRef by remember { mutableStateOf<CourseRef?>(null) }

    val database by produceState<ProfessorDatabase?>(
        initialValue = null,
        resetTrigger,
        host,
        port,
        bearerToken,
        refreshKey
    ) {
        loadError = null
        value = try {
            val result = if (resetTrigger > 0 || refreshKey > 0) {
                repository.refresh()
            } else {
                repository.load()
            }
            result.database
        } catch (t: Throwable) {
            val reason = t.message?.takeIf { it.isNotBlank() } ?: t::class.simpleName ?: "erreur inconnue"
            loadError = "Impossible de charger l'annuaire depuis le serveur : $reason"
            null
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when {
            selectedCourseRef != null -> {
                CoursesFicheScreen(
                    courseRef = selectedCourseRef!!,
                    authToken = authToken,
                    onBack = { selectedCourseRef = null },
                    modifier = Modifier.fillMaxSize()
                )
            }

            database == null && loadError == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            database == null && loadError != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = loadError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { refreshKey++ }) {
                            Text("Reessayer")
                        }
                    }
                }
            }

            else -> {
                ProfessorsMainScreen(
                    database = database!!,
                    searchQuery = searchQuery,
                    selectedSpeciality = selectedSpeciality,
                    onSearch = { searchQuery = it },
                    onFilterChange = { selectedSpeciality = it },
                    onCourseSelected = { course ->
                        selectedCourseRef = CourseRef(course.code, course.detailsUrl)
                    }
                )
            }
        }
    }
}

/* --------------------------- SPECIALITY METADATA --------------------------- */

private val specialityLabels = mapOf(
    "FGS" to "Formation Générale et Santé",
    "GEA" to "Génie Électrique et Automatique",
    "GMT" to "Génie Mécanique et Thermique",
    "GEI" to "Génie Électronique et Informatique",
    "GCG" to "Génie Construction et Géomètre",
    "GLA" to "Gestion et Langues"
)

private val specialityIcons = mapOf(
    "FGS" to Icons.Filled.LocalHospital,
    "GEA" to Icons.Filled.Bolt,
    "GMT" to Icons.Filled.Build,
    "GEI" to Icons.Filled.Memory,
    "GCG" to Icons.Filled.LocationCity,
    "GLA" to Icons.Filled.Language
)

/* --------------------------- MAIN PAGE (FILTERS + GRID) --------------------------- */

@Composable
private fun ProfessorsMainScreen(
    database: ProfessorDatabase,
    searchQuery: TextFieldValue,
    selectedSpeciality: String?,
    onSearch: (TextFieldValue) -> Unit,
    onFilterChange: (String?) -> Unit,
    onCourseSelected: (ProfessorCourse) -> Unit
) {
    var selectedProfessor by remember { mutableStateOf<Professor?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        /* -------- SEARCH -------- */
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            label = { Text("Rechercher un professeur") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Spacer(Modifier.height(12.dp))

        /* -------- FILTERS -------- */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSpeciality == null,
                onClick = { onFilterChange(null) },
                label = { Text("Tous") }
            )

            specialityLabels.forEach { (code, label) ->
                FilterChip(
                    selected = selectedSpeciality == code,
                    onClick = { onFilterChange(code) },
                    label = { Text(code) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        /* -------- FILTER LOGIC -------- */
        val q = searchQuery.text.lowercase()
        val filtered = database.professors.filter { prof ->
            (selectedSpeciality == null || prof.speciality == selectedSpeciality) &&
                    (q.isBlank() ||
                            prof.firstName.lowercase().contains(q) ||
                            prof.lastName.lowercase().contains(q))
        }

        /* -------- RESPONSIVE GRID -------- */
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 260.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered) { prof ->
                ProfessorCard(
                    professor = prof,
                    onInfoClick = { selectedProfessor = prof },
                    onCourseClick = onCourseSelected
                )
            }
        }
    }

    selectedProfessor?.let { prof ->
        ProfessorDetailsDialog(
            professor = prof,
            onDismiss = { selectedProfessor = null },
            onCourseSelected = onCourseSelected
        )
    }
}

/* --------------------------- PROFESSOR CARD --------------------------- */

@Composable
private fun ProfessorCard(
    professor: Professor,
    onInfoClick: () -> Unit,
    onCourseClick: ((ProfessorCourse) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            /* -------- AVATAR + NAME -------- */
            Row(verticalAlignment = Alignment.CenterVertically) {

                val photoUrl = professor.photoUrl?.takeIf { it.isNotBlank() }
                if (photoUrl != null) {
                    KamelImage(
                        resource = { asyncPainterResource(Url(photoUrl)) },
                        contentDescription = "${professor.firstName} ${professor.lastName}",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        onLoading = {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        },
                        onFailure = {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(randomColorFor(professor.id)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${professor.firstName.take(1)}${professor.lastName.take(1)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(randomColorFor(professor.id)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${professor.firstName.take(1)}${professor.lastName.take(1)}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        "${professor.firstName} ${professor.lastName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        specialityLabels[professor.speciality] ?: professor.speciality,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            /* -------- EMAIL -------- */
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(professor.email)
            }

            /* -------- OFFICE -------- */
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Room, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(professor.office ?: "Non renseigné")
            }

            /* -------- COURSES SUMMARY -------- */
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
                Icon(Icons.Default.School, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("${professor.courses.size} cours enseignés")
            }

            if (professor.courses.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))

                /* -------- HIDE/SHOW BUTTON -------- */
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Masquer les cours" else "Voir les cours") }
                TextButton(onClick = onInfoClick) { Text("Fiche professeur") }
            } else {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onInfoClick) { Text("Fiche professeur") }
            }

            /* -------- COURSE LIST -------- */
            if (expanded) {
                val sorted = professor.courses.sortedBy { it.code.firstOrNull()?.digitToIntOrNull() ?: 9 }

                sorted.forEach { course ->

                    Spacer(Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when {
                                    onCourseClick != null -> onCourseClick(course)
                                    else -> course.detailsUrl?.let { uriHandler.openUri(it) }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${course.code.uppercase()} – ${course.title}",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessorDetailsDialog(
    professor: Professor,
    onDismiss: () -> Unit,
    onCourseSelected: (ProfessorCourse) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1200.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val photoUrl = professor.photoUrl?.takeIf { it.isNotBlank() }
                        if (photoUrl != null) {
                            KamelImage(
                                resource = { asyncPainterResource(Url(photoUrl)) },
                                contentDescription = "${professor.firstName} ${professor.lastName}",
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                onLoading = {
                                    Box(
                                        modifier = Modifier
                                            .size(86.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                },
                                onFailure = {
                                    Box(
                                        modifier = Modifier
                                            .size(86.dp)
                                            .clip(CircleShape)
                                            .background(randomColorFor(professor.id)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${professor.firstName.take(1)}${professor.lastName.take(1)}",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(CircleShape)
                                    .background(randomColorFor(professor.id)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${professor.firstName.take(1)}${professor.lastName.take(1)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${professor.firstName} ${professor.lastName}",
                                style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                specialityLabels[professor.speciality] ?: professor.speciality,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            professor.roleTitle?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Informations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        InfoRow(Icons.Default.Email, professor.email)
                        professor.phone?.takeIf { it.isNotBlank() && it != "1" }?.let {
                            InfoRow(Icons.Default.Phone, it)
                        }
                        InfoRow(Icons.Default.Room, professor.office ?: "Non renseigné", tint = MaterialTheme.colorScheme.tertiary)
                        professor.roleDetail?.takeIf { it.isNotBlank() }?.let {
                            InfoRow(Icons.Default.Work, it, tint = MaterialTheme.colorScheme.secondary)
                        }
                        professor.diplomas?.takeIf { it.isNotBlank() }?.let {
                            InfoRow(Icons.Default.School, it, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Cours enseignés (${professor.courses.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (professor.courses.isEmpty()) {
                            Text("Aucun cours associé", style = MaterialTheme.typography.bodySmall)
                        } else {
                            professor.courses.forEach { course ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    tonalElevation = 1.dp,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .clickable {
                                                onCourseSelected(course)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Book,
                                            null,
                                            modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(course.title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                            Text(course.code, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Fermer") }
                    }
                }
            }
        }
    )
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, tint: Color? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (tint != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/* --------------------------- RANDOM COLOR FOR AVATARS --------------------------- */

private fun randomColorFor(seed: Int): Color {
    val colors = listOf(
        Color(0xFF3F51B5),
        Color(0xFF009688),
        Color(0xFF9C27B0),
        Color(0xFFFF9800),
        Color(0xFF4CAF50),
        Color(0xFFE91E63)
    )
    return colors[seed.absoluteValue % colors.size]
}

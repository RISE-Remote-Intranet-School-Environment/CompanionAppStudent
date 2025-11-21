package be.ecam.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import be.ecam.companion.data.EcamProfessorsRepository
import be.ecam.companion.data.Professor
import be.ecam.companion.data.ProfessorDatabase
import kotlinx.coroutines.launch

@Composable
fun ProfessorsScreen(
    modifier: Modifier = Modifier,
    resetTrigger: Int = 0,
    onContextChange: (String?) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var database by remember { mutableStateOf<ProfessorDatabase?>(null) }
    var uiState by remember { mutableStateOf<ProfessorUiState>(ProfessorUiState.List) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(Unit) {
        scope.launch {
            database = EcamProfessorsRepository.load()
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            ProfessorUiState.List -> onContextChange(null)
            is ProfessorUiState.Detail -> onContextChange("${state.professor.firstName} ${state.professor.lastName}")
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            ProfessorUiState.List -> ProfessorListScreen(
                database = database,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onSelectProfessor = { uiState = ProfessorUiState.Detail(it) }
            )
            is ProfessorUiState.Detail -> ProfessorDetailScreen(
                professor = state.professor,
                onBack = { uiState = ProfessorUiState.List }
            )
        }
    }
}

private sealed interface ProfessorUiState {
    data object List : ProfessorUiState
    data class Detail(val professor: Professor) : ProfessorUiState
}

@Composable
private fun ProfessorListScreen(
    database: ProfessorDatabase?,
    searchQuery: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    onSelectProfessor: (Professor) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Professeurs",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text("Recherche par nom ou cours") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        if (database == null) {
            CircularProgressIndicator()
            return@Column
        }

        val filtered = database.professors.filter {
            val query = searchQuery.text.trim().lowercase()
            query.isBlank() ||
                    it.firstName.lowercase().contains(query) ||
                    it.lastName.lowercase().contains(query)
        }

        val grouped = filtered.groupBy { it.speciality }

        grouped.forEach { (speciality, professors) ->
            Text(
                text = speciality,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            professors.forEach { prof ->
                ProfessorCard(prof, onClick = { onSelectProfessor(prof) })
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfessorCard(professor: Professor, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${professor.firstName} ${professor.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Email : ${professor.email}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Bureau : ${professor.office ?: "Non renseigné"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ProfessorDetailScreen(professor: Professor, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Button(onClick = onBack) {
            Text("← Retour")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "${professor.firstName} ${professor.lastName}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text("Spécialité : ${professor.speciality}")
        Text("Email : ${professor.email}")
        Text("Bureau : ${professor.office ?: "Non renseigné"}")
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Cours donnés :",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))

        if (professor.courses.isEmpty()) {
            Text("Aucun cours enregistré pour l’instant.")
        } else {
            professor.courses.forEach { course ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(course.title)
                    course.detailsUrl?.let { url ->
                        TextButton(onClick = { uriHandler.openUri(url) }) {
                            Text("Fiche")
                        }
                    }
                }
            }
        }
    }
}

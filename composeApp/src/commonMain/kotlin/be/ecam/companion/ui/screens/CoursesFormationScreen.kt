package be.ecam.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.Formation
import be.ecam.companion.data.FormationBlock
import be.ecam.companion.data.FormationCatalogRepository
import be.ecam.companion.data.FormationCatalogResult
import be.ecam.companion.data.FormationDatabase
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.buildBaseUrl
import be.ecam.companion.ui.CourseRef
import be.ecam.companion.ui.CoursesFicheScreen
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.http.Url

@Composable
fun CoursesFormationScreen(
    modifier: Modifier = Modifier,
    resetTrigger: Int = 0,
    authToken: String? = null,
    onContextChange: (String?) -> Unit = {},
    onCourseSelected: ((CourseRef) -> Unit)? = null,
    onOpenCourseCalendar: (String?, String?) -> Unit = { _, _ -> }
) {
    val httpClient = koinInject<HttpClient>()
    val settingsRepo = koinInject<SettingsRepository>()
    val host by settingsRepo.serverHostFlow.collectAsState(settingsRepo.getServerHost())
    val port by settingsRepo.serverPortFlow.collectAsState(settingsRepo.getServerPort())
    val bearerToken = remember(authToken) {
        authToken?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
    }
    val formationsRepo = remember(httpClient, host, port, bearerToken) {
        FormationCatalogRepository(
            client = httpClient,
            baseUrlProvider = { buildBaseUrl(host, port) },
            authTokenProvider = { bearerToken }
        )
    }

    var loadError by remember { mutableStateOf<String?>(null) }
    val catalogResult by produceState<FormationCatalogResult?>(
        initialValue = null,
        resetTrigger,
        host,
        port,
        bearerToken
    ) {
        loadError = null
        value = try {
            formationsRepo.load()
        } catch (t: Throwable) {
            val reason = t.message?.takeIf { it.isNotBlank() } ?: t::class.simpleName ?: "erreur inconnue"
            loadError = "Impossible de charger les formations depuis le serveur : $reason"
            null
        }
    }
    val database = catalogResult?.database
    val programs = remember(database) { database?.formations?.toProgramCards().orEmpty() }
    var uiState by remember { mutableStateOf<CoursesState>(CoursesState.ProgramList) }
    val scrollState = rememberScrollState()
    var selectedCourseRef by remember { mutableStateOf<CourseRef?>(null) }
    val blockSelection = remember { mutableStateMapOf<String, String>() }
    var lastSelectedBlockName by remember { mutableStateOf<String?>(null) }
    val selectBlock: (ProgramCardData, FormationBlock) -> Unit = { program, block ->
        blockSelection[program.formation.id] = block.name
        lastSelectedBlockName = block.name
        uiState = CoursesState.BlockDetail(program, block)
    }
    val selectProgram: (ProgramCardData) -> Unit = { program ->
        val blocks = program.formation.blocks.sortedByBlocOrder()
        val preferred = blockSelection[program.formation.id] ?: lastSelectedBlockName
        val targetBlock = program.formation.blocks.firstOrNull { it.name == preferred }
            ?: blocks.firstOrNull()
        uiState = if (targetBlock != null) {
            CoursesState.BlockDetail(program, targetBlock)
        } else {
            CoursesState.ProgramDetail(program)
        }
    }
    val selectedProgram = when (val state = uiState) {
        is CoursesState.ProgramDetail -> state.program
        is CoursesState.BlockDetail -> state.program
        else -> null
    }

    val headerTitle = when (val state = uiState) {
        CoursesState.ProgramList -> "Formations"
        is CoursesState.ProgramDetail -> "Formations - ${state.program.title}"
        is CoursesState.BlockDetail -> "Formations - ${state.program.title}"
    }
    val headerSubtitle = when (val state = uiState) {
        CoursesState.ProgramList -> "Toutes les formations a portee de main"
        is CoursesState.ProgramDetail -> "Choisis un bloc pour explorer les cours"
        is CoursesState.BlockDetail -> ""
    }

    val handlesDetailsInPlace = onCourseSelected == null
    val tableCourseSelection: (CourseRef) -> Unit = onCourseSelected ?: { courseRef ->
        selectedCourseRef = courseRef
    }
    val showingCourseDetails = handlesDetailsInPlace && selectedCourseRef != null

    LaunchedEffect(uiState, selectedCourseRef, onCourseSelected) {
        if (showingCourseDetails) {
            val codeLabel = selectedCourseRef?.code?.takeIf { it.isNotBlank() } ?: "Fiche"
            onContextChange("Fiche - $codeLabel")
        } else {
            when (val state = uiState) {
                CoursesState.ProgramList -> onContextChange(null)
                is CoursesState.ProgramDetail -> onContextChange(state.program.title)
                is CoursesState.BlockDetail -> onContextChange(state.program.title)
            }
        }
    }
    LaunchedEffect(resetTrigger) {
        selectedCourseRef = null
        uiState = CoursesState.ProgramList
        if (scrollState.value != 0) {
            scrollState.scrollTo(0)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        if (showingCourseDetails) {
            CoursesFicheScreen(
                modifier = Modifier.fillMaxSize(),
                courseRef = selectedCourseRef!!,
                onBack = { selectedCourseRef = null }
            )
        } else {
            // ON SÉPARE ICI : LazyColumn pour la liste, Column Scrollable pour les détails
            when (val state = uiState) {
                // CAS 1 : LA LISTE DES FORMATIONS (Optimisé avec LazyColumn)
                is CoursesState.ProgramList -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Le Titre (Header)
                        item {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.displayLarge,
                                textAlign = TextAlign.Center
                            )
                            if (headerSubtitle.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = headerSubtitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // 2. Texte d'intro et Erreurs
                        item {
                            IntroText(database)
                            Spacer(Modifier.height(24.dp))

                            if (database == null) {
                                if (loadError != null) {
                                    Text(
                                        text = loadError ?: "Erreur inconnue",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    CircularProgressIndicator()
                                }
                            } else if (programs.isEmpty()) {
                                Text(
                                    text = "Aucune formation trouvée.\n",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }

                        // 3. La Grille des programmes (Rendu optimisé)
                        if (programs.isNotEmpty()) {
                            items(programs.chunked(3)) { rowPrograms ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowPrograms.forEach { program ->
                                        ProgramCard(
                                            program = program,
                                            modifier = Modifier.weight(1f),
                                            onClick = { selectProgram(program) }
                                        )
                                    }
                                    // Remplissage pour garder l'alignement si la ligne n'est pas pleine
                                    repeat(3 - rowPrograms.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }

                // CAS 2 : LES DÉTAILS (On garde le scroll classique ici)
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header (identique mais dans une Column classique)
                        if (uiState !is CoursesState.BlockDetail) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.displayLarge,
                                textAlign = TextAlign.Center
                            )
                            if (headerSubtitle.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = headerSubtitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Sélecteur de formation et Hero Card
                        if (selectedProgram != null && uiState !is CoursesState.BlockDetail) {
                            FormationSelector(
                                programs = programs,
                                selectedProgram = selectedProgram,
                                onProgramSelected = selectProgram
                            )
                            Spacer(Modifier.height(12.dp))
                            FormationHeroCard(
                                program = selectedProgram,
                                year = database?.year,
                                totalCourses = selectedProgram.formation.blocks.sumOf { it.courses.size }
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        // Contenu spécifique (Blocs ou Détail du bloc)
                        when (state) {
                            is CoursesState.ProgramDetail -> {
                                ProgramBlocks(
                                    program = state.program,
                                    onBlockSelected = { block -> selectBlock(state.program, block) }
                                )
                            }
                            is CoursesState.BlockDetail -> {
                                CoursesFormationBlocScreen(
                                    program = state.program,
                                    block = state.block,
                                    programs = programs,
                                    databaseYear = database?.year,
                                    onFormationSelected = selectProgram,
                                    onBlockSelected = { block -> selectBlock(state.program, block) },
                                    onCourseSelected = tableCourseSelection,
                                    onOpenCourseCalendar = onOpenCourseCalendar
                                )
                            }
                            else -> {} 
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroText(database: FormationDatabase?) {
    val uriHandler = LocalUriHandler.current
    database?.let {
        val annotated = buildAnnotatedString {
            append("Programme ${it.year} - Source : ")
            val start = length
            append(it.source)
            addStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
                start,
                length
            )
            addStringAnnotation(tag = "URL", annotation = it.source, start = start, end = length)
        }
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri(it.source) }
        )
    }
}

@Composable
private fun ProgramGrid(
    programs: List<ProgramCardData>,
    onProgramSelected: (ProgramCardData) -> Unit
) {
    programs.chunked(3).forEach { rowPrograms ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rowPrograms.forEach { program ->
                ProgramCard(
                    program = program,
                    modifier = Modifier.weight(1f),
                    onClick = { onProgramSelected(program) }
                )
            }
            repeat(3 - rowPrograms.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FormationSelector(
    programs: List<ProgramCardData>,
    selectedProgram: ProgramCardData?,
    onProgramSelected: (ProgramCardData) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        programs.forEach { program ->
            OutlinedButton(
                onClick = { onProgramSelected(program) },
                modifier = Modifier.padding(horizontal = 4.dp),
                colors = if (program == selectedProgram) {
                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormationAvatar(program.formation.id)
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramCard(
    program: ProgramCardData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardHeight = 260.dp
    val imageHeight = 140.dp
    Card(
        modifier = modifier.height(cardHeight),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val imageShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            
            if (program.imageUrl != null) {
                KamelImage(
                    resource = asyncPainterResource(data = Url(program.imageUrl)),
                    contentDescription = program.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(imageShape),
                    onLoading = {
                        // Placeholder pendant le chargement
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    },
                    onFailure = {
                        // Fallback si l'image ne charge pas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .clip(imageShape)
                                .background(
                                    formationColors[program.formation.id]
                                        ?: MaterialTheme.colorScheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = program.title.take(2).uppercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(imageShape)
                        .background(
                            formationColors[program.formation.id]
                                ?: MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = program.title.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${program.formation.blocks.size} blocs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProgramBlocks(
    program: ProgramCardData,
    onBlockSelected: (FormationBlock) -> Unit,
    selectedBlock: FormationBlock? = null,
    showIntro: Boolean = true,
    inlineChips: Boolean = false
) {
    val sortedBlocks = program.formation.blocks.sortedByBlocOrder()

    if (showIntro) {
        Text(
            text = program.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        if (!inlineChips) {
            Text(
                text = "Selectionne un bloc pour consulter la liste des cours.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        program.formation.notes?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    } else if (!inlineChips) {
        Text(
            text = "Choisis un bloc",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }

    if (program.formation.blocks.isEmpty()) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Le detail des cours pour cette formation sera disponible prochainement.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        return
    }

    if (inlineChips) {
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = Int.MAX_VALUE
        ) {
            sortedBlocks.forEach { block ->
                BlockChip(
                    block = block,
                    selected = selectedBlock == block,
                    formationId = program.formation.id,
                    onClick = { onBlockSelected(block) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    } else {
        Spacer(Modifier.height(24.dp))
        sortedBlocks.chunked(2).forEach { blockRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                blockRow.forEach { block ->
                    BlockCard(
                        block = block,
                        selected = selectedBlock == block,
                        modifier = Modifier.weight(1f),
                        onClick = { onBlockSelected(block) }
                    )
                }
                if (blockRow.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockCard(
    block: FormationBlock,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = block.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "${block.courses.size} cours",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BlockChip(
    block: FormationBlock,
    selected: Boolean,
    formationId: String,
    onClick: () -> Unit
) {
    val formationColor = formationColors[formationId] ?: MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp),
        colors = if (selected) {
            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BlocAvatar(block.name, color = formationColor)
            Text(block.name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private sealed interface CoursesState {
    data object ProgramList : CoursesState
    data class ProgramDetail(val program: ProgramCardData) : CoursesState
    data class BlockDetail(val program: ProgramCardData, val block: FormationBlock) : CoursesState
}

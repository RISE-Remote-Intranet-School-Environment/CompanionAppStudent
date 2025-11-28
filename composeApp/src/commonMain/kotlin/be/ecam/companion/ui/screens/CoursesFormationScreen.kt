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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import org.koin.compose.koinInject

@Composable
fun CoursesFormationScreen(
    modifier: Modifier = Modifier,
    resetTrigger: Int = 0,
    onContextChange: (String?) -> Unit = {},
    onCourseSelected: ((CourseRef) -> Unit)? = null
) {
    val httpClient = koinInject<HttpClient>()
    val settingsRepo = koinInject<SettingsRepository>()
    val host by settingsRepo.serverHostFlow.collectAsState(settingsRepo.getServerHost())
    val port by settingsRepo.serverPortFlow.collectAsState(settingsRepo.getServerPort())
    val formationsRepo = remember(httpClient, host, port) {
        FormationCatalogRepository(httpClient) { buildBaseUrl(host, port) }
    }

    var loadError by remember { mutableStateOf<String?>(null) }
    val catalogResult by produceState<FormationCatalogResult?>(initialValue = null, key1 = resetTrigger, key2 = host, key3 = port) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )
                if (uiState !is CoursesState.ProgramList && headerSubtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = headerSubtitle,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
                loadError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                }
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
                when (val state = uiState) {
                    CoursesState.ProgramList -> {
                        IntroText(database)
                        Spacer(Modifier.height(24.dp))
                        if (database == null || programs.isEmpty()) {
                            if (loadError != null) {
                                Text(
                                    text = "Aucune donnee n'a pu etre chargee.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        } else {
                            ProgramGrid(
                                programs = programs,
                                onProgramSelected = selectProgram
                            )
                        }
                    }

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
                            onCourseSelected = tableCourseSelection
                        )
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
                AsyncImage(
                    model = program.imageUrl,
                    contentDescription = program.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(imageShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(imageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormationAvatar(program.formation.id)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = program.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = program.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormationHeroCard(
    program: ProgramCardData,
    year: String?,
    totalCourses: Int,
    block: FormationBlock? = null,
    modifier: Modifier = Modifier
) {
    val color = formationColors[program.formation.id] ?: MaterialTheme.colorScheme.primary
    val gradient = Brush.horizontalGradient(
        listOf(
            color.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 960.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FormationAvatar(program.formation.id)
            Column(modifier = Modifier.weight(1f)) {
                Text(program.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    program.description.ifBlank { "Explore les blocs, cours et credits de la formation." },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    block?.let {
                        InfoPill(icon = Icons.Filled.ViewWeek, label = "Bloc", value = it.name)
                        InfoPill(
                            icon = Icons.Filled.AutoGraph,
                            label = "Cours du bloc ${it.name}",
                            value = it.courses.size.toString()
                        )
                    } ?: run {
                        InfoPill(icon = Icons.Filled.ViewWeek, label = "Blocs", value = program.formation.blocks.size.toString())
                        InfoPill(icon = Icons.Filled.AutoGraph, label = "Cours", value = totalCourses.toString())
                        year?.let { InfoPill(icon = Icons.Filled.Timer, label = "Programme", value = it) }
                    }
                }
            }
        }
    }
}

@Composable
fun FormationAvatar(formationId: String, size: Dp = 44.dp) {
    val icon = formationIcons[formationId] ?: Icons.Filled.School
    val color = formationColors[formationId] ?: MaterialTheme.colorScheme.primary
    val bg = color.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
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
private fun InfoPill(icon: ImageVector, label: String, value: String) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
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
            BlocAvatar(block.name)
            Text(block.name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun BlocAvatar(name: String, size: Dp = 36.dp) {
    val number = Regex("""\d+""").find(name)?.value
    val color = number?.let { blocColors[it] } ?: MaterialTheme.colorScheme.primary
    val icon = number?.let { blocIcons[it] } ?: Icons.Filled.School
    val bg = color.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
    }
}

data class ProgramCardData(
    val formation: Formation,
    val description: String,
    val imageUrl: String?
) {
    val title: String get() = formation.name
}

private sealed interface CoursesState {
    data object ProgramList : CoursesState
    data class ProgramDetail(val program: ProgramCardData) : CoursesState
    data class BlockDetail(val program: ProgramCardData, val block: FormationBlock) : CoursesState
}

private val formationOrder = listOf(
    "automatisation",
    "construction",
    "electromecanique",
    "electronique",
    "geometre",
    "informatique",
    "ingenierie_sante",
    "ingenieur_industriel_commercial",
    "business_analyst"
)

private val formationDescriptions = mapOf(
    "automatisation" to "Robotique, capteurs intelligents et pilotage de chaines de production.",
    "construction" to "Structures durables, gestion de chantiers et materiaux innovants.",
    "electromecanique" to "Conversion d'energie, machines tournantes et maintenance.",
    "electronique" to "Systemes embarques, telecom et objets connectes.",
    "geometre" to "Topographie, BIM et modelisation 3D du territoire.",
    "informatique" to "Developpement logiciel, cloud et cybersecurite.",
    "ingenierie_sante" to "Dispositifs medicaux, bio-instrumentation et regulation.",
    "ingenieur_industriel_commercial" to "Double diplome ECAM-ICHEC en 6 ans oriente business.",
    "business_analyst" to "Passerelle entre utilisateurs et equipes de developpement."
)

private val formationIcons: Map<String, ImageVector> = mapOf(
    "automatisation" to Icons.Filled.Settings,
    "construction" to Icons.Filled.Apartment,
    "electromecanique" to Icons.Filled.Construction,
    "electronique" to Icons.Filled.Memory,
    "geometre" to Icons.Filled.Map,
    "informatique" to Icons.Filled.Computer,
    "ingenierie_sante" to Icons.Filled.HealthAndSafety,
    "ingenieur_industriel_commercial" to Icons.Filled.BusinessCenter,
    "business_analyst" to Icons.Filled.Assessment
)

private val formationColors: Map<String, Color> = mapOf(
    "automatisation" to Color(0xFF5C6BC0),
    "construction" to Color(0xFF8D6E63),
    "electromecanique" to Color(0xFF00897B),
    "electronique" to Color(0xFF7E57C2),
    "geometre" to Color(0xFF5C7F67),
    "informatique" to Color(0xFF3949AB),
    "ingenierie_sante" to Color(0xFF26A69A),
    "ingenieur_industriel_commercial" to Color(0xFFF9A825),
    "business_analyst" to Color(0xFF6D4C41)
)

private val blocColors: Map<String, Color> = mapOf(
    "1" to Color(0xFF7E57C2),
    "2" to Color(0xFF5C6BC0),
    "3" to Color(0xFF42A5F5),
    "4" to Color(0xFF26A69A),
    "5" to Color(0xFF66BB6A),
    "6" to Color(0xFFF9A825)
)

private val blocIcons: Map<String, ImageVector> = mapOf(
    "1" to Icons.AutoMirrored.Filled.TrendingUp,
    "2" to Icons.Filled.Build,
    "3" to Icons.Filled.EmojiEvents,
    "4" to Icons.Filled.WorkspacePremium,
    "5" to Icons.Filled.School,
    "6" to Icons.Filled.Science
)

fun List<Formation>.toProgramCards(): List<ProgramCardData> =
    this.sortedBy { formationOrder.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
        .map { formation ->
            val imageUrl = formation.imageUrl?.takeIf { it.isNotBlank() }
            val description = formationDescriptions[formation.id] ?: ""
            ProgramCardData(
                formation = formation,
                description = description,
                imageUrl = imageUrl
            )
        }

fun List<FormationBlock>.sortedByBlocOrder(): List<FormationBlock> {
    fun extractNumber(name: String): Int? =
        Regex("""\d+""").find(name)?.value?.toIntOrNull()

    return this.sortedWith(
        compareBy<FormationBlock> { extractNumber(it.name) ?: Int.MAX_VALUE }
            .thenBy { it.name }
    )
}

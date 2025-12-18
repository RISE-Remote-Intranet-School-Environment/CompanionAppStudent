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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
        CoursesState.ProgramList -> ""
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
                authToken = authToken,
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
                        // 1. Texte d'intro (le header title est maintenant intégré dedans)
                        item {
                            IntroText(database)
                            
                            Spacer(Modifier.height(16.dp))

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

                        // 2. La Grille des programmes (Rendu optimisé)
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
                                    authToken = authToken,
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val isWide = maxWidth > 700.dp

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isWide) {
                // MODE LARGE : Titre à gauche, tout le contenu à droite
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // GAUCHE : Titre "Formations" avec icône - plus grand
                    Column(
                        modifier = Modifier.width(230.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Formations",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // DROITE : Tout le texte descriptif
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("L'")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("ECAM")
                                }
                                append(" est un Institut Supérieur Industriel ayant pour objet la formation de ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Master en sciences industrielles")
                                }
                                append(" dans une des finalités ci-dessous :")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Les deux infos côte à côte
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MiniInfoCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Handshake,
                                title = "Partenariat ICHEC",
                                color = MaterialTheme.colorScheme.tertiary,
                                content = {
                                    Text(
                                        text = buildAnnotatedString {
                                            append("Double diplôme ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Ingénieur industriel et commercial") }
                                            append(" (6 ans) & Master ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Business Analyst") }
                                            append(" dont le but est d’établir des ponts entre les utilisateurs et les équipes de développement, d’accompagner les projets et de participer à la stratégie IT de l’entreprise.")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            MiniInfoCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Science,
                                title = "CERDECAM",
                                color = MaterialTheme.colorScheme.secondary,
                                content = {
                                    Text(
                                        text = buildAnnotatedString {
                                            append("Le Centre de Recherche de l’ECAM, le ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("CERDECAM") }
                                            append(" , organise également des Formations Continues.")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                // MODE MOBILE : Empilé verticalement
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Formations",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Pour tout savoir sur l'organisation des études.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniInfoCard(
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Handshake,
                            title = "Partenariat ICHEC",
                            color = MaterialTheme.colorScheme.tertiary,
                            content = {
                                Text(
                                    text = "Double diplôme Ingénieur industriel et commercial & Master Business Analyst.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        MiniInfoCard(
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Science,
                            title = "Formation Continue",
                            color = MaterialTheme.colorScheme.secondary,
                            content = {
                                Text(
                                    text = "Le CERDECAM organise également des Formations Continues.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniInfoCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(4.dp))
            content()
        }
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
    val cardHeight = 240.dp
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
                    resource = { asyncPainterResource(Url(program.imageUrl)) },
                    contentDescription = program.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(imageShape),
                    onLoading = { progress ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .clip(imageShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    onFailure = { exception ->
                        // Log pour debug
                        println("Kamel image load failed: ${exception.message}")
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
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = program.description.ifBlank { "Explore les blocs, cours et credits de la formation." },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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

@Composable
private fun DashboardHighlights() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val isWide = maxWidth > 850.dp // Breakpoint pour passer en mode horizontal

        if (isWide) {
            // MODE DESKTOP : 3 cartes côte à côte
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top // Important pour aligner les cartes en haut
            ) {
                HighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.School,
                    title = "Master Ingénieur",
                    accentColor = MaterialTheme.colorScheme.primary,
                    content = {
                        Text(
                            text = "Master en sciences industrielles dans l'une des finalités ci-dessous.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                HighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Handshake,
                    title = "Double Diplôme",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    content = {
                        Text(
                            text = buildAnnotatedString {
                                append("Partenariat ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("ICHEC") }
                                append(" : Ingénieur commercial (6 ans) & Master Business Analyst.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                HighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Science,
                    title = "Recherche & Continue",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    content = {
                        Text(
                            text = buildAnnotatedString {
                                append("Le ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("CERDECAM") }
                                append(" organise également des Formations Continues.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        } else {
            // MODE MOBILE : Un slider horizontal (LazyRow) ou une colonne compacte
            // Ici on choisit une colonne compacte mais stylisée pour prendre moins de place visuelle
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Master en sciences industrielles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Incluant double diplôme ICHEC et formations continues via le CERDECAM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.heightIn(min = 100.dp), // Hauteur fixe minimale pour uniformité
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

private sealed interface CoursesState {
    data object ProgramList : CoursesState
    data class ProgramDetail(val program: ProgramCardData) : CoursesState
    data class BlockDetail(val program: ProgramCardData, val block: FormationBlock) : CoursesState
}

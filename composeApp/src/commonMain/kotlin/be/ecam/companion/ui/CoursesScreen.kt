package be.ecam.companion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.EcamFormationsRepository
import be.ecam.companion.data.Formation
import be.ecam.companion.data.FormationBlock
import be.ecam.companion.data.FormationCourse
import be.ecam.companion.data.FormationDatabase
import companion.composeapp.generated.resources.Res
import companion.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@Composable
fun CoursesScreen(
    modifier: Modifier = Modifier,
    resetTrigger: Int = 0,
    onContextChange: (String?) -> Unit = {},
    onCourseSelected: (String) -> Unit = {} // 👈 ajoute ceci
) {
    val database by produceState<FormationDatabase?>(initialValue = null) {
        value = EcamFormationsRepository.load()
    }
    val programs = remember(database) { database?.formations?.toProgramCards().orEmpty() }
    var uiState by remember { mutableStateOf<CoursesState>(CoursesState.ProgramList) }
    val scrollState = rememberScrollState()
    val blockSelection = remember { mutableStateMapOf<String, String>() }
    var lastSelectedBlockName by remember { mutableStateOf<String?>(null) }
    val selectBlock: (ProgramCardData, FormationBlock) -> Unit = { program, block ->
        blockSelection[program.formation.id] = block.name
        lastSelectedBlockName = block.name
        uiState = CoursesState.BlockDetail(program, block)
    }
    val selectProgram: (ProgramCardData) -> Unit = { program ->
        val preferred = blockSelection[program.formation.id] ?: lastSelectedBlockName
        val targetBlock = program.formation.blocks.firstOrNull { it.name == preferred }
            ?: program.formation.blocks.firstOrNull()
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

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            CoursesState.ProgramList -> onContextChange(null)
            is CoursesState.ProgramDetail -> onContextChange(state.program.title)
            is CoursesState.BlockDetail -> onContextChange(state.program.title)
        }
    }
    LaunchedEffect(resetTrigger) {
        uiState = CoursesState.ProgramList
        if (scrollState.value != 0) {
            scrollState.scrollTo(0)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
            Spacer(Modifier.height(12.dp))
            if (selectedProgram != null) {
                FormationSelector(
                    programs = programs,
                    selectedProgram = selectedProgram,
                    onProgramSelected = selectProgram
                )
                Spacer(Modifier.height(16.dp))
            }
            when (val state = uiState) {
                CoursesState.ProgramList -> {
                    IntroText(database)
                    Spacer(Modifier.height(24.dp))
                    if (database == null || programs.isEmpty()) {
                        CircularProgressIndicator()
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
                    ProgramBlocks(
                        program = state.program,
                        selectedBlock = state.block,
                        showIntro = false,
                        inlineChips = true,
                        onBlockSelected = { block -> selectBlock(state.program, block) }
                    )
                    BlockDetails(
                        program = state.program,
                        block = state.block,
                        onCourseSelected = { code -> onCourseSelected(code) }
                    )
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
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.labelLarge
                )
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
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(program.image),
                contentDescription = program.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodyMedium
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
            program.formation.blocks.forEach { block ->
                BlockChip(
                    block = block,
                    selected = selectedBlock == block,
                    onClick = { onBlockSelected(block) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    } else {
        Spacer(Modifier.height(24.dp))
        program.formation.blocks.chunked(2).forEach { blockRow ->
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
private fun BlockDetails(
    program: ProgramCardData,
    block: FormationBlock,
    onCourseSelected: (String) -> Unit // 🔹 nouveau paramètre
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .requiredWidthIn(max = 860.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            TableHeader()
            Spacer(Modifier.height(8.dp))
            block.courses.forEachIndexed { index, course ->
                CourseRow(
                    course = course,
                    striped = index % 2 == 0,
                    onCourseSelected = onCourseSelected // 🔹 on envoie le code du cours
                )
            }
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Unité d'enseignement",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.width(creditColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Crédits",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.width(periodColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Périodes",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.width(actionColumnWidth))
    }
}


@Composable
private fun CourseRow(
    course: FormationCourse,
    striped: Boolean,
    onCourseSelected: (String) -> Unit
) {
    val backgroundColor = if (striped) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .clickable { onCourseSelected(course.code) }, // 🔹 clic sur toute la ligne
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(course.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(course.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(
            modifier = Modifier.width(creditColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(course.credits.formatCredits(), style = MaterialTheme.typography.bodyMedium)
        }
    }
    Spacer(Modifier.height(6.dp))
}
private data class ProgramCardData(
    val formation: Formation,
    val description: String,
    val image: DrawableResource
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

private val creditColumnWidth = 70.dp
private val periodColumnWidth = 120.dp
private val actionColumnWidth = 110.dp
private val actionButtonMinWidth = 88.dp

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

private val formationImages = mapOf(
    "automatisation" to Res.drawable.courses_automatisation,
    "construction" to Res.drawable.courses_construction,
    "electromecanique" to Res.drawable.courses_electromecanique,
    "electronique" to Res.drawable.courses_electronique,
    "geometre" to Res.drawable.courses_geometre,
    "informatique" to Res.drawable.courses_informatique,
    "ingenierie_sante" to Res.drawable.courses_sante,
    "ingenieur_industriel_commercial" to Res.drawable.courses_industriel_commercial,
    "business_analyst" to Res.drawable.courses_business_analyst
)

private fun List<Formation>.toProgramCards(): List<ProgramCardData> =
    this.sortedBy { formationOrder.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
        .mapNotNull { formation ->
            val image = formationImages[formation.id] ?: return@mapNotNull null
            val description = formationDescriptions[formation.id] ?: ""
            ProgramCardData(formation = formation, description = description, image = image)
        }

  private fun Double.formatCredits(): String {
      val rounded = (this * 100).roundToInt() / 100.0
      return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
  }
@Composable
private fun BlockChip(
    block: FormationBlock,
    selected: Boolean,
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
        Text(block.name, style = MaterialTheme.typography.labelLarge)
    }
}




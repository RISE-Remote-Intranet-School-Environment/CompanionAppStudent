package be.ecam.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.Formation
import be.ecam.companion.data.FormationBlock

@Composable
fun FormationHeroCard(
    program: ProgramCardData,
    year: String?,
    totalCourses: Int,
    block: FormationBlock? = null,
    modifier: Modifier = Modifier
) {
    val color = formationColors[program.formation.id] ?: MaterialTheme.colorScheme.primary
    val totalBlocks = program.formation.blocks.size
    val totalCoursesAll = program.formation.blocks.sumOf { it.courses.size }
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
                        InfoPill(icon = Icons.Filled.ViewWeek, label = "Blocs", value = totalBlocks.toString())
                        InfoPill(icon = Icons.Filled.AutoGraph, label = "Cours totaux", value = totalCoursesAll.toString())
                    } ?: run {
                        InfoPill(icon = Icons.Filled.ViewWeek, label = "Blocs", value = totalBlocks.toString())
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
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
    }
}

@Composable
fun BlocAvatar(name: String, size: Dp = 36.dp, color: Color? = null) {
    val number = Regex("""\d+""").find(name)?.value
    val accent = color ?: number?.let { blocColors[it] } ?: MaterialTheme.colorScheme.primary
    val icon = number?.let { blocIcons[it] } ?: Icons.Filled.School
    val bg = accent.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent
        )
    }
}

@Composable
fun InfoPill(icon: ImageVector, label: String, value: String) {
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
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

data class ProgramCardData(
    val formation: Formation,
    val description: String,
    val imageUrl: String?
) {
    val title: String get() = formation.name
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

val formationIcons: Map<String, ImageVector> = mapOf(
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

val formationColors: Map<String, Color> = mapOf(
    "automatisation" to Color(0xFF5C6BC0),
    "construction" to Color(0xFF8D6E63),
    "electromecanique" to Color(0xFF00897B),
    "electronique" to Color(0xFF7E57C2),
    "geometre" to Color(0xFF5C7F67),
    "informatique" to Color(0xFF3949AB),
    "ingenierie_sante" to Color(0xFF26A69A),
    "ingenieur_industriel_commercial" to Color(0xFFE07A5F),
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
            val description = formation.description
                ?.takeIf { it.isNotBlank() }
                ?: formation.notes
                ?: formationDescriptions[formation.id]
                ?: ""
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

package be.ecam.companion.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

const val SLIDE_DURATION_MS = 100
const val FADE_DURATION_MS = 100

@OptIn(ExperimentalTime::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier.background(MaterialTheme.colorScheme.surface),
    initialMode: CalendarMode? = null,
    initialAnchorDate: LocalDate? = null,
    initialDialogDate: LocalDate? = null,
    scheduledByDate: Map<LocalDate, List<String>> = emptyMap()
) {
    var today by remember {
        mutableStateOf(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        )
    }
    var anchorDate by remember { mutableStateOf(initialAnchorDate ?: today) }
    var mode by remember { mutableStateOf(initialMode ?: CalendarMode.Month) }
    var slideDirection by remember { mutableStateOf(0) } // -1 for left (next), +1 for right (prev)

    Column(modifier = modifier.padding(8.dp)) {
        // Header with month/year and navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Text(
                    text = "${
                        anchorDate.month.name.lowercase().replaceFirstChar { it.titlecase() }
                    } ${anchorDate.year}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Today",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { slideDirection = 0; anchorDate = today },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Previous",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { slideDirection = 1; anchorDate = mode.prev(anchorDate) }
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Next",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { slideDirection = -1; anchorDate = mode.next(anchorDate) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Mode switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == CalendarMode.Week,
                onClick = { slideDirection = 0; mode = CalendarMode.Week },
                label = { Text("Week") }
            )
            FilterChip(
                selected = mode == CalendarMode.Month,
                onClick = { slideDirection = 0; mode = CalendarMode.Month },
                label = { Text("Month") }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Week day headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            for (d in days) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        d,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        var dialogDate by remember { mutableStateOf(initialDialogDate) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(mode, anchorDate) {
                    var triggered = false
                    var totalDx = 0f
                    val threshold = 60f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            if (triggered) return@detectHorizontalDragGestures
                            totalDx += dragAmount
                            if (totalDx <= -threshold) {
                                slideDirection = -1
                                anchorDate = mode.next(anchorDate)
                                triggered = true
                            } else if (totalDx >= threshold) {
                                slideDirection = 1
                                anchorDate = mode.prev(anchorDate)
                                triggered = true
                            }
                        },
                        onDragEnd = {
                            triggered = false
                            totalDx = 0f
                        },
                        onDragCancel = {
                            triggered = false
                            totalDx = 0f
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = Pair(mode, anchorDate),
                transitionSpec = {
                    val dir = slideDirection
                    if (dir < 0) {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(SLIDE_DURATION_MS)
                        ) + fadeIn(animationSpec = tween(SLIDE_DURATION_MS)) togetherWith
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(SLIDE_DURATION_MS)
                                ) + fadeOut(animationSpec = tween(SLIDE_DURATION_MS))
                    } else if (dir > 0) {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(SLIDE_DURATION_MS)
                        ) + fadeIn(animationSpec = tween(SLIDE_DURATION_MS)) togetherWith
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(SLIDE_DURATION_MS)
                                ) + fadeOut(animationSpec = tween(SLIDE_DURATION_MS))
                    } else {
                        fadeIn(animationSpec = tween(FADE_DURATION_MS)) togetherWith fadeOut(
                            animationSpec = tween(FADE_DURATION_MS)
                        )
                    }
                }, label = "calendarPager"
            ) { (calendarMode, aDate) ->
                when (calendarMode) {
                    CalendarMode.Month -> MonthGrid(
                        anchorDate = aDate,
                        today = today,
                        scheduledByDate = scheduledByDate,
                        onDateClick = { date -> dialogDate = date }
                    )

                    CalendarMode.Week -> WeekRow(
                        anchorDate = aDate,
                        today = today,
                        scheduledByDate = scheduledByDate,
                        onDateClick = { date -> dialogDate = date }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (dialogDate != null) {
            val items = scheduledByDate[dialogDate] ?: emptyList()
            Column(modifier = Modifier.fillMaxWidth()) {
                val header = "${dialogDate!!.year}-${dialogDate!!.month.number.toString().padStart(2, '0')}-${dialogDate!!.day.toString().padStart(2, '0')}"
                Text(text = "Items on $header", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                if (items.isEmpty()) {
                    Text("No items", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                } else {
                    for (t in items) {
                        Text("• $t")
                    }
                }
            }
        }
    }
}

enum class CalendarMode {
    Week, Month;

    fun next(date: LocalDate): LocalDate = when (this) {
        Week -> date.plus(7, DateTimeUnit.DAY)
        Month -> date.plus(1, DateTimeUnit.MONTH)
    }

    fun prev(date: LocalDate): LocalDate = when (this) {
        Week -> date.minus(7, DateTimeUnit.DAY)
        Month -> date.minus(1, DateTimeUnit.MONTH)
    }
}

@Composable
private fun WeekRow(
    anchorDate: LocalDate,
    today: LocalDate,
    scheduledByDate: Map<LocalDate, List<String>>,
    onDateClick: (LocalDate) -> Unit
) {
    val start = anchorDate.startOfWeek()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0 until 7) {
            val d = start.plus(i, DateTimeUnit.DAY)
            val has = scheduledByDate.containsKey(d)
            Box(modifier = Modifier.weight(1f)) {
                DayCell(
                    date = d,
                    isToday = d == today,
                    isOtherMonth = false,
                    hasItems = has,
                    onClick = { onDateClick(d) }
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    anchorDate: LocalDate,
    today: LocalDate,
    scheduledByDate: Map<LocalDate, List<String>>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstOfMonth = LocalDate(anchorDate.year, anchorDate.month, 1)
    val start = firstOfMonth.startOfWeek()
    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until 7) {
                    val idx = row * 7 + col
                    val date = start.plus(idx, DateTimeUnit.DAY)
                    val isOther = date.month != anchorDate.month
                    Box(modifier = Modifier.weight(1f)) {
                        val has = scheduledByDate.containsKey(date)
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isOtherMonth = isOther,
                            hasItems = has,
                            onClick = { onDateClick(date) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isOtherMonth: Boolean,
    hasItems: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor =
        if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    val textColor = when {
        isOtherMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .padding(2.dp)
            .background(backgroundColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = date.day.toString(), color = textColor)
        if (hasItems) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
        }
    }
}

private fun LocalDate.startOfWeek(): LocalDate {
    // Assume Monday as first day
    val dayIndex = (this.dayOfWeek.isoDayNumber - 1) // 0..6
    return this.minus(dayIndex, DateTimeUnit.DAY)
}


@Preview
@Composable
private fun Preview_Calendar_Month_NoDialog() {
    CalendarScreen(
        initialMode = CalendarMode.Month,
        initialAnchorDate = LocalDate(2025, 9, 15)
    )
}

@Preview
@Composable
private fun Preview_Calendar_Week_NoDialog() {
    CalendarScreen(
        initialMode = CalendarMode.Week,
        initialAnchorDate = LocalDate(2025, 9, 15)
    )
}

@Preview
@Composable
private fun Preview_Calendar_Month_WithDialog() {
    val dateWithEntries = LocalDate(2025, 9, 10)
    CalendarScreen(
        initialMode = CalendarMode.Month,
        initialAnchorDate = LocalDate(2025, 9, 1),
        initialDialogDate = dateWithEntries
    )
}

@Preview
@Composable
private fun Preview_Calendar_Week_WithDialog() {
    val dateWithEntries = LocalDate(2025, 9, 21)
    CalendarScreen(
        initialMode = CalendarMode.Week,
        initialAnchorDate = LocalDate(2025, 9, 21),
        initialDialogDate = dateWithEntries
    )
}

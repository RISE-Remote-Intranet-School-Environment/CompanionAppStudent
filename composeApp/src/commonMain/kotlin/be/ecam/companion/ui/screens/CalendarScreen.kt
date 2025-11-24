package be.ecam.companion.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    var dialogDate by remember { mutableStateOf(initialDialogDate) }
    val localEventsByDate = rememberCalendarEventsByDate()
    val eventsByDate = remember(localEventsByDate, scheduledByDate) {
        mergeCalendarEvents(localEventsByDate, scheduledByDate)
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
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
                        .clickable {
                            slideDirection = 0
                            anchorDate = today
                            dialogDate = today
                        },
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
                        eventsByDate = eventsByDate,
                        onDateClick = { date -> dialogDate = date }
                    )

                    CalendarMode.Week -> WeekRow(
                        anchorDate = aDate,
                        today = today,
                        eventsByDate = eventsByDate,
                        onDateClick = { date -> dialogDate = date }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (dialogDate != null) {
            val items = eventsByDate[dialogDate] ?: emptyList()
            Column(modifier = Modifier.fillMaxWidth()) {
                val header = dialogDate!!.toEuropeanString()
                Text(text = "Events on $header", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                if (items.isEmpty()) {
                    Text("No events", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                } else {
                    for (event in items) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = event.category.color,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (event.description.isNotBlank() && event.description != event.title) {
                                    Text(
                                        text = event.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun mergeCalendarEvents(
    localEvents: Map<LocalDate, List<CalendarEvent>>,
    remoteEvents: Map<LocalDate, List<String>>
): Map<LocalDate, List<CalendarEvent>> {
    if (remoteEvents.isEmpty()) return localEvents
    val merged = localEvents.mapValues { entry -> entry.value.toMutableList() }.toMutableMap()
    remoteEvents.forEach { (date, titles) ->
        if (titles.isEmpty()) return@forEach
        val additions = titles.mapIndexed { index, title ->
            CalendarEvent(
                id = "remote_${date}_$index",
                title = title,
                description = title,
                category = CalendarEventCategory.Remote,
                date = date,
                years = emptyList()
            )
        }
        val list = merged.getOrPut(date) { mutableListOf<CalendarEvent>() }
        list += additions
    }
    return merged.mapValues { it.value.toList() }
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
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    onDateClick: (LocalDate) -> Unit
) {
    val start = anchorDate.startOfWeek()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0 until 7) {
            val d = start.plus(i, DateTimeUnit.DAY)
            val events = eventsByDate[d].orEmpty()
            Box(
                modifier = Modifier.weight(1f)
            ) {
                DayCell(
                    date = d,
                    isToday = d == today,
                    isOtherMonth = false,
                    events = events,
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
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
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
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        val events = eventsByDate[date].orEmpty()
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isOtherMonth = isOther,
                            events = events,
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
    events: List<CalendarEvent> = emptyList(),
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
            .fillMaxWidth()
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = date.day.toString(), color = textColor)
        if (events.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            events.take(3).forEach { event ->
                CalendarEventBadge(event)
                Spacer(Modifier.height(2.dp))
            }
            if (events.size > 3) {
                Text(
                    text = "+${events.size - 3} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalendarEventBadge(event: CalendarEvent) {
    val baseColor = event.category.color
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(baseColor.copy(alpha = 0.2f))
    ) {
        Text(
            text = event.title,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = baseColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun LocalDate.startOfWeek(): LocalDate {
    // Assume Monday as first day
    val dayIndex = (this.dayOfWeek.isoDayNumber - 1) // 0..6
    return this.minus(dayIndex, DateTimeUnit.DAY)
}

private fun LocalDate.toEuropeanString(): String {
    // dd/MM/yyyy formatting for Belgian/European style
    val day = this.day.toString().padStart(2, '0')
    val month = this.month.number.toString().padStart(2, '0')
    return "$day/$month/${this.year}"
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

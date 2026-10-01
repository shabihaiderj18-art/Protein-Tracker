package app.protein.tracker.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.protein.tracker.domain.DayClock
import app.protein.tracker.domain.Fmt
import app.protein.tracker.domain.MonthAverage
import app.protein.tracker.domain.UserSettings
import app.protein.tracker.ui.components.EmptyState
import app.protein.tracker.ui.components.ProteinRing
import app.protein.tracker.ui.components.ScreenTitle
import app.protein.tracker.ui.components.SectionCard
import app.protein.tracker.ui.components.SectionLabel
import app.protein.tracker.ui.components.mealSections
import app.protein.tracker.ui.components.proteinStatus
import app.protein.tracker.ui.components.rememberAppContainer
import app.protein.tracker.ui.components.rememberEntryActions
import app.protein.tracker.ui.components.screenPadding
import app.protein.tracker.ui.theme.NumberStyle
import app.protein.tracker.ui.theme.SectionLabelStyle

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    selectedDay: Long?,
    onSelectDay: (Long?) -> Unit,
    onEditEntry: (Long) -> Unit,
) {
    AnimatedContent(
        targetState = selectedDay,
        transitionSpec = {
            val direction = if (targetState != null) 1 else -1
            (slideInHorizontally(tween(260)) { width -> direction * width / 5 } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(260)) { width -> -direction * width / 5 } + fadeOut(tween(160)))
        },
        label = "history",
    ) { day ->
        if (day == null) {
            HistoryOverview(contentPadding = contentPadding, onSelectDay = { onSelectDay(it) })
        } else {
            DayDetail(
                day = day,
                contentPadding = contentPadding,
                onBack = { onSelectDay(null) },
                onEditEntry = onEditEntry,
            )
        }
    }
}

@Composable
private fun HistoryOverview(
    contentPadding: PaddingValues,
    onSelectDay: (Long) -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: HistoryViewModel = viewModel { HistoryViewModel(container.repository, container.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(contentPadding, bottomExtra = 24.dp),
    ) {
        item(key = "title") { ScreenTitle(title = "History") }

        if (state.loaded && !state.hasAnyData) {
            item(key = "empty") {
                SectionCard {
                    EmptyState(
                        icon = Icons.Rounded.DateRange,
                        title = "Your history will appear here",
                        message = "Log your meals for a few days to see your weekly average, trends and monthly averages.",
                    )
                }
            }
        } else if (state.loaded) {
            item(key = "week") {
                SectionCard {
                    WeekHeader(state, viewModel::previousWeek, viewModel::nextWeek)
                    Spacer(Modifier.height(20.dp))
                    WeeklyChart(week = state.week, target = state.proteinTarget, today = state.today)
                }
            }
        }

        if (state.loaded) {
            item(key = "calendar") {
                Column {
                    SectionLabel("Calendar")
                    SectionCard {
                        MonthCalendar(
                            month = state.month,
                            today = state.today,
                            totals = state.monthTotals,
                            target = state.proteinTarget,
                            canGoNext = state.canGoNextMonth,
                            onPrevious = { viewModel.showMonth(state.month.minusMonths(1)) },
                            onNext = { viewModel.showMonth(state.month.plusMonths(1)) },
                            onSelectDay = onSelectDay,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Darker days came closer to your protein target. Tap a day to see or edit it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (state.monthly.isNotEmpty()) {
            item(key = "monthly") {
                Column {
                    SectionLabel("Monthly averages")
                    SectionCard(padded = false) {
                        state.monthly.take(12).forEachIndexed { index, month ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                            MonthRow(month)
                        }
                    }
                    Text(
                        "Averages count only the days you logged something.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(state: HistoryUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val week = state.week
    Text("7-DAY AVERAGE", style = SectionLabelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            week.avgProtein?.let { "${Fmt.protein(it)} g" } ?: "—",
            style = MaterialTheme.typography.displaySmall.merge(NumberStyle),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "protein a day",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
    Text(
        week.avgKcal?.let { "${Fmt.kcal(it)} kcal a day · ${week.loggedDays} of 7 days logged" }
            ?: "Nothing logged in these seven days",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous week")
        }
        Text(
            if (week.days.isNotEmpty()) Fmt.dayRange(week.days.first(), week.days.last()) else "",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = state.weekOffset > 0) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun MonthRow(month: MonthAverage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(Fmt.month(month.month), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (month.loggedDays == 1) "1 day logged" else "${month.loggedDays} days logged",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${Fmt.protein(month.avgProtein)} g",
                style = MaterialTheme.typography.titleMedium.merge(NumberStyle),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${Fmt.kcal(month.avgKcal)} kcal",
                style = MaterialTheme.typography.bodySmall.merge(NumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayDetail(
    day: Long,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onEditEntry: (Long) -> Unit,
) {
    val container = rememberAppContainer()
    val entriesFlow = remember(day) { container.repository.observeDay(day) }
    val entries by entriesFlow.collectAsStateWithLifecycle(initialValue = null)
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val actions = rememberEntryActions()
    val today = DayClock.today(settings.dayStartMinutes)
    val list = entries.orEmpty()
    val protein = list.sumOf { it.protein }
    val kcal = list.sumOf { it.kcal }
    val title = Fmt.dayTitle(day, today)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(contentPadding),
    ) {
        item(key = "top") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to history")
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    if (title != Fmt.fullDate(day)) {
                        Text(
                            Fmt.fullDate(day),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item(key = "summary") {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProteinRing(
                        eaten = protein,
                        target = settings.proteinTarget,
                        size = 92.dp,
                        strokeWidth = 10.dp,
                        showLabels = false,
                    )
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(
                            "${Fmt.protein(protein)} g protein",
                            style = MaterialTheme.typography.titleLarge.merge(NumberStyle),
                        )
                        Text(
                            proteinStatus(protein, settings.proteinTarget),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "${Fmt.kcal(kcal)} of ${Fmt.kcal(settings.kcalTarget)} kcal",
                            style = MaterialTheme.typography.bodyMedium.merge(NumberStyle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (entries != null && list.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.DateRange,
                    title = "Nothing logged on this day",
                    message = "Tap “Add to this day” below to fill it in.",
                )
            }
        } else {
            mealSections(
                entries = list,
                onEdit = { onEditEntry(it.id) },
                onDelete = actions::delete,
                onLogAgain = actions::logAgainToday,
            )
            if (list.isNotEmpty()) {
                item(key = "hint") {
                    Text(
                        "Long-press an entry to log it again today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

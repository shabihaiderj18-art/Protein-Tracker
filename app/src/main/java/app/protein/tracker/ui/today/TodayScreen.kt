package app.protein.tracker.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.protein.tracker.domain.Fmt
import app.protein.tracker.ui.components.CalorieBar
import app.protein.tracker.ui.components.EmptyState
import app.protein.tracker.ui.components.MacroRow
import app.protein.tracker.ui.components.ProteinRing
import app.protein.tracker.ui.components.ScreenTitle
import app.protein.tracker.ui.components.SectionCard
import app.protein.tracker.ui.components.mealSections
import app.protein.tracker.ui.components.screenPadding
import app.protein.tracker.ui.components.rememberAppContainer
import app.protein.tracker.ui.components.rememberEntryActions

@Composable
fun TodayScreen(
    contentPadding: PaddingValues,
    onEditEntry: (Long) -> Unit,
    onLogFood: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: TodayViewModel = viewModel { TodayViewModel(container.repository, container.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = rememberEntryActions()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(contentPadding),
    ) {
        item(key = "title") {
            ScreenTitle(
                title = "Today",
                subtitle = if (state.loaded) Fmt.fullDate(state.day) else " ",
            )
        }
        item(key = "ring") {
            SectionCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProteinRing(eaten = state.protein, target = state.settings.proteinTarget)
                    Spacer(Modifier.height(24.dp))
                    CalorieBar(kcal = state.kcal, target = state.kcalTarget)
                    Spacer(Modifier.height(16.dp))
                    MacroRow(carbs = state.carbs, fat = state.fat, fiber = state.fiber)
                }
            }
        }
        if (state.loaded && state.settings.autoCalories) {
            item(key = "check-in") {
                CheckInCard(
                    settings = state.settings,
                    activity = state.activity,
                    usualDay = viewModel.usualDay(state.settings),
                    kcalTarget = state.kcalTarget,
                    onSave = { viewModel.saveActivity(state.day, it) },
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        if (state.loaded && state.entries.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.Add,
                    title = "Nothing logged yet today",
                    message = "Tap “Log food” below to add your first meal. It takes a few seconds.",
                    modifier = Modifier.animateItem(),
                    onIconClick = onLogFood,
                )
            }
        } else {
            mealSections(
                entries = state.entries,
                onEdit = { onEditEntry(it.id) },
                onDelete = actions::delete,
                onLogAgain = actions::logAgainToday,
            )
            if (state.entries.isNotEmpty()) {
                item(key = "hint") {
                    Text(
                        "Swipe right to edit · swipe left to delete · long-press to log again",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .animateItem(),
                    )
                }
            }
        }
    }
}

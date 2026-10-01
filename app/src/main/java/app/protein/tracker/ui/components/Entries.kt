package app.protein.tracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.protein.tracker.data.ProteinRepository
import app.protein.tracker.data.db.LogEntry
import app.protein.tracker.data.db.isQuick
import app.protein.tracker.domain.Fmt
import app.protein.tracker.domain.MealSlot
import app.protein.tracker.ui.theme.NumberStyle
import app.protein.tracker.ui.theme.SectionLabelStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

fun LogEntry.detailLabel(): String =
    if (isQuick) "Typed in directly" else Fmt.amountLabel(amount, inUnits, unitName, quantity, baseUnit)

/** Delete (with undo) and "log again today" (with undo), shared by Today and History. */
class EntryActions(
    private val repository: ProteinRepository,
    private val scope: CoroutineScope,
    private val snack: Snack,
    private val haptics: Haptics,
) {
    fun delete(entry: LogEntry) {
        scope.launch {
            val removed = repository.deleteEntry(entry.id) ?: return@launch
            snack.show("Removed ${removed.name}", "Undo") {
                scope.launch { repository.restoreEntry(removed) }
            }
        }
    }

    fun logAgainToday(entry: LogEntry) {
        scope.launch {
            val newId = repository.logAgainToday(entry)
            haptics.confirm()
            snack.show("Logged ${entry.name} again today", "Undo") {
                scope.launch { repository.deleteEntry(newId) }
            }
        }
    }
}

@Composable
fun rememberEntryActions(): EntryActions {
    val container = rememberAppContainer()
    val snack = LocalSnack.current
    val haptics = rememberHaptics()
    return remember(container, snack, haptics) {
        EntryActions(container.repository, container.appScope, snack, haptics)
    }
}

/**
 * Today's (or a past day's) entries, grouped under meal headers.
 * Swipe right to edit, swipe left to delete, tap to edit, long-press to log again today.
 */
fun LazyListScope.mealSections(
    entries: List<LogEntry>,
    onEdit: (LogEntry) -> Unit,
    onDelete: (LogEntry) -> Unit,
    onLogAgain: (LogEntry) -> Unit,
) {
    val grouped = MealSlot.entries.map { meal -> meal to entries.filter { it.meal == meal } }
    val order = grouped.flatMap { it.second }.withIndex().associate { it.value.id to it.index }

    grouped.forEach { (meal, mealEntries) ->
        if (mealEntries.isEmpty()) return@forEach
        item(key = "meal-${meal.name}", contentType = "meal-header") {
            MealHeader(meal, mealEntries, Modifier.animateItem())
        }
        items(mealEntries, key = { it.id }, contentType = { "entry" }) { entry ->
            SwipeableEntry(
                entry = entry,
                onEdit = { onEdit(entry) },
                onDelete = { onDelete(entry) },
                onLogAgain = { onLogAgain(entry) },
                appearDelayMillis = min(order[entry.id] ?: 0, 8) * 45,
                modifier = Modifier
                    .animateItem(fadeInSpec = null)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
fun MealHeader(meal: MealSlot, entries: List<LogEntry>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            meal.label.uppercase(),
            style = SectionLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${Fmt.protein(entries.sumOf { it.protein })} g · ${Fmt.kcal(entries.sumOf { it.kcal })} kcal",
            style = MaterialTheme.typography.labelMedium.merge(NumberStyle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SwipeableEntry(
    entry: LogEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogAgain: () -> Unit,
    modifier: Modifier = Modifier,
    appearDelayMillis: Int = 0,
) {
    val state = rememberSwipeToDismissBoxState()
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnDelete by rememberUpdatedState(onDelete)

    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                currentOnEdit()
                state.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> currentOnDelete()
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    // Entries slide up and fade in when they first appear.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(appearDelayMillis.toLong())
        appear.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }

    Box(
        modifier.graphicsLayer {
            alpha = appear.value
            translationY = (1f - appear.value) * 28.dp.toPx()
        }
    ) {
        SwipeToDismissBox(
            state = state,
            backgroundContent = { SwipeBackground(state.dismissDirection) },
        ) {
            EntryCard(entry = entry, onClick = onEdit, onLongClick = onLogAgain)
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.surfaceContainerHighest
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) {
            Alignment.CenterEnd
        } else {
            Alignment.CenterStart
        },
    ) {
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Edit", style = MaterialTheme.typography.labelLarge)
            }
            SwipeToDismissBoxValue.EndToStart -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Delete", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.Delete, contentDescription = null)
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
}

@Composable
fun EntryCard(entry: LogEntry, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = "Edit",
                onLongClickLabel = "Log again today",
            ),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 68.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    entry.detailLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${Fmt.protein(entry.protein)} g",
                    style = MaterialTheme.typography.titleMedium.merge(NumberStyle),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${Fmt.kcal(entry.kcal)} kcal",
                    style = MaterialTheme.typography.bodySmall.merge(NumberStyle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

package app.protein.tracker.ui.foods

import androidx.compose.foundation.clickable
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.protein.tracker.data.ProteinRepository
import app.protein.tracker.data.db.Food
import app.protein.tracker.domain.FuzzySearch
import app.protein.tracker.ui.components.EmptyState
import app.protein.tracker.ui.components.LocalSnack
import app.protein.tracker.ui.components.ScreenTitle
import app.protein.tracker.ui.components.foodSummary
import app.protein.tracker.ui.components.rememberAppContainer
import app.protein.tracker.ui.components.rememberHaptics
import app.protein.tracker.ui.components.screenPadding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodsViewModel(private val repository: ProteinRepository) : ViewModel() {
    val foods: StateFlow<List<Food>?> = repository.foods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleFavorite(food: Food) {
        viewModelScope.launch { repository.setFavorite(food.id, !food.isFavorite) }
    }
}

@Composable
fun FoodsScreen(
    contentPadding: PaddingValues,
    onEditFood: (Long) -> Unit,
    onAddFood: (String) -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: FoodsViewModel = viewModel { FoodsViewModel(container.repository) }
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var favouritesOnly by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberHaptics()

    val shown = remember(query, foods, favouritesOnly) {
        val all = foods.orEmpty().filter { !favouritesOnly || it.isFavorite }
        if (query.isBlank()) {
            all.sortedWith(compareByDescending<Food> { it.isFavorite }.thenBy { it.name.lowercase() })
        } else {
            FuzzySearch.rank(query, all, { it.name }) { if (it.isFavorite) 5.0 else 0.0 }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(contentPadding),
    ) {
        item(key = "title") {
            ScreenTitle(
                title = "Foods",
                subtitle = foods?.let { "${it.size} foods · values per 100 g or 100 ml" } ?: " ",
            )
        }
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text("Search your foods") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        item(key = "filter") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                FilterChip(
                    selected = !favouritesOnly,
                    onClick = { favouritesOnly = false },
                    label = { Text("All foods") },
                )
                FilterChip(
                    selected = favouritesOnly,
                    onClick = { favouritesOnly = true },
                    label = { Text("Favourites") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
        when {
            foods == null -> Unit
            shown.isEmpty() && favouritesOnly && query.isBlank() -> item(key = "no-favourites") {
                EmptyState(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = "No favourites yet",
                    message = "Tap the heart next to any food. Favourites appear first when you log.",
                    action = { Button(onClick = { favouritesOnly = false }) { Text("Show all foods") } },
                )
            }
            shown.isEmpty() && query.isNotBlank() -> item(key = "no-match") {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No food called “${query.trim()}”",
                    message = "Add it once and it will be ready every time you log.",
                    action = { Button(onClick = { onAddFood(query.trim()) }) { Text("Add “${query.trim()}”") } },
                )
            }
            shown.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No foods yet",
                    message = "Add the foods you eat often, with their protein and calories per 100 g.",
                    action = { Button(onClick = { onAddFood("") }) { Text("Add a food") } },
                )
            }
            else -> items(shown, key = { it.id }) { food ->
                FoodRow(
                    food = food,
                    onClick = { onEditFood(food.id) },
                    onToggleFavorite = {
                        haptics.tick()
                        viewModel.toggleFavorite(food)
                    },
                    modifier = Modifier
                        .animateItem()
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FoodRow(
    food: Food,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(start = 18.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    food.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    foodSummary(food),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val note = food.note
                if (!note.isNullOrBlank()) {
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (food.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (food.isFavorite) "Remove from favourites" else "Add to favourites",
                        tint = if (food.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/** Full-screen editor shown over the app for adding or editing a food from the Foods tab. */
@Composable
fun FoodEditorPanel(foodId: Long?, prefillName: String, onClose: () -> Unit) {
    val container = rememberAppContainer()
    val snack = LocalSnack.current
    var food by remember { mutableStateOf<Food?>(null) }
    var loaded by remember { mutableStateOf(foodId == null) }

    LaunchedEffect(foodId) {
        if (foodId != null) {
            food = container.repository.food(foodId)
            loaded = true
            if (food == null) onClose()
        }
    }
    if (!loaded) return

    val current = food
    val haptics = rememberHaptics()
    FoodEditor(
        initial = current,
        prefillName = prefillName,
        onClose = onClose,
        onSave = { edited ->
            container.appScope.launch {
                container.repository.saveFood(edited)
                haptics.confirm()
            }
            onClose()
        },
        onDelete = current?.let { existing ->
            {
                container.appScope.launch {
                    container.repository.deleteFood(existing)
                    snack.show("Deleted ${existing.name}", "Undo") {
                        container.appScope.launch { container.repository.restoreFood(existing) }
                    }
                }
                onClose()
            }
        },
    )
}

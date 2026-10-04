package app.protein.tracker.ui.log

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.protein.tracker.data.FoodPick
import app.protein.tracker.data.db.Food
import app.protein.tracker.data.db.LogEntry
import app.protein.tracker.data.db.isQuick
import app.protein.tracker.domain.BaseUnit
import app.protein.tracker.domain.DayClock
import app.protein.tracker.domain.Fmt
import app.protein.tracker.domain.FuzzySearch
import app.protein.tracker.domain.MealSlot
import app.protein.tracker.domain.Nutrition
import app.protein.tracker.domain.Units
import app.protein.tracker.domain.UserSettings
import app.protein.tracker.ui.components.EmptyState
import app.protein.tracker.ui.components.MealSelector
import app.protein.tracker.ui.components.PanelTopBar
import app.protein.tracker.ui.components.PresetRow
import app.protein.tracker.ui.components.foodSummary
import app.protein.tracker.ui.components.rememberAppContainer
import app.protein.tracker.ui.components.rememberAutoFocus
import app.protein.tracker.ui.components.rememberEntryActions
import app.protein.tracker.ui.components.rememberHaptics
import app.protein.tracker.ui.foods.FoodEditor
import app.protein.tracker.ui.theme.NumberStyle
import app.protein.tracker.ui.theme.SectionLabelStyle
import java.time.LocalTime

private const val DEFAULT_QUICK_NAME = "Quick entry"

/** What the log panel was opened for. */
sealed interface LogRequest {
    /** Log something new. [day] null means today. */
    data class New(val day: Long? = null) : LogRequest

    data class Edit(val entryId: Long) : LogRequest
}

private sealed interface LogStep {
    data object Loading : LogStep
    data object Search : LogStep
    data class Amount(val pick: FoodPick, val editing: LogEntry? = null) : LogStep
    data class Quick(val editing: LogEntry? = null) : LogStep
    data class NewFood(val name: String) : LogStep
}

/**
 * Full-screen panel for logging. Flow: search → pick a food → amount → Save.
 * The search box sits at the bottom, and the best match appears right above it.
 */
@Composable
fun LogScreen(request: LogRequest, onClose: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: LogViewModel = viewModel { LogViewModel(container.repository) }
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val haptics = rememberHaptics()
    val actions = rememberEntryActions()

    val targetDay = (request as? LogRequest.New)?.day
    val today = DayClock.today(settings.dayStartMinutes)
    val dayNote = targetDay?.takeIf { it != today }?.let { "Adding to ${Fmt.dayTitle(it, today)}" }
    val defaultMeal = remember { MealSlot.at(LocalTime.now()) }

    var query by rememberSaveable { mutableStateOf("") }
    var step by remember {
        mutableStateOf<LogStep>(if (request is LogRequest.Edit) LogStep.Loading else LogStep.Search)
    }

    if (request is LogRequest.Edit) {
        LaunchedEffect(request.entryId) {
            val entry = viewModel.loadEntry(request.entryId)
            if (entry == null) {
                onClose()
            } else {
                step = if (entry.isQuick) LogStep.Quick(entry) else LogStep.Amount(FoodPick.from(entry), entry)
            }
        }
    }

    val isNew = request is LogRequest.New
    BackHandler(enabled = isNew && step !is LogStep.Search) { step = LogStep.Search }
    val backToSearchOrClose: () -> Unit = if (isNew) ({ step = LogStep.Search }) else onClose
    val onSaved: () -> Unit = {
        haptics.confirm()
        onClose()
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val direction = if (targetState is LogStep.Search) -1 else 1
            (slideInHorizontally(tween(260)) { width -> direction * width / 4 } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(260)) { width -> -direction * width / 4 } + fadeOut(tween(160)))
        },
        label = "log-step",
    ) { current ->
        when (current) {
            LogStep.Loading -> Box(Modifier.fillMaxSize())

            LogStep.Search -> SearchStep(
                foods = foods,
                query = query,
                onQueryChange = { query = it },
                dayNote = dayNote,
                onPick = { food -> step = LogStep.Amount(FoodPick.from(food)) },
                onQuick = { step = LogStep.Quick() },
                onNewFood = { name -> step = LogStep.NewFood(name) },
                onClose = onClose,
            )

            is LogStep.Amount -> {
                val foodId = current.pick.foodId
                val isFavorite = foodId?.let { id -> foods?.firstOrNull { it.id == id }?.isFavorite }
                val editing = current.editing
                AmountStep(
                    pick = current.pick,
                    isFavorite = isFavorite,
                    editing = editing,
                    initialMeal = editing?.meal ?: defaultMeal,
                    dayNote = dayNote,
                    onBack = backToSearchOrClose,
                    onToggleFavorite = {
                        if (foodId != null && isFavorite != null) viewModel.setFavorite(foodId, !isFavorite)
                    },
                    onSave = { amount, inUnits, meal ->
                        viewModel.saveFood(current.pick, amount, inUnits, meal, targetDay, editing, onSaved)
                    },
                    onDelete = editing?.let { entry ->
                        {
                            actions.delete(entry)
                            onClose()
                        }
                    },
                )
            }

            is LogStep.Quick -> {
                val editing = current.editing
                QuickStep(
                    editing = editing,
                    initialMeal = editing?.meal ?: defaultMeal,
                    dayNote = dayNote,
                    onBack = backToSearchOrClose,
                    onSave = { name, protein, kcal, meal ->
                        viewModel.saveQuick(name, protein, kcal, meal, targetDay, editing, onSaved)
                    },
                    onDelete = editing?.let { entry ->
                        {
                            actions.delete(entry)
                            onClose()
                        }
                    },
                )
            }

            is LogStep.NewFood -> FoodEditor(
                initial = null,
                prefillName = current.name,
                saveLabel = "Save and continue",
                onClose = { step = LogStep.Search },
                onSave = { food ->
                    viewModel.createFood(food) { created -> step = LogStep.Amount(FoodPick.from(created)) }
                },
                onDelete = null,
            )
        }
    }
}

// ---------------------------------------------------------------- Search

private sealed interface SearchRow {
    val key: String

    data class Header(val title: String) : SearchRow {
        override val key: String get() = "header-$title"
    }

    data class Item(val food: Food, val section: String) : SearchRow {
        override val key: String get() = "$section-${food.id}"
    }
}

/**
 * Rows for a list drawn bottom-up: index 0 sits right above the search box.
 * With no search text: favourites, then recent foods, then everything else A–Z.
 */
private fun buildRows(query: String, foods: List<Food>, now: Long): List<SearchRow> {
    if (query.isNotBlank()) {
        return FuzzySearch
            .rank(query, foods, { it.name }) { FuzzySearch.usageBoost(it.isFavorite, it.lastUsedAt, it.useCount, now) }
            .map { SearchRow.Item(it, "match") }
    }
    val favourites = foods.filter { it.isFavorite }.sortedBy { it.name.lowercase() }
    val recent = foods
        .filter { !it.isFavorite && it.lastUsedAt != null }
        .sortedByDescending { it.lastUsedAt ?: 0L }
        .take(8)
    val quickIds = (favourites + recent).map { it.id }.toSet()
    val rest = foods.filter { it.id !in quickIds }.sortedBy { it.name.lowercase() }

    return buildList {
        if (quickIds.isNotEmpty()) {
            favourites.forEach { add(SearchRow.Item(it, "fav")) }
            recent.forEach { add(SearchRow.Item(it, "recent")) }
            val title = when {
                favourites.isEmpty() -> "Recent"
                recent.isEmpty() -> "Favourites"
                else -> "Favourites & recent"
            }
            add(SearchRow.Header(title))
        }
        if (rest.isNotEmpty()) {
            // Added Z→A so that, drawn bottom-up, the list reads A→Z from the top.
            rest.asReversed().forEach { add(SearchRow.Item(it, "all")) }
            add(SearchRow.Header("All foods"))
        }
    }
}

@Composable
private fun SearchStep(
    foods: List<Food>?,
    query: String,
    onQueryChange: (String) -> Unit,
    dayNote: String?,
    onPick: (Food) -> Unit,
    onQuick: () -> Unit,
    onNewFood: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focus = rememberAutoFocus()
    val listState = rememberLazyListState()
    val now = remember { System.currentTimeMillis() }
    val rows = remember(query, foods) { if (foods == null) emptyList() else buildRows(query, foods, now) }

    LaunchedEffect(query) {
        if (listState.layoutInfo.totalItemsCount > 0) listState.scrollToItem(0)
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PanelTopBar(
            title = "Log food",
            subtitle = dayNote,
            navigationIcon = Icons.Rounded.Close,
            navigationLabel = "Close",
            onNavigate = onClose,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            when {
                foods == null -> Unit
                rows.isEmpty() && query.isNotBlank() -> EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No match for “${query.trim()}”",
                    message = "Log it as a quick entry, or add it to your foods so it's here next time.",
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = onQuick) { Text("Quick entry") }
                            Button(onClick = { onNewFood(query.trim()) }) { Text("Add food") }
                        }
                    },
                )
                rows.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "Your food list is empty",
                    message = "Add a food, or use a quick entry to type protein and calories directly.",
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = onQuick) { Text("Quick entry") }
                            Button(onClick = { onNewFood("") }) { Text("Add food") }
                        }
                    },
                )
                else -> LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(rows, key = { it.key }) { row ->
                        when (row) {
                            is SearchRow.Header -> Text(
                                row.title.uppercase(),
                                style = SectionLabelStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 14.dp, top = 16.dp, bottom = 4.dp),
                            )
                            is SearchRow.Item -> FoodPickRow(row.food) { onPick(row.food) }
                        }
                    }
                }
            }
        }
        SearchBox(
            query = query,
            onQueryChange = onQueryChange,
            focus = focus,
            onQuick = onQuick,
            onSearch = {
                rows.firstNotNullOfOrNull { (it as? SearchRow.Item)?.food }?.let(onPick)
            },
        )
    }
}

@Composable
private fun FoodPickRow(food: Food, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
        }
        if (food.isFavorite) {
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = "Favourite",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    focus: FocusRequester,
    onQuick: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focus),
            placeholder = { Text("Search foods") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            } else {
                null
            },
            singleLine = true,
            shape = CircleShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        FilledTonalButton(
            onClick = onQuick,
            modifier = Modifier.height(56.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Quick")
        }
    }
}

// ---------------------------------------------------------------- Amount

private fun presetsFor(inUnits: Boolean, baseUnit: BaseUnit): List<Double> = when {
    inUnits -> listOf(0.5, 1.0, 2.0, 3.0)
    baseUnit == BaseUnit.ML -> listOf(100.0, 200.0, 250.0, 300.0)
    else -> listOf(50.0, 100.0, 150.0, 200.0)
}

@Composable
private fun AmountStep(
    pick: FoodPick,
    isFavorite: Boolean?,
    editing: LogEntry?,
    initialMeal: MealSlot,
    dayNote: String?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSave: (amount: Double, inUnits: Boolean, meal: MealSlot) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val unitName = pick.unitName.orEmpty()
    val unitSize = pick.unitSize
    val hasUnit = pick.hasUnit && unitSize != null

    var inUnits by remember { mutableStateOf(pick.initialInUnits && hasUnit) }
    val startText = Fmt.plain(pick.initialAmount)
    var field by remember { mutableStateOf(TextFieldValue(startText, TextRange(0, startText.length))) }
    var meal by remember { mutableStateOf(initialMeal) }
    val focus = rememberAutoFocus()

    val amount = Nutrition.parseAmount(field.text)
    val quantity = amount?.let { Nutrition.quantity(it, inUnits, unitSize) } ?: 0.0
    val nutrients = Nutrition.forQuantity(pick.proteinPer100, pick.kcalPer100, quantity)
    val canSave = amount != null && amount > 0.0
    val shownProtein by animateFloatAsState(nutrients.protein.toFloat(), tween(250), label = "protein")
    val shownKcal by animateFloatAsState(nutrients.kcal.toFloat(), tween(250), label = "kcal")

    fun setAmount(value: Double) {
        val text = Fmt.plain(value)
        field = TextFieldValue(text, TextRange(text.length))
    }

    fun switchUnits(toUnits: Boolean) {
        if (toUnits == inUnits || unitSize == null) return
        amount?.let { setAmount(Nutrition.convertAmount(it, toUnits, unitSize)) }
        inUnits = toUnits
    }

    val save: () -> Unit = {
        if (amount != null && amount > 0.0) onSave(amount, inUnits, meal)
    }

    val suffix = if (inUnits) Units.label(unitName, amount ?: 2.0) else pick.baseUnit.symbol
    val helper = when {
        inUnits && unitSize != null -> "= ${Fmt.amount(quantity)} ${pick.baseUnit.symbol}"
        hasUnit -> Fmt.unitDefinition(unitName, unitSize, pick.baseUnit)
        else -> "Decimals are fine, e.g. 1.75"
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PanelTopBar(
            title = pick.name,
            subtitle = dayNote
                ?: "${Fmt.protein(pick.proteinPer100)} g protein · ${Fmt.kcal(pick.kcalPer100)} kcal per 100 ${pick.baseUnit.symbol}",
            navigationIcon = if (editing == null) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
            navigationLabel = if (editing == null) "Back" else "Close",
            onNavigate = onBack,
            actions = {
                if (isFavorite != null) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(vertical = 20.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "${Fmt.protein(shownProtein.toDouble())} g",
                        style = MaterialTheme.typography.displayMedium.merge(NumberStyle),
                    )
                    Text("protein", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${Fmt.kcal(shownKcal.toDouble())} kcal",
                        style = MaterialTheme.typography.titleMedium.merge(NumberStyle),
                    )
                    if (pick.carbsPer100 != null || pick.fatPer100 != null) {
                        fun part(per100: Double?) = Fmt.protein((per100 ?: 0.0) * quantity / 100.0)
                        Text(
                            "Carbs ${part(pick.carbsPer100)} g · Fat ${part(pick.fatPer100)} g · " +
                                "Fibre ${part(pick.fiberPer100)} g",
                            style = MaterialTheme.typography.bodySmall.merge(NumberStyle),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (hasUnit) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !inUnits,
                        onClick = { switchUnits(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text(pick.baseUnit.longName) }
                    SegmentedButton(
                        selected = inUnits,
                        onClick = { switchUnits(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text(Units.pluralTitle(unitName)) }
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                label = { Text("Amount") },
                suffix = { Text(suffix) },
                supportingText = { Text(helper) },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.merge(NumberStyle),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )

            Spacer(Modifier.height(4.dp))
            PresetRow(
                presets = presetsFor(inUnits, pick.baseUnit),
                suffix = if (inUnits) "" else " ${pick.baseUnit.symbol}",
                onPick = { setAmount(it) },
            )

            Spacer(Modifier.height(24.dp))
            Text("Meal", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            MealSelector(selected = meal, onSelect = { meal = it })
            Spacer(Modifier.height(16.dp))
        }

        BottomActions(
            saveLabel = if (editing != null) "Save changes" else "Save",
            canSave = canSave,
            onSave = save,
            onDelete = onDelete,
        )
    }
}

// ---------------------------------------------------------------- Quick entry

@Composable
private fun QuickStep(
    editing: LogEntry?,
    initialMeal: MealSlot,
    dayNote: String?,
    onBack: () -> Unit,
    onSave: (name: String, protein: Double, kcal: Double, meal: MealSlot) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val startProtein = editing?.protein?.let { Fmt.plain(it) } ?: ""
    var protein by remember { mutableStateOf(TextFieldValue(startProtein, TextRange(0, startProtein.length))) }
    var kcal by remember { mutableStateOf(editing?.kcal?.let { Fmt.plain(it) } ?: "") }
    var name by remember { mutableStateOf(editing?.name?.takeIf { it != DEFAULT_QUICK_NAME } ?: "") }
    var meal by remember { mutableStateOf(initialMeal) }
    val focus = rememberAutoFocus()

    val proteinValue = if (protein.text.isBlank()) 0.0 else Nutrition.parseAmount(protein.text)
    val kcalValue = if (kcal.isBlank()) 0.0 else Nutrition.parseAmount(kcal)
    val canSave = proteinValue != null && kcalValue != null && (proteinValue > 0.0 || kcalValue > 0.0)

    val save: () -> Unit = {
        if (proteinValue != null && kcalValue != null && (proteinValue > 0.0 || kcalValue > 0.0)) {
            onSave(name.trim().ifBlank { DEFAULT_QUICK_NAME }, proteinValue, kcalValue, meal)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PanelTopBar(
            title = "Quick entry",
            subtitle = dayNote ?: "Type protein and calories directly",
            navigationIcon = if (editing == null) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
            navigationLabel = if (editing == null) "Back" else "Close",
            onNavigate = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus),
                    label = { Text("Protein") },
                    suffix = { Text("g") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.merge(NumberStyle),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Calories") },
                    suffix = { Text("kcal") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.merge(NumberStyle),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name (optional)") },
                placeholder = { Text("e.g. Office canteen lunch") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )
            Column {
                Text("Meal", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                MealSelector(selected = meal, onSelect = { meal = it })
            }
            Spacer(Modifier.height(4.dp))
        }
        BottomActions(
            saveLabel = if (editing != null) "Save changes" else "Save",
            canSave = canSave,
            onSave = save,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun BottomActions(
    saveLabel: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onDelete != null) {
            OutlinedButton(onClick = onDelete, modifier = Modifier.height(56.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete")
            }
        }
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Text(saveLabel, style = MaterialTheme.typography.titleMedium)
        }
    }
}

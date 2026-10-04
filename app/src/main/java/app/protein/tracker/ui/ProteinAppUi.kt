package app.protein.tracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.protein.tracker.ui.components.LocalSnack
import app.protein.tracker.ui.components.Snack
import app.protein.tracker.ui.foods.FoodEditorPanel
import app.protein.tracker.ui.foods.FoodsScreen
import app.protein.tracker.ui.history.HistoryScreen
import app.protein.tracker.ui.log.LogRequest
import app.protein.tracker.ui.log.LogScreen
import app.protein.tracker.ui.settings.SettingsScreen
import app.protein.tracker.ui.today.TodayScreen
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Rounded.Home),
    HISTORY("History", Icons.Rounded.DateRange),
    FOODS("Foods", Icons.AutoMirrored.Rounded.List),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

/** Full-screen panels that slide up over the tabs. */
private sealed interface Panel {
    data class Log(val request: LogRequest) : Panel
    data class EditFood(val foodId: Long?, val prefillName: String = "") : Panel
}

@Composable
fun ProteinAppUi() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snack = remember {
        Snack { message, actionLabel, onAction ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
            }
        }
    }

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var historyDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    val tabStates = rememberSaveableStateHolder()

    // Back button: the handler declared last wins when several are enabled.
    BackHandler(enabled = panel == null && tab != Tab.TODAY) { tab = Tab.TODAY }
    BackHandler(enabled = panel == null && tab == Tab.HISTORY && historyDay != null) { historyDay = null }
    BackHandler(enabled = panel != null) { panel = null }

    CompositionLocalProvider(LocalSnack provides snack) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = {
                                    if (tab == item && item == Tab.HISTORY) historyDay = null
                                    tab = item
                                },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                },
                floatingActionButton = {
                    val fab: Pair<String, () -> Unit>? = when {
                        tab == Tab.TODAY -> "Log food" to { panel = Panel.Log(LogRequest.New()) }
                        tab == Tab.HISTORY && historyDay != null ->
                            "Add to this day" to { panel = Panel.Log(LogRequest.New(day = historyDay)) }
                        tab == Tab.FOODS -> "Add food" to { panel = Panel.EditFood(foodId = null) }
                        else -> null
                    }
                    AnimatedContent(
                        targetState = fab?.first,
                        transitionSpec = { scaleIn(tween(200)) + fadeIn(tween(200)) togetherWith scaleOut(tween(150)) + fadeOut(tween(150)) },
                        label = "fab",
                    ) { label ->
                        if (label != null && fab != null) {
                            ExtendedFloatingActionButton(
                                onClick = fab.second,
                                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                text = { Text(label) },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Crossfade(targetState = tab, animationSpec = tween(220), label = "tabs") { current ->
                    tabStates.SaveableStateProvider(current.name) {
                        when (current) {
                            Tab.TODAY -> TodayScreen(
                                contentPadding = innerPadding,
                                onEditEntry = { id -> panel = Panel.Log(LogRequest.Edit(id)) },
                                onLogFood = { panel = Panel.Log(LogRequest.New()) },
                                onOpenSettings = { tab = Tab.SETTINGS },
                            )
                            Tab.HISTORY -> HistoryScreen(
                                contentPadding = innerPadding,
                                selectedDay = historyDay,
                                onSelectDay = { historyDay = it },
                                onEditEntry = { id -> panel = Panel.Log(LogRequest.Edit(id)) },
                            )
                            Tab.FOODS -> FoodsScreen(
                                contentPadding = innerPadding,
                                onEditFood = { id -> panel = Panel.EditFood(foodId = id) },
                                onAddFood = { name -> panel = Panel.EditFood(foodId = null, prefillName = name) },
                            )
                            Tab.SETTINGS -> SettingsScreen(contentPadding = innerPadding)
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = panel,
                transitionSpec = {
                    (slideInVertically(tween(300)) { height -> height / 5 } + fadeIn(tween(240))) togetherWith
                        (slideOutVertically(tween(240)) { height -> height / 5 } + fadeOut(tween(200)))
                },
                label = "panel",
            ) { current ->
                if (current != null) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (current) {
                            is Panel.Log -> LogScreen(request = current.request, onClose = { panel = null })
                            is Panel.EditFood -> FoodEditorPanel(
                                foodId = current.foodId,
                                prefillName = current.prefillName,
                                onClose = { panel = null },
                            )
                        }
                    }
                }
            }
        }
    }
}

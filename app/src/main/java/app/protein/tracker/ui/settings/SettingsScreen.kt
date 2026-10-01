package app.protein.tracker.ui.settings

import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.protein.tracker.data.ProteinRepository
import app.protein.tracker.data.backup.BackupFiles
import app.protein.tracker.data.backup.NotABackupException
import app.protein.tracker.data.settings.SettingsRepository
import app.protein.tracker.domain.Fmt
import app.protein.tracker.domain.Nutrition
import app.protein.tracker.domain.ThemeMode
import app.protein.tracker.domain.UserSettings
import app.protein.tracker.ui.components.LocalSnack
import app.protein.tracker.ui.components.NumberDialog
import app.protein.tracker.ui.components.ScreenTitle
import app.protein.tracker.ui.components.SectionCard
import app.protein.tracker.ui.components.SectionLabel
import app.protein.tracker.ui.components.rememberAppContainer
import app.protein.tracker.ui.components.screenPadding
import app.protein.tracker.ui.components.show
import app.protein.tracker.ui.theme.NumberStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

class SettingsViewModel(
    private val repository: ProteinRepository,
    private val settingsRepository: SettingsRepository,
    private val files: BackupFiles,
) : ViewModel() {
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun exportBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val message = try {
                files.write(uri, repository.exportJson())
                "Backup saved"
            } catch (e: Exception) {
                "Couldn't save the backup"
            }
            onResult(message)
        }
    }

    fun importBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val message = try {
                val summary = repository.importJson(files.read(uri))
                "Restored ${summary.foods} foods and ${summary.entries} entries"
            } catch (e: NotABackupException) {
                e.message ?: "That file isn't a Protein backup"
            } catch (e: Exception) {
                "Couldn't read that file"
            }
            onResult(message)
        }
    }

    fun exportCsv(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val message = try {
                files.write(uri, repository.exportCsv())
                "CSV saved"
            } catch (e: Exception) {
                "Couldn't save the CSV file"
            }
            onResult(message)
        }
    }
}

private enum class SettingsDialog { PROTEIN, KCAL, WEIGHT, DAY_START, IMPORT }

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val container = rememberAppContainer()
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(container.repository, container.settings, container.backupFiles)
    }
    val loaded by viewModel.settings.collectAsStateWithLifecycle()
    val settings = loaded ?: return
    val snack = LocalSnack.current
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup(uri) { snack.show(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importBackup(uri) { snack.show(it) }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.exportCsv(uri) { snack.show(it) }
    }
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(contentPadding, bottomExtra = 24.dp),
    ) {
        item(key = "title") { ScreenTitle(title = "Settings") }

        item(key = "targets") {
            Column {
                SectionLabel("Daily targets")
                SectionCard(padded = false) {
                    SettingRow(
                        title = "Protein",
                        value = "${Fmt.protein(settings.proteinTarget)} g",
                        onClick = { dialog = SettingsDialog.PROTEIN },
                    )
                    Divider()
                    SettingRow(
                        title = "Calories",
                        value = "${Fmt.kcal(settings.kcalTarget)} kcal",
                        onClick = { dialog = SettingsDialog.KCAL },
                    )
                }
            }
        }

        item(key = "weight") {
            Column {
                SectionLabel("Body weight")
                SectionCard(padded = false) {
                    val weight = settings.bodyWeightKg
                    SettingRow(
                        title = "Body weight",
                        subtitle = "Optional. Used only to suggest a protein target.",
                        value = weight?.let { "${Fmt.amount(it)} kg" } ?: "Not set",
                        onClick = { dialog = SettingsDialog.WEIGHT },
                    )
                    if (weight != null) {
                        Divider()
                        WeightSuggestion(
                            weightKg = weight,
                            currentTarget = settings.proteinTarget,
                            onUse = { grams -> viewModel.update { it.copy(proteinTarget = grams) } },
                        )
                    }
                }
            }
        }

        item(key = "day") {
            Column {
                SectionLabel("Day boundary")
                SectionCard {
                    Text("A new day starts at", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    val isMidnight = settings.dayStartMinutes == 0
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = isMidnight,
                            onClick = { viewModel.update { it.copy(dayStartMinutes = 0) } },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("Midnight") }
                        SegmentedButton(
                            selected = !isMidnight,
                            onClick = {
                                if (isMidnight) {
                                    viewModel.update { it.copy(dayStartMinutes = UserSettings.DEFAULT_DAY_START_MINUTES) }
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("Custom time") }
                    }
                    if (!isMidnight) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { dialog = SettingsDialog.DAY_START },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Start time", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                Fmt.timeOfDay(settings.dayStartMinutes),
                                style = MaterialTheme.typography.titleMedium.merge(NumberStyle),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isMidnight) {
                            "Each day runs from midnight to midnight."
                        } else {
                            "Anything you log before ${Fmt.timeOfDay(settings.dayStartMinutes)} counts toward the previous day, so late-night snacks stay with the day you ate them."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "appearance") {
            Column {
                SectionLabel("Appearance")
                SectionCard {
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.update { it.copy(themeMode = mode) } },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                            ) { Text(mode.label) }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { viewModel.update { it.copy(dynamicColor = !it.dynamicColor) } },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Wallpaper colours", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Use Material You colours from your wallpaper",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = settings.dynamicColor,
                                onCheckedChange = { checked -> viewModel.update { it.copy(dynamicColor = checked) } },
                            )
                        }
                    }
                }
            }
        }

        item(key = "backup") {
            Column {
                SectionLabel("Backup")
                SectionCard(padded = false) {
                    SettingRow(
                        title = "Export backup",
                        subtitle = "Save all foods, entries and settings as a JSON file",
                        onClick = { exportLauncher.launch("protein-backup-${LocalDate.now()}.json") },
                    )
                    Divider()
                    SettingRow(
                        title = "Import backup",
                        subtitle = "Replace everything with a backup file",
                        onClick = { dialog = SettingsDialog.IMPORT },
                    )
                    Divider()
                    SettingRow(
                        title = "Export entries as CSV",
                        subtitle = "Opens in Excel or Google Sheets",
                        onClick = { csvLauncher.launch("protein-entries-${LocalDate.now()}.csv") },
                    )
                }
            }
        }

        item(key = "about") {
            Text(
                "Protein $versionName · Works fully offline. Your data stays on this phone unless you export it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 24.dp),
            )
        }
    }

    when (dialog) {
        SettingsDialog.PROTEIN -> NumberDialog(
            title = "Daily protein target",
            initial = settings.proteinTarget,
            suffix = "g",
            onDismiss = { dialog = null },
            onConfirm = { value ->
                if (value != null) viewModel.update { it.copy(proteinTarget = value) }
                dialog = null
            },
        )
        SettingsDialog.KCAL -> NumberDialog(
            title = "Daily calorie target",
            initial = settings.kcalTarget,
            suffix = "kcal",
            onDismiss = { dialog = null },
            onConfirm = { value ->
                if (value != null) viewModel.update { it.copy(kcalTarget = value) }
                dialog = null
            },
        )
        SettingsDialog.WEIGHT -> NumberDialog(
            title = "Body weight",
            initial = settings.bodyWeightKg,
            suffix = "kg",
            message = "Used only to suggest a protein range of 1.6–2.0 g per kg.",
            allowClear = true,
            onDismiss = { dialog = null },
            onConfirm = { value ->
                viewModel.update { it.copy(bodyWeightKg = value) }
                dialog = null
            },
        )
        SettingsDialog.DAY_START -> DayStartDialog(
            initialMinutes = settings.dayStartMinutes,
            onDismiss = { dialog = null },
            onConfirm = { minutes ->
                viewModel.update { it.copy(dayStartMinutes = minutes) }
                dialog = null
            },
        )
        SettingsDialog.IMPORT -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Import a backup?") },
            text = {
                Text(
                    "This replaces all your current foods, entries and settings with the ones in the backup file. " +
                        "Export a backup first if you want to keep what's here now."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text("Choose file") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Cancel") }
            },
        )
        null -> Unit
    }
}

@Composable
private fun SettingRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    value: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.merge(NumberStyle),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun WeightSuggestion(weightKg: Double, currentTarget: Double, onUse: (Double) -> Unit) {
    val range = Nutrition.suggestedProteinRange(weightKg)
    val suggested = (weightKg * 1.8).roundToInt().toDouble()
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            "Suggested: ${Fmt.protein(range.start)}–${Fmt.protein(range.endInclusive)} g protein a day",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "1.6–2.0 g per kg of body weight",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(
            onClick = { onUse(suggested) },
            enabled = currentTarget != suggested,
        ) {
            Text(
                if (currentTarget == suggested) {
                    "Using ${Fmt.protein(suggested)} g (1.8 g/kg)"
                } else {
                    "Use ${Fmt.protein(suggested)} g (1.8 g/kg) as target"
                }
            )
        }
    }
}

@Composable
private fun DayStartDialog(initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Day starts at") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

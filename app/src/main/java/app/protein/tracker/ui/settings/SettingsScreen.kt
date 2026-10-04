package app.protein.tracker.ui.settings

import android.net.Uri
import app.protein.tracker.work.Background
import app.protein.tracker.work.AutoBackup
import app.protein.tracker.domain.Sex
import app.protein.tracker.domain.JobType
import app.protein.tracker.domain.GymLevel
import app.protein.tracker.domain.Goal
import app.protein.tracker.domain.Energy
import app.protein.tracker.domain.ActivityInput
import androidx.compose.foundation.layout.Arrangement
import android.content.Intent
import android.Manifest
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

private enum class SettingsDialog { PROTEIN, KCAL, WEIGHT, DAY_START, IMPORT, AGE, HEIGHT, WORK_HOURS, REMIND_0, REMIND_1, REMIND_2, CHECK_IN }

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
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.update { it.copy(remindersOn = true) }
        } else {
            snack.show("Allow notifications for Protein in your phone's settings to get reminders")
        }
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            viewModel.update { it.copy(autoBackupFolder = uri.toString()) }
            Background.backupNow(context)
            snack.show("Daily backup turned on")
        }
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

        item(key = "calories") {
            Column {
                SectionLabel("Calorie target")
                SectionCard(padded = false) {
                    SwitchRow(
                        title = "Automatic calorie target",
                        subtitle = "Adjusts each day to your work hours and gym. Uses your weight, height, age and sex.",
                        checked = settings.autoCalories,
                        onChange = { on -> viewModel.update { it.copy(autoCalories = on) } },
                    )
                    if (settings.autoCalories) {
                        Divider()
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Text("Sex", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                Sex.entries.forEachIndexed { index, sex ->
                                    SegmentedButton(
                                        selected = settings.sex == sex,
                                        onClick = { viewModel.update { it.copy(sex = sex) } },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Sex.entries.size),
                                    ) { Text(sex.label) }
                                }
                            }
                        }
                        Divider()
                        SettingRow(
                            title = "Age",
                            value = settings.ageYears?.let { "$it years" } ?: "Not set",
                            onClick = { dialog = SettingsDialog.AGE },
                        )
                        Divider()
                        SettingRow(
                            title = "Height",
                            subtitle = settings.heightCm?.let { feetInches(it) },
                            value = settings.heightCm?.let { "${Fmt.amount(it)} cm" } ?: "Not set",
                            onClick = { dialog = SettingsDialog.HEIGHT },
                        )
                        Divider()
                        SettingRow(
                            title = "Body weight",
                            value = settings.bodyWeightKg?.let { "${Fmt.amount(it)} kg" } ?: "Not set",
                            onClick = { dialog = SettingsDialog.WEIGHT },
                        )
                        Divider()
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Text("Your job", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                JobType.entries.forEachIndexed { index, job ->
                                    SegmentedButton(
                                        selected = settings.jobType == job,
                                        onClick = { viewModel.update { it.copy(jobType = job) } },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = JobType.entries.size),
                                    ) { Text(job.label, maxLines = 1) }
                                }
                            }
                        }
                        Divider()
                        SettingRow(
                            title = "Usual work hours",
                            subtitle = "Used until you answer the daily check-in",
                            value = "${Fmt.amount(settings.usualWorkHours)} h",
                            onClick = { dialog = SettingsDialog.WORK_HOURS },
                        )
                        Divider()
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Text("Goal", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                Goal.entries.forEachIndexed { index, goal ->
                                    SegmentedButton(
                                        selected = settings.goal == goal,
                                        onClick = { viewModel.update { it.copy(goal = goal) } },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Goal.entries.size),
                                    ) { Text(goal.label, maxLines = 1) }
                                }
                            }
                        }
                        val rest = Energy.dayTarget(settings, ActivityInput(false, 0.0, GymLevel.NONE, 0))
                        val work = Energy.dayTarget(settings, ActivityInput(true, settings.usualWorkHours, GymLevel.NONE, 0))
                        val gym = Energy.dayTarget(settings, ActivityInput(true, settings.usualWorkHours, GymLevel.MODERATE, 60))
                        Divider()
                        Text(
                            if (rest != null && work != null && gym != null) {
                                "Day off: ${Fmt.kcal(rest)} kcal · Work day: ${Fmt.kcal(work)} kcal · " +
                                    "Work + 1 h gym: ${Fmt.kcal(gym)} kcal"
                            } else {
                                "Fill in sex, age, height and weight to see your targets."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
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

        item(key = "reminders") {
            Column {
                SectionLabel("Notifications")
                SectionCard(padded = false) {
                    SwitchRow(
                        title = "Progress reminders",
                        subtitle = "Tells you how much protein and how many calories are still to go",
                        checked = settings.remindersOn,
                        onChange = { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.update { it.copy(remindersOn = on) }
                            }
                        },
                    )
                    if (settings.remindersOn) {
                        listOf(SettingsDialog.REMIND_0, SettingsDialog.REMIND_1, SettingsDialog.REMIND_2)
                            .forEachIndexed { index, which ->
                                Divider()
                                SettingRow(
                                    title = "Reminder ${index + 1}",
                                    value = settings.reminderTimes.getOrNull(index)?.let { Fmt.timeOfDay(it) } ?: "Off",
                                    onClick = { dialog = which },
                                )
                            }
                        if (settings.autoCalories) {
                            Divider()
                            SwitchRow(
                                title = "Morning check-in",
                                subtitle = "Asks about work and gym at ${Fmt.timeOfDay(settings.checkInTime)}",
                                checked = settings.checkInReminderOn,
                                onChange = { on -> viewModel.update { it.copy(checkInReminderOn = on) } },
                            )
                            if (settings.checkInReminderOn) {
                                Divider()
                                SettingRow(
                                    title = "Check-in time",
                                    value = Fmt.timeOfDay(settings.checkInTime),
                                    onClick = { dialog = SettingsDialog.CHECK_IN },
                                )
                            }
                        }
                    }
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
                    Divider()
                    val folder = settings.autoBackupFolder
                    if (folder == null) {
                        SettingRow(
                            title = "Automatic daily backup",
                            subtitle = "Pick a folder once. A backup is saved there every day; the newest 10 are kept.",
                            value = "Off",
                            onClick = { folderLauncher.launch(null) },
                        )
                    } else {
                        SettingRow(
                            title = "Automatic daily backup",
                            subtitle = "Folder: ${AutoBackup.folderLabel(folder)} · " +
                                (settings.lastAutoBackupAt?.let { "last backup ${lastBackupText(it)}" } ?: "no backup yet"),
                            value = "On",
                            onClick = { folderLauncher.launch(null) },
                        )
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(onClick = {
                                Background.backupNow(context)
                                snack.show("Backing up now")
                            }) { Text("Back up now") }
                            TextButton(onClick = { viewModel.update { it.copy(autoBackupFolder = null) } }) {
                                Text("Turn off")
                            }
                        }
                    }
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
        SettingsDialog.AGE -> NumberDialog(
            title = "Age",
            initial = settings.ageYears?.toDouble(),
            suffix = "years",
            onDismiss = { dialog = null },
            onConfirm = { value ->
                viewModel.update { it.copy(ageYears = value?.toInt()?.coerceIn(10, 100)) }
                dialog = null
            },
        )
        SettingsDialog.HEIGHT -> NumberDialog(
            title = "Height",
            initial = settings.heightCm,
            suffix = "cm",
            message = "In centimetres. For example 5 ft 4 in = 163 cm, 5 ft 8 in = 173 cm.",
            onDismiss = { dialog = null },
            onConfirm = { value ->
                viewModel.update { it.copy(heightCm = value?.coerceIn(100.0, 230.0)) }
                dialog = null
            },
        )
        SettingsDialog.WORK_HOURS -> NumberDialog(
            title = "Usual work hours",
            initial = settings.usualWorkHours,
            suffix = "hours",
            onDismiss = { dialog = null },
            onConfirm = { value ->
                if (value != null) viewModel.update { it.copy(usualWorkHours = value.coerceIn(0.5, 16.0)) }
                dialog = null
            },
        )
        SettingsDialog.REMIND_0, SettingsDialog.REMIND_1, SettingsDialog.REMIND_2 -> {
            val index = dialog!!.ordinal - SettingsDialog.REMIND_0.ordinal
            TimeDialog(
                title = "Reminder ${index + 1}",
                initialMinutes = settings.reminderTimes.getOrNull(index) ?: (12 * 60),
                onDismiss = { dialog = null },
                onConfirm = { minutes ->
                    viewModel.update {
                        val times = it.reminderTimes.toMutableList()
                        while (times.size <= index) times += 12 * 60
                        times[index] = minutes
                        it.copy(reminderTimes = times)
                    }
                    dialog = null
                },
            )
        }
        SettingsDialog.CHECK_IN -> TimeDialog(
            title = "Check-in time",
            initialMinutes = settings.checkInTime,
            onDismiss = { dialog = null },
            onConfirm = { minutes ->
                viewModel.update { it.copy(checkInTime = minutes) }
                dialog = null
            },
        )
        SettingsDialog.DAY_START -> TimeDialog(
            title = "Day starts at",
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
private fun TimeDialog(title: String, initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
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
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** 162.6 cm → "5 ft 4 in" */
private fun feetInches(cm: Double): String {
    val totalInches = (cm / 2.54).roundToInt()
    return "${totalInches / 12} ft ${totalInches % 12} in"
}

private fun lastBackupText(millis: Long): String {
    val time = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
    val day = time.toLocalDate()
    val clock = Fmt.timeOfDay(time.hour * 60 + time.minute)
    return when (day) {
        LocalDate.now() -> "today $clock"
        LocalDate.now().minusDays(1) -> "yesterday $clock"
        else -> Fmt.shortDate(day.toEpochDay())
    }
}

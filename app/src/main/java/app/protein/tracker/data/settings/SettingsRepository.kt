package app.protein.tracker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.protein.tracker.domain.Goal
import app.protein.tracker.domain.JobType
import app.protein.tracker.domain.Sex
import app.protein.tracker.domain.ThemeMode
import app.protein.tracker.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.settingsStore

    private object Keys {
        val PROTEIN_TARGET = doublePreferencesKey("protein_target")
        val KCAL_TARGET = doublePreferencesKey("kcal_target")
        val BODY_WEIGHT = doublePreferencesKey("body_weight_kg")
        val DAY_START = intPreferencesKey("day_start_minutes")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AUTO_CALORIES = booleanPreferencesKey("auto_calories")
        val SEX = stringPreferencesKey("sex")
        val AGE = intPreferencesKey("age")
        val HEIGHT = doublePreferencesKey("height_cm")
        val JOB = stringPreferencesKey("job_type")
        val WORK_HOURS = doublePreferencesKey("usual_work_hours")
        val GOAL = stringPreferencesKey("goal")
        val REMINDERS_ON = booleanPreferencesKey("reminders_on")
        val REMINDER_TIMES = stringPreferencesKey("reminder_times")
        val CHECK_IN_ON = booleanPreferencesKey("check_in_reminder_on")
        val CHECK_IN_TIME = intPreferencesKey("check_in_time")
        val BACKUP_FOLDER = stringPreferencesKey("auto_backup_folder")
        val LAST_BACKUP = longPreferencesKey("last_auto_backup_at")
    }

    val settings: Flow<UserSettings> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toSettings() }
        .distinctUntilChanged()

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    suspend fun replace(settings: UserSettings) {
        store.edit { prefs -> prefs.write(settings) }
    }

    private fun Preferences.toSettings() = UserSettings(
        proteinTarget = this[Keys.PROTEIN_TARGET] ?: UserSettings.DEFAULT_PROTEIN_TARGET,
        kcalTarget = this[Keys.KCAL_TARGET] ?: UserSettings.DEFAULT_KCAL_TARGET,
        bodyWeightKg = this[Keys.BODY_WEIGHT],
        dayStartMinutes = this[Keys.DAY_START] ?: UserSettings.DEFAULT_DAY_START_MINUTES,
        themeMode = ThemeMode.entries.firstOrNull { it.name == this[Keys.THEME] } ?: ThemeMode.SYSTEM,
        dynamicColor = this[Keys.DYNAMIC_COLOR] ?: true,
        autoCalories = this[Keys.AUTO_CALORIES] ?: false,
        sex = Sex.entries.firstOrNull { it.name == this[Keys.SEX] },
        ageYears = this[Keys.AGE],
        heightCm = this[Keys.HEIGHT],
        jobType = JobType.entries.firstOrNull { it.name == this[Keys.JOB] } ?: JobType.FIELD,
        usualWorkHours = this[Keys.WORK_HOURS] ?: 7.0,
        goal = Goal.entries.firstOrNull { it.name == this[Keys.GOAL] } ?: Goal.MAINTAIN,
        remindersOn = this[Keys.REMINDERS_ON] ?: false,
        reminderTimes = this[Keys.REMINDER_TIMES]
            ?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.takeIf { it.isNotEmpty() }
            ?: UserSettings().reminderTimes,
        checkInReminderOn = this[Keys.CHECK_IN_ON] ?: true,
        checkInTime = this[Keys.CHECK_IN_TIME] ?: (9 * 60),
        autoBackupFolder = this[Keys.BACKUP_FOLDER],
        lastAutoBackupAt = this[Keys.LAST_BACKUP],
    )

    private fun MutablePreferences.write(settings: UserSettings) {
        this[Keys.PROTEIN_TARGET] = settings.proteinTarget
        this[Keys.KCAL_TARGET] = settings.kcalTarget
        val weight = settings.bodyWeightKg
        if (weight != null) this[Keys.BODY_WEIGHT] = weight else remove(Keys.BODY_WEIGHT)
        this[Keys.DAY_START] = settings.dayStartMinutes
        this[Keys.THEME] = settings.themeMode.name
        this[Keys.DYNAMIC_COLOR] = settings.dynamicColor
        this[Keys.AUTO_CALORIES] = settings.autoCalories
        settings.sex?.let { this[Keys.SEX] = it.name } ?: remove(Keys.SEX)
        settings.ageYears?.let { this[Keys.AGE] = it } ?: remove(Keys.AGE)
        settings.heightCm?.let { this[Keys.HEIGHT] = it } ?: remove(Keys.HEIGHT)
        this[Keys.JOB] = settings.jobType.name
        this[Keys.WORK_HOURS] = settings.usualWorkHours
        this[Keys.GOAL] = settings.goal.name
        this[Keys.REMINDERS_ON] = settings.remindersOn
        this[Keys.REMINDER_TIMES] = settings.reminderTimes.joinToString(",")
        this[Keys.CHECK_IN_ON] = settings.checkInReminderOn
        this[Keys.CHECK_IN_TIME] = settings.checkInTime
        settings.autoBackupFolder?.let { this[Keys.BACKUP_FOLDER] = it } ?: remove(Keys.BACKUP_FOLDER)
        settings.lastAutoBackupAt?.let { this[Keys.LAST_BACKUP] = it } ?: remove(Keys.LAST_BACKUP)
    }
}

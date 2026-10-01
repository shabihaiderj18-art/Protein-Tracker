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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    )

    private fun MutablePreferences.write(settings: UserSettings) {
        this[Keys.PROTEIN_TARGET] = settings.proteinTarget
        this[Keys.KCAL_TARGET] = settings.kcalTarget
        val weight = settings.bodyWeightKg
        if (weight != null) this[Keys.BODY_WEIGHT] = weight else remove(Keys.BODY_WEIGHT)
        this[Keys.DAY_START] = settings.dayStartMinutes
        this[Keys.THEME] = settings.themeMode.name
        this[Keys.DYNAMIC_COLOR] = settings.dynamicColor
    }
}

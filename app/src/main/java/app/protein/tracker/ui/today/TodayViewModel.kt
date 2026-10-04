package app.protein.tracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.protein.tracker.data.ProteinRepository
import app.protein.tracker.data.db.DayActivity
import app.protein.tracker.data.db.LogEntry
import app.protein.tracker.data.logicalToday
import app.protein.tracker.data.settings.SettingsRepository
import app.protein.tracker.domain.ActivityInput
import app.protein.tracker.domain.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val loaded: Boolean = false,
    val day: Long = 0,
    val settings: UserSettings = UserSettings(),
    val entries: List<LogEntry> = emptyList(),
    val activity: DayActivity? = null,
    val kcalTarget: Double = UserSettings.DEFAULT_KCAL_TARGET,
) {
    val protein: Double get() = entries.sumOf { it.protein }
    val kcal: Double get() = entries.sumOf { it.kcal }
    val carbs: Double get() = entries.sumOf { it.carbs }
    val fat: Double get() = entries.sumOf { it.fat }
    val fiber: Double get() = entries.sumOf { it.fiber }
}

class TodayViewModel(
    private val repository: ProteinRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val dayData = logicalToday(settingsRepository.settings)
        .flatMapLatest { day ->
            combine(repository.observeDay(day), repository.observeActivity(day)) { entries, activity ->
                Triple(day, entries, activity)
            }
        }

    val state: StateFlow<TodayUiState> = combine(settingsRepository.settings, dayData) { settings, (day, entries, activity) ->
        TodayUiState(
            loaded = true,
            day = day,
            settings = settings,
            entries = entries,
            activity = activity,
            kcalTarget = repository.kcalTargetFor(settings, activity),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun saveActivity(day: Long, input: ActivityInput) {
        viewModelScope.launch {
            repository.saveActivity(
                DayActivity(day, input.worked, input.workHours, input.gym, input.gymMinutes)
            )
        }
    }

    fun usualDay(settings: UserSettings): ActivityInput = repository.usualDay(settings)
}

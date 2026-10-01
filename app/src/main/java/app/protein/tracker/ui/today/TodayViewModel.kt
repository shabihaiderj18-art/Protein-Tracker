package app.protein.tracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.protein.tracker.data.ProteinRepository
import app.protein.tracker.data.db.LogEntry
import app.protein.tracker.data.logicalToday
import app.protein.tracker.data.settings.SettingsRepository
import app.protein.tracker.domain.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val loaded: Boolean = false,
    val day: Long = 0,
    val settings: UserSettings = UserSettings(),
    val entries: List<LogEntry> = emptyList(),
) {
    val protein: Double get() = entries.sumOf { it.protein }
    val kcal: Double get() = entries.sumOf { it.kcal }
}

class TodayViewModel(
    repository: ProteinRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val dayEntries = logicalToday(settingsRepository.settings)
        .flatMapLatest { day -> repository.observeDay(day).map { entries -> day to entries } }

    val state: StateFlow<TodayUiState> = combine(settingsRepository.settings, dayEntries) { settings, (day, entries) ->
        TodayUiState(loaded = true, day = day, settings = settings, entries = entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())
}

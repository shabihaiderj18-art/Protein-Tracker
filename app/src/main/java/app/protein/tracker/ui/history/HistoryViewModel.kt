package app.protein.tracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.protein.tracker.data.ProteinRepository
import app.protein.tracker.data.logicalToday
import app.protein.tracker.data.settings.SettingsRepository
import app.protein.tracker.domain.DayTotal
import app.protein.tracker.domain.MonthAverage
import app.protein.tracker.domain.Stats
import app.protein.tracker.domain.UserSettings
import app.protein.tracker.domain.WeekSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

data class HistoryUiState(
    val loaded: Boolean = false,
    val today: Long = 0,
    val proteinTarget: Double = UserSettings.DEFAULT_PROTEIN_TARGET,
    val week: WeekSummary = WeekSummary(emptyList(), emptyList(), null, null, 0),
    val weekOffset: Int = 0,
    val month: YearMonth = YearMonth.now(),
    val monthTotals: Map<Long, DayTotal> = emptyMap(),
    val canGoNextMonth: Boolean = false,
    val monthly: List<MonthAverage> = emptyList(),
    val hasAnyData: Boolean = false,
)

class HistoryViewModel(
    repository: ProteinRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    /** 0 = the last seven days, 1 = the seven days before that, and so on. */
    private val weekOffset = MutableStateFlow(0)

    /** Null = the current month. */
    private val shownMonth = MutableStateFlow<YearMonth?>(null)

    val state: StateFlow<HistoryUiState> = combine(
        settingsRepository.settings,
        logicalToday(settingsRepository.settings),
        repository.dayTotals,
        weekOffset,
        shownMonth,
    ) { settings, today, totals, offset, monthChoice ->
        val currentMonth = YearMonth.from(LocalDate.ofEpochDay(today))
        val month = monthChoice ?: currentMonth
        HistoryUiState(
            loaded = true,
            today = today,
            proteinTarget = settings.proteinTarget,
            week = Stats.week(totals.associateBy { it.day }, today - 7L * offset),
            weekOffset = offset,
            month = month,
            monthTotals = Stats.inMonth(totals, month),
            canGoNextMonth = month < currentMonth,
            monthly = Stats.monthly(totals),
            hasAnyData = totals.isNotEmpty(),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun previousWeek() = weekOffset.update { it + 1 }

    fun nextWeek() = weekOffset.update { (it - 1).coerceAtLeast(0) }

    fun showMonth(month: YearMonth) {
        shownMonth.value = month
    }
}

package app.protein.tracker.data

import app.protein.tracker.domain.DayClock
import app.protein.tracker.domain.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Emits the current logical day, and again whenever it changes, for example when the app is
 * left open past the day-start time.
 */
fun logicalToday(settings: Flow<UserSettings>): Flow<Long> =
    settings
        .map { it.dayStartMinutes }
        .distinctUntilChanged()
        .flatMapLatest { dayStart ->
            flow {
                while (true) {
                    emit(DayClock.today(dayStart))
                    delay(30_000)
                }
            }
        }
        .distinctUntilChanged()

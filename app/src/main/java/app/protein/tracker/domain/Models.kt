package app.protein.tracker.domain

import java.time.LocalTime
import java.time.YearMonth

/** What a food's per-100 values refer to: 100 g or 100 ml. */
enum class BaseUnit(val symbol: String, val longName: String) {
    GRAM("g", "Grams"),
    ML("ml", "Millilitres"),
}

/** The four meal slots. The slot is picked from the clock when logging, and can be changed. */
enum class MealSlot(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night");

    companion object {
        fun at(time: LocalTime): MealSlot = when (time.hour) {
            in 4..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..20 -> EVENING
            else -> NIGHT
        }
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

data class UserSettings(
    val proteinTarget: Double = DEFAULT_PROTEIN_TARGET,
    val kcalTarget: Double = DEFAULT_KCAL_TARGET,
    val bodyWeightKg: Double? = null,
    /** Minutes after midnight when a new day starts. 0 = midnight, 240 = 4:00 AM. */
    val dayStartMinutes: Int = DEFAULT_DAY_START_MINUTES,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
) {
    companion object {
        const val DEFAULT_PROTEIN_TARGET = 110.0
        const val DEFAULT_KCAL_TARGET = 2000.0
        const val DEFAULT_DAY_START_MINUTES = 240
    }
}

/** Sum of everything logged on one day. [day] is a LocalDate epoch day. */
data class DayTotal(
    val day: Long,
    val protein: Double,
    val kcal: Double,
    val entryCount: Int,
)

data class MonthAverage(
    val month: YearMonth,
    val avgProtein: Double,
    val avgKcal: Double,
    val loggedDays: Int,
)

data class WeekSummary(
    val days: List<Long>,
    val totals: List<DayTotal?>,
    val avgProtein: Double?,
    val avgKcal: Double?,
    val loggedDays: Int,
)

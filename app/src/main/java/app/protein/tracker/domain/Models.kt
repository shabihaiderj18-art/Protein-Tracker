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

enum class Sex(val label: String) { MALE("Male"), FEMALE("Female") }

/** Extra energy per working hour comes from the job's MET value (1 MET = resting). */
enum class JobType(val label: String, val met: Double) {
    DESK("Desk", 1.5),
    ON_FEET("On my feet", 2.5),
    FIELD("Field work", 3.5),
}

enum class GymLevel(val label: String, val met: Double) {
    NONE("No gym", 0.0),
    LIGHT("Light", 3.5),
    MODERATE("Moderate", 5.0),
    HARD("Hard", 6.5),
}

enum class Goal(val label: String, val kcalAdjust: Double) {
    LOSE("Lose fat", -400.0),
    MAINTAIN("Maintain", 0.0),
    GAIN("Gain muscle", 300.0),
}

data class UserSettings(
    val proteinTarget: Double = DEFAULT_PROTEIN_TARGET,
    val kcalTarget: Double = DEFAULT_KCAL_TARGET,
    val bodyWeightKg: Double? = null,
    /** Minutes after midnight when a new day starts. 0 = midnight, 240 = 4:00 AM. */
    val dayStartMinutes: Int = DEFAULT_DAY_START_MINUTES,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    // Activity-based calorie target
    val autoCalories: Boolean = false,
    val sex: Sex? = null,
    val ageYears: Int? = null,
    val heightCm: Double? = null,
    val jobType: JobType = JobType.FIELD,
    val usualWorkHours: Double = 7.0,
    val goal: Goal = Goal.MAINTAIN,
    // Notifications (minutes after midnight)
    val remindersOn: Boolean = false,
    val reminderTimes: List<Int> = listOf(13 * 60, 18 * 60, 21 * 60),
    val checkInReminderOn: Boolean = true,
    val checkInTime: Int = 9 * 60,
    // Automatic backup to a folder the user picked
    val autoBackupFolder: String? = null,
    val lastAutoBackupAt: Long? = null,
) {
    val profileComplete: Boolean
        get() = sex != null && ageYears != null && heightCm != null && bodyWeightKg != null

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

/** What you did on one day, answered in the daily check-in. */
data class ActivityInput(
    val worked: Boolean,
    val workHours: Double,
    val gym: GymLevel,
    val gymMinutes: Int,
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

package app.protein.tracker.data.backup

import app.protein.tracker.data.db.DayActivity
import app.protein.tracker.data.db.Food
import app.protein.tracker.data.db.LogEntry
import app.protein.tracker.domain.BaseUnit
import app.protein.tracker.domain.Goal
import app.protein.tracker.domain.GymLevel
import app.protein.tracker.domain.JobType
import app.protein.tracker.domain.MealSlot
import app.protein.tracker.domain.Sex
import app.protein.tracker.domain.ThemeMode
import app.protein.tracker.domain.UserSettings
import java.time.LocalDate

fun UserSettings.toBackup() = BackupSettings(
    proteinTarget = proteinTarget,
    kcalTarget = kcalTarget,
    bodyWeightKg = bodyWeightKg,
    dayStartMinutes = dayStartMinutes,
    theme = themeMode.name,
    dynamicColor = dynamicColor,
    autoCalories = autoCalories,
    sex = sex?.name,
    ageYears = ageYears,
    heightCm = heightCm,
    jobType = jobType.name,
    usualWorkHours = usualWorkHours,
    goal = goal.name,
    remindersOn = remindersOn,
    reminderTimes = reminderTimes,
    checkInReminderOn = checkInReminderOn,
    checkInTime = checkInTime,
)

fun BackupSettings.toSettings() = UserSettings(
    proteinTarget = proteinTarget.takeIf { it > 0 } ?: UserSettings.DEFAULT_PROTEIN_TARGET,
    kcalTarget = kcalTarget.takeIf { it > 0 } ?: UserSettings.DEFAULT_KCAL_TARGET,
    bodyWeightKg = bodyWeightKg?.takeIf { it > 0 },
    dayStartMinutes = dayStartMinutes.coerceIn(0, 24 * 60 - 1),
    themeMode = ThemeMode.entries.firstOrNull { it.name == theme } ?: ThemeMode.SYSTEM,
    dynamicColor = dynamicColor,
    autoCalories = autoCalories,
    sex = Sex.entries.firstOrNull { it.name == sex },
    ageYears = ageYears?.takeIf { it in 10..100 },
    heightCm = heightCm?.takeIf { it > 0 },
    jobType = JobType.entries.firstOrNull { it.name == jobType } ?: JobType.FIELD,
    usualWorkHours = usualWorkHours.coerceIn(0.0, 16.0),
    goal = Goal.entries.firstOrNull { it.name == goal } ?: Goal.MAINTAIN,
    remindersOn = remindersOn,
    reminderTimes = reminderTimes.filter { it in 0 until 24 * 60 }.ifEmpty { listOf(780, 1080, 1260) },
    checkInReminderOn = checkInReminderOn,
    checkInTime = checkInTime.coerceIn(0, 24 * 60 - 1),
)

fun DayActivity.toBackup() = BackupActivity(
    date = LocalDate.ofEpochDay(day).toString(),
    worked = worked,
    workHours = workHours,
    gym = gym.name,
    gymMinutes = gymMinutes,
)

fun BackupActivity.toActivity() = DayActivity(
    day = LocalDate.parse(date).toEpochDay(),
    worked = worked,
    workHours = workHours,
    gym = GymLevel.entries.firstOrNull { it.name == gym } ?: GymLevel.NONE,
    gymMinutes = gymMinutes,
)

fun Food.toBackup() = BackupFood(
    id = id,
    name = name,
    kcalPer100 = kcalPer100,
    proteinPer100 = proteinPer100,
    baseUnit = baseUnit.name,
    unitName = unitName,
    unitSize = unitSize,
    note = note,
    favorite = isFavorite,
    lastUsedAt = lastUsedAt,
    useCount = useCount,
    lastAmount = lastAmount,
    lastInUnits = lastInUnits,
    carbsPer100 = carbsPer100,
    fatPer100 = fatPer100,
    fiberPer100 = fiberPer100,
)

fun BackupFood.toFood() = Food(
    id = id,
    name = name,
    kcalPer100 = kcalPer100,
    proteinPer100 = proteinPer100,
    baseUnit = baseUnitOf(baseUnit),
    unitName = unitName,
    unitSize = unitSize,
    note = note,
    isFavorite = favorite,
    lastUsedAt = lastUsedAt,
    useCount = useCount,
    lastAmount = lastAmount,
    lastInUnits = lastInUnits,
    carbsPer100 = carbsPer100,
    fatPer100 = fatPer100,
    fiberPer100 = fiberPer100,
)

fun LogEntry.toBackup() = BackupEntry(
    id = id,
    date = LocalDate.ofEpochDay(day).toString(),
    loggedAt = loggedAt,
    meal = meal.name,
    foodId = foodId,
    name = name,
    amount = amount,
    inUnits = inUnits,
    unitName = unitName,
    unitSize = unitSize,
    quantity = quantity,
    baseUnit = baseUnit.name,
    proteinPer100 = proteinPer100,
    kcalPer100 = kcalPer100,
    protein = protein,
    kcal = kcal,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    carbsPer100 = carbsPer100,
    fatPer100 = fatPer100,
    fiberPer100 = fiberPer100,
)

fun BackupEntry.toEntry() = LogEntry(
    id = id,
    day = LocalDate.parse(date).toEpochDay(),
    loggedAt = loggedAt,
    meal = MealSlot.entries.firstOrNull { it.name == meal } ?: MealSlot.MORNING,
    foodId = foodId,
    name = name,
    amount = amount,
    inUnits = inUnits,
    unitName = unitName,
    unitSize = unitSize,
    quantity = quantity,
    baseUnit = baseUnitOf(baseUnit),
    proteinPer100 = proteinPer100,
    kcalPer100 = kcalPer100,
    protein = protein,
    kcal = kcal,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    carbsPer100 = carbsPer100,
    fatPer100 = fatPer100,
    fiberPer100 = fiberPer100,
)

private fun baseUnitOf(name: String): BaseUnit =
    BaseUnit.entries.firstOrNull { it.name == name } ?: BaseUnit.GRAM

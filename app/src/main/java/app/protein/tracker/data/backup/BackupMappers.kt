package app.protein.tracker.data.backup

import app.protein.tracker.data.db.Food
import app.protein.tracker.data.db.LogEntry
import app.protein.tracker.domain.BaseUnit
import app.protein.tracker.domain.MealSlot
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
)

fun BackupSettings.toSettings() = UserSettings(
    proteinTarget = proteinTarget.takeIf { it > 0 } ?: UserSettings.DEFAULT_PROTEIN_TARGET,
    kcalTarget = kcalTarget.takeIf { it > 0 } ?: UserSettings.DEFAULT_KCAL_TARGET,
    bodyWeightKg = bodyWeightKg?.takeIf { it > 0 },
    dayStartMinutes = dayStartMinutes.coerceIn(0, 24 * 60 - 1),
    themeMode = ThemeMode.entries.firstOrNull { it.name == theme } ?: ThemeMode.SYSTEM,
    dynamicColor = dynamicColor,
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
)

private fun baseUnitOf(name: String): BaseUnit =
    BaseUnit.entries.firstOrNull { it.name == name } ?: BaseUnit.GRAM

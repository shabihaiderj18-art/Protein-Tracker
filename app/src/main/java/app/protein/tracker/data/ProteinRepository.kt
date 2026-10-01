package app.protein.tracker.data

import app.protein.tracker.data.backup.BACKUP_FORMAT
import app.protein.tracker.data.backup.BackupCodec
import app.protein.tracker.data.backup.BackupFile
import app.protein.tracker.data.backup.CsvExport
import app.protein.tracker.data.backup.NotABackupException
import app.protein.tracker.data.backup.toBackup
import app.protein.tracker.data.backup.toEntry
import app.protein.tracker.data.backup.toFood
import app.protein.tracker.data.backup.toSettings
import app.protein.tracker.data.db.AppDatabase
import app.protein.tracker.data.db.Food
import app.protein.tracker.data.db.LogEntry
import app.protein.tracker.data.settings.SettingsRepository
import app.protein.tracker.domain.DayClock
import app.protein.tracker.domain.DayTotal
import app.protein.tracker.domain.MealSlot
import app.protein.tracker.domain.Nutrition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime

data class ImportSummary(val foods: Int, val entries: Int)

class ProteinRepository(
    database: AppDatabase,
    private val settingsRepository: SettingsRepository,
) {
    private val foodDao = database.foodDao()
    private val entryDao = database.entryDao()
    private val backupDao = database.backupDao()

    val foods: Flow<List<Food>> = foodDao.observeAll()
    val dayTotals: Flow<List<DayTotal>> = entryDao.observeDayTotals()

    fun observeDay(day: Long): Flow<List<LogEntry>> = entryDao.observeDay(day)

    suspend fun today(): Long = DayClock.today(settingsRepository.settings.first().dayStartMinutes)

    // ---- Foods ----

    suspend fun food(id: Long): Food? = foodDao.get(id)

    /** Inserts a new food (id 0) or updates an existing one. Returns the food's id. */
    suspend fun saveFood(food: Food): Long =
        if (food.id == 0L) {
            foodDao.insert(food)
        } else {
            foodDao.update(food)
            food.id
        }

    suspend fun deleteFood(food: Food) = foodDao.delete(food)

    suspend fun restoreFood(food: Food) {
        foodDao.upsert(food)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = foodDao.setFavorite(id, favorite)

    // ---- Entries ----

    suspend fun entry(id: Long): LogEntry? = entryDao.get(id)

    /** Logs a portion of a food. [day] null means today. Returns the new entry's id. */
    suspend fun logFood(pick: FoodPick, amount: Double, inUnits: Boolean, meal: MealSlot, day: Long?): Long {
        val now = System.currentTimeMillis()
        val useUnits = inUnits && pick.hasUnit
        val quantity = Nutrition.quantity(amount, useUnits, pick.unitSize)
        val nutrients = Nutrition.forQuantity(pick.proteinPer100, pick.kcalPer100, quantity)
        val id = entryDao.insert(
            LogEntry(
                day = day ?: today(),
                loggedAt = now,
                meal = meal,
                foodId = pick.foodId,
                name = pick.name,
                amount = amount,
                inUnits = useUnits,
                unitName = pick.unitName,
                unitSize = pick.unitSize,
                quantity = quantity,
                baseUnit = pick.baseUnit,
                proteinPer100 = pick.proteinPer100,
                kcalPer100 = pick.kcalPer100,
                protein = nutrients.protein,
                kcal = nutrients.kcal,
            )
        )
        pick.foodId?.let { foodDao.markUsed(it, now, amount, useUnits) }
        return id
    }

    /** Changes the amount or meal of an entry, using the values saved in the entry itself. */
    suspend fun updateFoodEntry(entry: LogEntry, amount: Double, inUnits: Boolean, meal: MealSlot) {
        val pick = FoodPick.from(entry)
        val useUnits = inUnits && pick.hasUnit
        val quantity = Nutrition.quantity(amount, useUnits, pick.unitSize)
        val nutrients = Nutrition.forQuantity(pick.proteinPer100, pick.kcalPer100, quantity)
        entryDao.update(
            entry.copy(
                amount = amount,
                inUnits = useUnits,
                quantity = quantity,
                meal = meal,
                protein = nutrients.protein,
                kcal = nutrients.kcal,
            )
        )
    }

    suspend fun logQuick(name: String, protein: Double, kcal: Double, meal: MealSlot, day: Long?): Long =
        entryDao.insert(
            LogEntry(
                day = day ?: today(),
                loggedAt = System.currentTimeMillis(),
                meal = meal,
                name = name,
                protein = protein,
                kcal = kcal,
            )
        )

    suspend fun updateQuick(entry: LogEntry, name: String, protein: Double, kcal: Double, meal: MealSlot) {
        entryDao.update(entry.copy(name = name, protein = protein, kcal = kcal, meal = meal))
    }

    /** Deletes an entry and returns it, so it can be put back with [restoreEntry]. */
    suspend fun deleteEntry(id: Long): LogEntry? {
        val entry = entryDao.get(id) ?: return null
        entryDao.deleteById(id)
        return entry
    }

    /**
     * Puts a deleted entry back. It gets a new id on purpose: the list remembers each row's
     * "swiped away" state by id, and reusing the old id would make the row dismiss itself again.
     */
    suspend fun restoreEntry(entry: LogEntry) {
        entryDao.insert(entry.copy(id = 0))
    }

    /** Copies a past entry into today, in the meal slot that matches the current time. */
    suspend fun logAgainToday(entry: LogEntry): Long {
        val now = System.currentTimeMillis()
        val id = entryDao.insert(
            entry.copy(id = 0, day = today(), loggedAt = now, meal = MealSlot.at(LocalTime.now()))
        )
        entry.foodId?.let { foodDao.markUsed(it, now, entry.amount, entry.inUnits) }
        return id
    }

    // ---- Backup ----

    suspend fun exportJson(): String {
        val file = BackupFile(
            format = BACKUP_FORMAT,
            exportedAt = Instant.now().toString(),
            settings = settingsRepository.settings.first().toBackup(),
            foods = foodDao.getAll().map { it.toBackup() },
            entries = entryDao.getAll().map { it.toBackup() },
        )
        return BackupCodec.encode(file)
    }

    /** Replaces all foods, entries and settings with the backup. Throws [NotABackupException]. */
    suspend fun importJson(text: String): ImportSummary {
        val file = BackupCodec.decode(text)
        val foods: List<Food>
        val entries: List<LogEntry>
        try {
            foods = file.foods.map { it.toFood() }
            entries = file.entries.map { it.toEntry() }
        } catch (e: Exception) {
            throw NotABackupException("This backup file is damaged.")
        }
        backupDao.replaceAll(foods, entries)
        settingsRepository.replace(file.settings.toSettings())
        return ImportSummary(foods = foods.size, entries = entries.size)
    }

    suspend fun exportCsv(): String = CsvExport.build(entryDao.getAll().map { it.toBackup() })
}

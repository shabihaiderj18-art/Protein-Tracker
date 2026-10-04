package app.protein.tracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.protein.tracker.domain.BaseUnit
import app.protein.tracker.domain.MealSlot

/**
 * One thing you ate. The food's values are copied into the entry, so editing or deleting a
 * food later never changes your history.
 */
@Entity(tableName = "entries", indices = [Index(value = ["day"])])
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The logical day (LocalDate epoch day), after applying the day-start setting. */
    val day: Long,
    val loggedAt: Long,
    val meal: MealSlot,
    /** Null for quick entries. */
    val foodId: Long? = null,
    val name: String,
    /** The number typed in, e.g. 1.75 (slices) or 150 (grams). */
    val amount: Double = 0.0,
    val inUnits: Boolean = false,
    val unitName: String? = null,
    val unitSize: Double? = null,
    /** Grams or ml actually eaten. */
    val quantity: Double = 0.0,
    val baseUnit: BaseUnit = BaseUnit.GRAM,
    /** Null for quick entries, where protein and kcal were typed in directly. */
    val proteinPer100: Double? = null,
    val kcalPer100: Double? = null,
    val protein: Double,
    val kcal: Double,
    @ColumnInfo(defaultValue = "0") val carbs: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val fat: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val fiber: Double = 0.0,
    val carbsPer100: Double? = null,
    val fatPer100: Double? = null,
    val fiberPer100: Double? = null,
)

val LogEntry.isQuick: Boolean
    get() = proteinPer100 == null || kcalPer100 == null

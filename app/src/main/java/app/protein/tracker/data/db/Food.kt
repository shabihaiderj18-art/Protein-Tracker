package app.protein.tracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.protein.tracker.domain.BaseUnit

/** A food in your list. Nutrition values are per 100 g (or per 100 ml). */
@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val baseUnit: BaseUnit = BaseUnit.GRAM,
    /** Optional serving unit, e.g. "egg", "slice", "scoop". */
    val unitName: String? = null,
    /** Grams (or ml) in one serving unit. */
    val unitSize: Double? = null,
    val note: String? = null,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
    /** The amount used last time, so logging the same portion again is one tap. */
    val lastAmount: Double? = null,
    val lastInUnits: Boolean = false,
)

val Food.hasUnit: Boolean
    get() = !unitName.isNullOrBlank() && (unitSize ?: 0.0) > 0.0

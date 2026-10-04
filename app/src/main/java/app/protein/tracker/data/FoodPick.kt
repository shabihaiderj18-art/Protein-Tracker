package app.protein.tracker.data

import app.protein.tracker.data.db.Food
import app.protein.tracker.data.db.LogEntry
import app.protein.tracker.data.db.hasUnit
import app.protein.tracker.domain.BaseUnit

/**
 * Everything needed to log a portion: the nutrition basis plus the amount to start with.
 * Built from a food in the list, or from an existing entry when editing it.
 */
data class FoodPick(
    val foodId: Long?,
    val name: String,
    val proteinPer100: Double,
    val kcalPer100: Double,
    val baseUnit: BaseUnit,
    val unitName: String?,
    val unitSize: Double?,
    val initialAmount: Double,
    val initialInUnits: Boolean,
    val carbsPer100: Double? = null,
    val fatPer100: Double? = null,
    val fiberPer100: Double? = null,
) {
    val hasUnit: Boolean
        get() = !unitName.isNullOrBlank() && (unitSize ?: 0.0) > 0.0

    companion object {
        fun from(food: Food): FoodPick {
            val last = food.lastAmount?.takeIf { it > 0.0 }
            val (amount, inUnits) = when {
                last != null && (!food.lastInUnits || food.hasUnit) -> last to food.lastInUnits
                food.hasUnit -> 1.0 to true
                else -> 100.0 to false
            }
            return FoodPick(
                foodId = food.id,
                name = food.name,
                proteinPer100 = food.proteinPer100,
                kcalPer100 = food.kcalPer100,
                baseUnit = food.baseUnit,
                unitName = food.unitName,
                unitSize = food.unitSize,
                initialAmount = amount,
                initialInUnits = inUnits,
                carbsPer100 = food.carbsPer100,
                fatPer100 = food.fatPer100,
                fiberPer100 = food.fiberPer100,
            )
        }

        fun from(entry: LogEntry): FoodPick = FoodPick(
            foodId = entry.foodId,
            name = entry.name,
            proteinPer100 = entry.proteinPer100 ?: 0.0,
            kcalPer100 = entry.kcalPer100 ?: 0.0,
            baseUnit = entry.baseUnit,
            unitName = entry.unitName,
            unitSize = entry.unitSize,
            initialAmount = entry.amount,
            initialInUnits = entry.inUnits,
            carbsPer100 = entry.carbsPer100,
            fatPer100 = entry.fatPer100,
            fiberPer100 = entry.fiberPer100,
        )
    }
}

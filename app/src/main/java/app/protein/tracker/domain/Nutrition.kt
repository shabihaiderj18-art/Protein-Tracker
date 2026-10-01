package app.protein.tracker.domain

import kotlin.math.round

data class Nutrients(val protein: Double, val kcal: Double)

object Nutrition {
    /** Protein and kcal for [quantity] grams (or ml) of a food with the given per-100 values. */
    fun forQuantity(proteinPer100: Double, kcalPer100: Double, quantity: Double): Nutrients =
        Nutrients(
            protein = proteinPer100 * quantity / 100.0,
            kcal = kcalPer100 * quantity / 100.0,
        )

    /** Grams (or ml) for an amount typed either in base units or in servings (eggs, slices…). */
    fun quantity(amount: Double, inUnits: Boolean, unitSize: Double?): Double =
        if (inUnits && unitSize != null) amount * unitSize else amount

    /** Reads a number typed by the user. Accepts "1.75" and "1,75". Returns null if invalid. */
    fun parseAmount(text: String): Double? {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        return value.takeIf { it.isFinite() && it >= 0.0 }
    }

    /** Converts an amount when switching between grams and servings, keeping the same quantity. */
    fun convertAmount(amount: Double, toUnits: Boolean, unitSize: Double): Double {
        val converted = if (toUnits) amount / unitSize else amount * unitSize
        return round(converted * 100.0) / 100.0
    }

    /** The commonly suggested protein range for active adults: 1.6–2.0 g per kg of body weight. */
    fun suggestedProteinRange(bodyWeightKg: Double): ClosedFloatingPointRange<Double> =
        (bodyWeightKg * 1.6)..(bodyWeightKg * 2.0)
}

package app.protein.tracker.domain

object Units {
    private val invariant = setOf("medium", "small", "large", "tbsp", "tsp", "pav", "g", "ml", "kg")

    /** "1 egg", "2 eggs", "1.75 slices", "2 medium". */
    fun label(unit: String, amount: Double): String {
        val trimmed = unit.trim()
        if (amount == 1.0 || trimmed.isEmpty()) return trimmed
        val lower = trimmed.lowercase()
        return when {
            lower in invariant || lower.endsWith("s") || trimmed.contains(' ') -> trimmed
            lower.endsWith("y") && lower.length > 1 && lower[lower.length - 2] !in "aeiou" ->
                trimmed.dropLast(1) + "ies"
            lower.endsWith("ch") || lower.endsWith("sh") || lower.endsWith("x") -> trimmed + "es"
            else -> trimmed + "s"
        }
    }

    /** "Eggs", "Slices" — used on the grams/servings switch. */
    fun pluralTitle(unit: String): String =
        label(unit, 2.0).replaceFirstChar { it.uppercase() }
}

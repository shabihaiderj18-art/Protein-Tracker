package app.protein.tracker.data.backup

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Builds a CSV file with one row per logged entry. Opens in Excel or Google Sheets. */
object CsvExport {
    private val header = listOf(
        "date", "time", "meal", "food", "amount", "unit", "quantity", "base_unit", "protein_g", "kcal",
        "carbs_g", "fat_g", "fiber_g",
    )

    fun build(entries: List<BackupEntry>, zone: ZoneId = ZoneId.systemDefault()): String {
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
        val lines = mutableListOf(row(header))
        entries
            .sortedWith(compareBy<BackupEntry> { it.date }.thenBy { it.loggedAt })
            .forEach { e ->
                val time = Instant.ofEpochMilli(e.loggedAt).atZone(zone).toLocalTime().format(timeFormat)
                val isQuick = e.proteinPer100 == null
                val unit = when {
                    isQuick -> ""
                    e.inUnits && !e.unitName.isNullOrBlank() -> e.unitName
                    else -> baseSymbol(e.baseUnit)
                }
                lines += row(
                    listOf(
                        e.date,
                        time,
                        e.meal.lowercase().replaceFirstChar { it.uppercase() },
                        e.name,
                        if (isQuick) "" else number(e.amount),
                        unit,
                        if (isQuick) "" else number(e.quantity),
                        if (isQuick) "" else baseSymbol(e.baseUnit),
                        number(e.protein),
                        number(e.kcal),
                        number(e.carbs),
                        number(e.fat),
                        number(e.fiber),
                    )
                )
            }
        return lines.joinToString("\r\n", postfix = "\r\n")
    }

    fun escape(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }

    private fun row(cells: List<String>): String = cells.joinToString(",") { escape(it) }

    private fun baseSymbol(baseUnit: String): String = if (baseUnit == "ML") "ml" else "g"

    private fun number(value: Double): String =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

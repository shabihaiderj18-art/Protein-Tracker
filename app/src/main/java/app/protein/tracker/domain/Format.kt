package app.protein.tracker.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

object Fmt {
    private fun format(pattern: String, value: Double): String {
        val df = DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.getDefault()))
        df.roundingMode = RoundingMode.HALF_UP
        val text = df.format(value)
        return if (text == "-0") "0" else text
    }

    /** "37.5", "62", "112" — one decimal below 100 g, whole numbers above. */
    fun protein(grams: Double): String =
        if (abs(grams) >= 100) format("#,##0", grams) else format("#,##0.#", grams)

    /** "1,420" */
    fun kcal(kcal: Double): String = format("#,##0", kcal)

    /** "1.75", "150", "0.5" */
    fun amount(value: Double): String = format("#,##0.##", value)

    /** Plain number for text fields: no grouping, dot as decimal mark, up to two decimals. */
    fun plain(value: Double): String =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    /** "2 eggs · 100 g", "150 g", "250 ml" */
    fun amountLabel(
        amount: Double,
        inUnits: Boolean,
        unitName: String?,
        quantity: Double,
        baseUnit: BaseUnit,
    ): String = if (inUnits && !unitName.isNullOrBlank()) {
        "${amount(amount)} ${Units.label(unitName, amount)} · ${amount(quantity)} ${baseUnit.symbol}"
    } else {
        "${amount(quantity)} ${baseUnit.symbol}"
    }

    /** "1 egg = 50 g" */
    fun unitDefinition(unitName: String, unitSize: Double, baseUnit: BaseUnit): String =
        "1 ${unitName.trim()} = ${amount(unitSize)} ${baseUnit.symbol}"

    fun dayTitle(day: Long, today: Long): String = when (day) {
        today -> "Today"
        today - 1 -> "Yesterday"
        else -> fullDate(day)
    }

    /** "Wednesday, 1 October" */
    fun fullDate(day: Long): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    /** "Wed, 1 Oct" */
    fun shortDate(day: Long): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))

    /** "25 Sep – 1 Oct" */
    fun dayRange(first: Long, last: Long): String {
        val f = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        return "${LocalDate.ofEpochDay(first).format(f)} – ${LocalDate.ofEpochDay(last).format(f)}"
    }

    /** "September 2026" */
    fun month(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

    /** "4:00 AM" or "04:00", following the phone's locale. */
    fun timeOfDay(minutes: Int): String =
        LocalTime.of((minutes / 60) % 24, minutes % 60)
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
}

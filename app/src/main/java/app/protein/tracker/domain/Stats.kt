package app.protein.tracker.domain

import java.time.LocalDate
import java.time.YearMonth

object Stats {
    /**
     * The seven days ending on [endDay]. Averages only count days with at least one entry,
     * so a day you forgot to log does not drag the average down.
     */
    fun week(byDay: Map<Long, DayTotal>, endDay: Long): WeekSummary {
        val days = (endDay - 6..endDay).toList()
        val totals = days.map { byDay[it] }
        val logged = totals.filterNotNull()
        return WeekSummary(
            days = days,
            totals = totals,
            avgProtein = logged.takeIf { it.isNotEmpty() }?.map { it.protein }?.average(),
            avgKcal = logged.takeIf { it.isNotEmpty() }?.map { it.kcal }?.average(),
            loggedDays = logged.size,
        )
    }

    /** Average protein and kcal per logged day for each month, newest month first. */
    fun monthly(totals: List<DayTotal>): List<MonthAverage> =
        totals
            .groupBy { YearMonth.from(LocalDate.ofEpochDay(it.day)) }
            .map { (month, days) ->
                MonthAverage(
                    month = month,
                    avgProtein = days.map { it.protein }.average(),
                    avgKcal = days.map { it.kcal }.average(),
                    loggedDays = days.size,
                )
            }
            .sortedByDescending { it.month }

    fun inMonth(totals: List<DayTotal>, month: YearMonth): Map<Long, DayTotal> {
        val first = month.atDay(1).toEpochDay()
        val last = month.atEndOfMonth().toEpochDay()
        return totals.filter { it.day in first..last }.associateBy { it.day }
    }
}

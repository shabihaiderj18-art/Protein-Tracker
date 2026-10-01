package app.protein.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StatsTest {
    private fun day(text: String) = LocalDate.parse(text).toEpochDay()

    @Test
    fun weekAverageSkipsEmptyDays() {
        val totals = listOf(
            DayTotal(day("2026-09-28"), 100.0, 2000.0, 3),
            DayTotal(day("2026-09-30"), 120.0, 2200.0, 4),
            DayTotal(day("2026-10-01"), 80.0, 1800.0, 2),
        )
        val week = Stats.week(totals.associateBy { it.day }, day("2026-10-01"))
        assertEquals(7, week.days.size)
        assertEquals(day("2026-09-25"), week.days.first())
        assertEquals(3, week.loggedDays)
        assertEquals(100.0, week.avgProtein!!, 1e-9)
        assertEquals(2000.0, week.avgKcal!!, 1e-9)
    }

    @Test
    fun emptyWeekHasNoAverage() {
        val week = Stats.week(emptyMap(), day("2026-10-01"))
        assertNull(week.avgProtein)
        assertEquals(0, week.loggedDays)
    }

    @Test
    fun monthlyAveragesNewestFirst() {
        val totals = listOf(
            DayTotal(day("2026-08-31"), 90.0, 1900.0, 1),
            DayTotal(day("2026-09-01"), 100.0, 2000.0, 1),
            DayTotal(day("2026-09-02"), 110.0, 2100.0, 1),
        )
        val months = Stats.monthly(totals)
        assertEquals(YearMonth.of(2026, 9), months[0].month)
        assertEquals(105.0, months[0].avgProtein, 1e-9)
        assertEquals(2, months[0].loggedDays)
        assertEquals(YearMonth.of(2026, 8), months[1].month)
    }

    @Test
    fun inMonthFilters() {
        val totals = listOf(
            DayTotal(day("2026-08-31"), 90.0, 1900.0, 1),
            DayTotal(day("2026-09-01"), 100.0, 2000.0, 1),
        )
        val sept = Stats.inMonth(totals, YearMonth.of(2026, 9))
        assertEquals(setOf(day("2026-09-01")), sept.keys)
    }
}

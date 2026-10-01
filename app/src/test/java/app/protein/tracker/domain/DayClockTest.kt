package app.protein.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DayClockTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun millis(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun lateSnackCountsTowardPreviousDay() {
        val date = DayClock.logicalDate(millis("2026-10-02T01:30"), dayStartMinutes = 240, zone = zone)
        assertEquals(LocalDate.parse("2026-10-01"), date)
    }

    @Test
    fun afterDayStartIsNewDay() {
        val date = DayClock.logicalDate(millis("2026-10-02T04:00"), dayStartMinutes = 240, zone = zone)
        assertEquals(LocalDate.parse("2026-10-02"), date)
    }

    @Test
    fun midnightBoundary() {
        assertEquals(
            LocalDate.parse("2026-10-02"),
            DayClock.logicalDate(millis("2026-10-02T00:05"), dayStartMinutes = 0, zone = zone),
        )
        assertEquals(
            LocalDate.parse("2026-10-01"),
            DayClock.logicalDate(millis("2026-10-01T23:59"), dayStartMinutes = 0, zone = zone),
        )
    }
}

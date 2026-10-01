package app.protein.tracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Turns a moment in time into the "logical" day it belongs to.
 *
 * With the day starting at 4:00 AM, a snack eaten at 1:30 AM on the 2nd belongs to the 1st.
 */
object DayClock {
    fun logicalDate(
        epochMillis: Long,
        dayStartMinutes: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalDate = Instant.ofEpochMilli(epochMillis)
        .atZone(zone)
        .toLocalDateTime()
        .minusMinutes(dayStartMinutes.toLong())
        .toLocalDate()

    fun logicalDay(
        epochMillis: Long,
        dayStartMinutes: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = logicalDate(epochMillis, dayStartMinutes, zone).toEpochDay()

    fun today(dayStartMinutes: Int): Long =
        logicalDay(System.currentTimeMillis(), dayStartMinutes)
}

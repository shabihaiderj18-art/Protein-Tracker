package app.protein.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class MealSlotTest {
    @Test
    fun slotsFollowTheClock() {
        assertEquals(MealSlot.NIGHT, MealSlot.at(LocalTime.of(3, 59)))
        assertEquals(MealSlot.MORNING, MealSlot.at(LocalTime.of(4, 0)))
        assertEquals(MealSlot.MORNING, MealSlot.at(LocalTime.of(11, 59)))
        assertEquals(MealSlot.AFTERNOON, MealSlot.at(LocalTime.of(12, 0)))
        assertEquals(MealSlot.AFTERNOON, MealSlot.at(LocalTime.of(16, 59)))
        assertEquals(MealSlot.EVENING, MealSlot.at(LocalTime.of(17, 0)))
        assertEquals(MealSlot.EVENING, MealSlot.at(LocalTime.of(20, 59)))
        assertEquals(MealSlot.NIGHT, MealSlot.at(LocalTime.of(21, 0)))
    }
}

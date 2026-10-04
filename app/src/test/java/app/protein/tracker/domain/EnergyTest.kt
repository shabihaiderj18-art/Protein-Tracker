package app.protein.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyTest {
    private val profile = UserSettings(
        sex = Sex.MALE, ageYears = 27, heightCm = 163.0, bodyWeightKg = 65.0, jobType = JobType.FIELD,
    )

    @Test
    fun bmrMatchesFormula() {
        assertEquals(1538.75, Energy.bmr(Sex.MALE, 65.0, 163.0, 27), 1e-9)
    }

    @Test
    fun workAndGymRaiseTheTarget() {
        val rest = Energy.dayTarget(profile, ActivityInput(false, 0.0, GymLevel.NONE, 0))!!
        val work = Energy.dayTarget(profile, ActivityInput(true, 7.0, GymLevel.NONE, 0))!!
        val gym = Energy.dayTarget(profile, ActivityInput(true, 7.0, GymLevel.MODERATE, 60))!!
        assertEquals(1850.0, rest, 0.0)
        assertTrue(work > rest + 800)
        assertTrue(gym > work + 200)
    }

    @Test
    fun incompleteProfileGivesNull() {
        assertNull(Energy.dayTarget(UserSettings(), ActivityInput(true, 7.0, GymLevel.NONE, 0)))
    }

    @Test
    fun feetAndInches() {
        assertEquals(162.56, Energy.feetInchesToCm(5, 4.0), 1e-9)
    }
}

package app.protein.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedFoodsTest {
    @Test
    fun namesAreUnique() {
        val names = SeedFoods.everything.map { it.name.lowercase() }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun valuesAreSensible() {
        SeedFoods.everything.forEach { seed ->
            assertTrue(seed.name, seed.kcalPer100 in 0.0..900.0)
            assertTrue(seed.name, seed.proteinPer100 in 0.0..100.0)
            assertTrue(seed.name, seed.proteinPer100 * 4 <= seed.kcalPer100 + 1)
            assertTrue(seed.name, (seed.unitName == null) == (seed.unitSize == null))
            assertTrue(seed.name, seed.carbsPer100 != null && seed.fatPer100 != null && seed.fiberPer100 != null)
            val kcalFromMacros = 4 * seed.proteinPer100 + 4 * seed.carbsPer100!! + 9 * seed.fatPer100!!
            assertTrue(seed.name, kcalFromMacros <= seed.kcalPer100 * 1.05 + 2)
        }
    }
}

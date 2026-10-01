package app.protein.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionTest {
    @Test
    fun chickenByGrams() {
        val n = Nutrition.forQuantity(proteinPer100 = 25.0, kcalPer100 = 165.0, quantity = 150.0)
        assertEquals(37.5, n.protein, 1e-9)
        assertEquals(247.5, n.kcal, 1e-9)
    }

    @Test
    fun breadBySlicesWithDecimals() {
        val quantity = Nutrition.quantity(amount = 1.75, inUnits = true, unitSize = 28.0)
        assertEquals(49.0, quantity, 1e-9)
        val n = Nutrition.forQuantity(9.0, 265.0, quantity)
        assertEquals(4.41, n.protein, 1e-9)
    }

    @Test
    fun parsesCommaAndDot() {
        assertEquals(1.75, Nutrition.parseAmount("1.75")!!, 1e-9)
        assertEquals(1.75, Nutrition.parseAmount(" 1,75 ")!!, 1e-9)
        assertNull(Nutrition.parseAmount(""))
        assertNull(Nutrition.parseAmount("abc"))
        assertNull(Nutrition.parseAmount("-3"))
    }

    @Test
    fun convertsBetweenGramsAndUnits() {
        assertEquals(100.0, Nutrition.convertAmount(2.0, toUnits = false, unitSize = 50.0), 1e-9)
        assertEquals(1.75, Nutrition.convertAmount(49.0, toUnits = true, unitSize = 28.0), 1e-9)
    }

    @Test
    fun suggestedRange() {
        val r = Nutrition.suggestedProteinRange(70.0)
        assertEquals(112.0, r.start, 1e-9)
        assertEquals(140.0, r.endInclusive, 1e-9)
    }
}

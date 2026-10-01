package app.protein.tracker.domain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FormatTest {
    private lateinit var saved: Locale

    @Before
    fun setUp() {
        saved = Locale.getDefault()
        Locale.setDefault(Locale.UK)
    }

    @After
    fun tearDown() = Locale.setDefault(saved)

    @Test
    fun proteinNumbers() {
        assertEquals("37.5", Fmt.protein(37.5))
        assertEquals("62", Fmt.protein(62.0))
        assertEquals("6.3", Fmt.protein(6.3))
        assertEquals("112", Fmt.protein(112.4))
        assertEquals("0", Fmt.protein(0.0))
    }

    @Test
    fun kcalAndAmounts() {
        assertEquals("1,420", Fmt.kcal(1419.6))
        assertEquals("1.75", Fmt.amount(1.75))
        assertEquals("150", Fmt.amount(150.0))
        assertEquals("1.75", Fmt.plain(1.75))
        assertEquals("100", Fmt.plain(100.0))
        assertEquals("0", Fmt.plain(0.0))
    }

    @Test
    fun amountLabels() {
        assertEquals("2 eggs · 100 g", Fmt.amountLabel(2.0, true, "egg", 100.0, BaseUnit.GRAM))
        assertEquals("1 egg · 50 g", Fmt.amountLabel(1.0, true, "egg", 50.0, BaseUnit.GRAM))
        assertEquals("1.75 slices · 49 g", Fmt.amountLabel(1.75, true, "slice", 49.0, BaseUnit.GRAM))
        assertEquals("250 ml", Fmt.amountLabel(250.0, false, null, 250.0, BaseUnit.ML))
    }

    @Test
    fun pluralUnits() {
        assertEquals("rotis", Units.label("roti", 2.0))
        assertEquals("medium", Units.label("medium", 2.0))
        assertEquals("pav", Units.label("pav", 3.0))
        assertEquals("tbsp", Units.label("tbsp", 2.0))
        assertEquals("pieces", Units.label("piece", 2.0))
        assertEquals("scoop", Units.label("scoop", 1.0))
        assertEquals("patties", Units.label("patty", 2.0))
        assertEquals("Slices", Units.pluralTitle("slice"))
    }
}

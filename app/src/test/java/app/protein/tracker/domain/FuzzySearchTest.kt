package app.protein.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchTest {
    private val names = SeedFoods.all.map { it.name }

    private fun search(query: String) = FuzzySearch.rank(query, names, { it })

    @Test
    fun prefixWins() {
        assertEquals("Chicken, cooked", search("chi").first())
        assertEquals("Paneer", search("pan").first())
    }

    @Test
    fun wordInsideName() {
        assertEquals("Milk, toned", search("toned").first())
        assertTrue(search("protein").containsAll(listOf("Pea protein", "Dahi, high-protein")))
    }

    @Test
    fun lettersInOrder() {
        assertEquals("Chicken, cooked", search("chkn").first())
    }

    @Test
    fun smallTypos() {
        assertEquals("Paneer", search("panir").first())
        assertEquals("Chicken, cooked", search("chiken").first())
        assertEquals("Egg", search("eggs").first())
        assertEquals("Capsicum", search("capsicam").first())
    }

    @Test
    fun severalWords() {
        assertEquals("Milk, toned", search("milk ton").first())
        assertEquals("Dahi, high-protein", search("dahi high").first())
    }

    @Test
    fun nonsenseFindsNothing() {
        assertTrue(search("xyzzy").isEmpty())
        assertTrue(search("").isEmpty())
    }

    @Test
    fun boostBreaksTies() {
        val ranked = FuzzySearch.rank("milk", names, { it }) { if (it == "Milk, toned") 10.0 else 0.0 }
        assertEquals("Milk, toned", ranked.first())
    }

    @Test
    fun usageBoostRewardsRecentFavourites() {
        val now = 1_000_000_000_000L
        val fresh = FuzzySearch.usageBoost(isFavorite = true, lastUsedAt = now, useCount = 5, now = now)
        val stale = FuzzySearch.usageBoost(isFavorite = false, lastUsedAt = now - 30L * 86_400_000L, useCount = 0, now = now)
        assertTrue(fresh > stale)
        assertEquals(0.0, stale, 1e-9)
    }

    @Test
    fun distance() {
        assertEquals(0, FuzzySearch.distance("dal", "dal"))
        assertEquals(1, FuzzySearch.distance("dal", "dahl"))
        assertEquals(1, FuzzySearch.distance("ab", "ba"))
    }
}

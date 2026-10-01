package app.protein.tracker.domain

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Small, forgiving search for food names.
 *
 * Every word typed must match the name somehow. Better kinds of match score higher:
 * start of the name > start of a word > anywhere in the name > letters in order ("chkn")
 * > a small typo ("panir" finds "Paneer").
 */
object FuzzySearch {

    fun normalize(text: String): String {
        val mapped = buildString {
            for (c in text.lowercase(Locale.ROOT)) append(if (c.isLetterOrDigit()) c else ' ')
        }
        return mapped.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
    }

    /** 0 means no match. Higher is better; a perfect match scores about 110. */
    fun score(query: String, target: String): Double {
        val q = normalize(query)
        if (q.isEmpty()) return 0.0
        val name = normalize(target)
        if (name.isEmpty()) return 0.0
        val words = name.split(' ')
        val tokens = q.split(' ')

        var total = 0.0
        for ((index, token) in tokens.withIndex()) {
            val s = tokenScore(token, name, words, isFirstToken = index == 0)
            if (s <= 0.0) return 0.0
            total += s
        }
        var result = total / tokens.size
        if (name == q) result += 10.0 else if (name.startsWith(q)) result += 5.0
        return result
    }

    /**
     * Ranks [items] by how well their name matches [query]. [boost] adds a few points,
     * e.g. for favourites and recently used foods, so they win close calls.
     */
    fun <T> rank(
        query: String,
        items: List<T>,
        name: (T) -> String,
        boost: (T) -> Double = { 0.0 },
    ): List<T> = items
        .mapNotNull { item ->
            val s = score(query, name(item))
            if (s > 0.0) item to s + boost(item) else null
        }
        .sortedWith(
            compareByDescending<Pair<T, Double>> { it.second }
                .thenBy { name(it.first).lowercase(Locale.ROOT) }
        )
        .map { it.first }

    /** Extra points for favourites, recent use and frequent use. At most about 17. */
    fun usageBoost(isFavorite: Boolean, lastUsedAt: Long?, useCount: Int, now: Long): Double {
        var boost = if (isFavorite) 8.0 else 0.0
        if (lastUsedAt != null) {
            val ageDays = (now - lastUsedAt) / 86_400_000.0
            if (ageDays in 0.0..14.0) boost += 6.0 * (1.0 - ageDays / 14.0)
        }
        boost += min(useCount, 30) / 10.0
        return boost
    }

    private fun tokenScore(token: String, name: String, words: List<String>, isFirstToken: Boolean): Double {
        if (isFirstToken && name.startsWith(token)) return 100.0
        if (words.any { it.startsWith(token) }) return 90.0
        if (name.contains(token)) return 70.0
        subsequenceScore(token, name)?.let { return it }
        typoScore(token, words)?.let { return it }
        return 0.0
    }

    /** Letters of [token] appear in order, starting at the beginning of a word. 35–60 points. */
    private fun subsequenceScore(token: String, name: String): Double? {
        if (token.length < 2) return null
        var best: Double? = null
        for (start in name.indices) {
            val isWordStart = start == 0 || name[start - 1] == ' '
            if (!isWordStart || name[start] != token[0]) continue
            var t = 1
            var i = start + 1
            while (i < name.length && t < token.length) {
                if (name[i] == token[t]) t++
                i++
            }
            if (t == token.length) {
                val span = i - start
                val s = max(35.0, 60.0 - (span - token.length) * 3.0)
                if (best == null || s > best) best = s
            }
        }
        return best
    }

    /** Close spelling of a word or of the start of a word. 21–35 points. */
    private fun typoScore(token: String, words: List<String>): Double? {
        val allowed = when (token.length) {
            in 0..2 -> 0
            in 3..4 -> 1
            else -> 2
        }
        if (allowed == 0) return null
        var bestDistance = Int.MAX_VALUE
        for (word in words) {
            val candidates = listOf(
                word,
                word.take(token.length),
                word.take(token.length + 1),
                word.take(max(1, token.length - 1)),
            )
            for (candidate in candidates) {
                bestDistance = min(bestDistance, distance(token, candidate))
            }
        }
        return if (bestDistance <= allowed) 35.0 - 7.0 * bestDistance else null
    }

    /** Optimal string alignment distance: edits, inserts, deletes and swaps of neighbours. */
    internal fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = min(v, d[i - 2][j - 2] + 1)
                }
                d[i][j] = v
            }
        }
        return d[a.length][b.length]
    }
}

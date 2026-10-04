package app.protein.tracker

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeLeft
import app.protein.tracker.data.FoodPick
import app.protein.tracker.data.backup.BackupCodec
import app.protein.tracker.domain.MealSlot
import app.protein.tracker.domain.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Runs the real app on the JVM (Robolectric) and walks through the main flows:
 * logging by search, servings, quick entry, swipe to delete with undo, long-press to log again,
 * history, foods, settings, backup round trip and dark mode. Screenshots go to app/build/screens.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xhdpi")
class AppSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container: AppContainer
        get() = (compose.activity.application as ProteinApp).container

    private fun waitForText(text: String, substring: Boolean = false) {
        try {
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
                        .fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            println(compose.onRoot().printToString())
            throw AssertionError("Text not found: $text", e)
        }
        compose.waitForIdle()
    }

    private fun waitForNoText(text: String) {
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        val bitmap = try {
            compose.onRoot().captureToImage().asAndroidBitmap()
        } catch (e: Throwable) {
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val dir = File("build/screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Clicks a text; falls back to the unmerged tree for buttons whose label isn't merged. */
    private fun clickText(text: String) {
        val merged = compose.onAllNodesWithText(text).fetchSemanticsNodes()
        if (merged.size == 1) {
            compose.onNodeWithText(text).performClick()
        } else {
            compose.onNodeWithText(text, useUnmergedTree = true).performClick()
        }
        compose.waitForIdle()
    }

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun mainFlows() {
        waitForText("Nothing logged yet today")
        shot("01-today-empty")

        // 150 g of chicken, found with a misspelling.
        clickText("Log food")
        waitForText("Search foods")
        waitForText("Whey protein")
        shot("02-log-list")
        compose.onNode(hasSetTextAction()).performTextInput("chiken cooked")
        waitForText("Chicken, cooked")
        shot("03-log-search")
        clickText("Chicken, cooked")
        waitForText("Amount")
        compose.onNode(hasSetTextAction()).performTextReplacement("150")
        waitForText("37.5 g")
        shot("04-log-amount")
        clickText("Save")
        waitForNoText("Nothing logged yet today")
        waitForText("150 g")

        // Two eggs, logged in servings.
        clickText("Log food")
        waitForText("Search foods")
        compose.onNode(hasSetTextAction()).performTextInput("egg")
        waitForText("Egg")
        clickText("Egg")
        waitForText("Amount")
        compose.onNode(hasSetTextAction()).performTextReplacement("2")
        waitForText("12.6 g")
        clickText("Save")
        waitForText("2 eggs · 100 g")

        // Quick entry.
        clickText("Log food")
        waitForText("Search foods")
        clickText("Quick")
        waitForText("Type protein and calories directly")
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("20")
        fields[1].performTextInput("180")
        fields[2].performTextInput("Protein bar")
        shot("05-quick-entry")
        clickText("Save")
        waitForText("Protein bar")
        waitForText("70.1 g")
        shot("06-today-entries")

        // Swipe left deletes, Undo brings it back.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Protein bar"))
        compose.waitForIdle()
        compose.onNodeWithText("Protein bar").performTouchInput { swipeLeft() }
        waitForText("Removed Protein bar")
        clickText("Undo")
        waitForText("Protein bar")

        // Long-press logs the same thing again today.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Protein bar"))
        compose.waitForIdle()
        compose.onNodeWithText("Protein bar").performTouchInput { longClick() }
        waitForText("Logged Protein bar again today")
        val todayEntries = {
            runBlocking {
                val repository = container.repository
                repository.observeDay(repository.today()).first()
            }
        }
        try {
            compose.waitUntil(10_000) { todayEntries().count { it.name == "Protein bar" } == 2 }
        } catch (e: Throwable) {
            throw AssertionError("Entries today: " + todayEntries().joinToString { "${it.id}:${it.name}:${it.day}:${it.meal}" }, e)
        }

        // Tap opens the editor.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Chicken, cooked"))
        compose.waitForIdle()
        clickText("Chicken, cooked")
        waitForText("Save changes")
        pressBack()
        waitForNoText("Save changes")

        // Some history, so the chart and averages have data.
        runBlocking {
            val repository = container.repository
            val foods = repository.foods.first()
            val chicken = foods.first { it.name == "Chicken, cooked" }
            val roti = foods.first { it.name == "Roti" }
            val dal = foods.first { it.name == "Dal, cooked" }
            val today = repository.today()
            for (daysAgo in 1..40) {
                if (daysAgo % 9 == 4) continue
                val day = today - daysAgo
                repository.logFood(FoodPick.from(chicken), 120.0 + (daysAgo % 5) * 35.0, false, MealSlot.AFTERNOON, day)
                repository.logFood(FoodPick.from(roti), 2.0 + daysAgo % 2, true, MealSlot.EVENING, day)
                if (daysAgo % 3 != 0) repository.logFood(FoodPick.from(dal), 200.0, false, MealSlot.NIGHT, day)
            }
        }

        clickText("History")
        waitForText("7-DAY AVERAGE")
        waitForText("protein a day")
        shot("07-history")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Darker days", substring = true))
        compose.waitForIdle()
        shot("08-history-calendar")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Averages count only", substring = true))
        compose.waitForIdle()
        shot("09-history-months")

        // Open today's date in the calendar.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Darker days", substring = true))
        val todayOfMonth = LocalDate.ofEpochDay(runBlocking { container.repository.today() }).dayOfMonth
        compose.onAllNodes(hasText("$todayOfMonth") and hasClickAction())[0].performClick()
        waitForText("Add to this day")
        shot("10-day-detail")
        pressBack()
        waitForText("7-DAY AVERAGE")

        clickText("Foods")
        waitForText("Search your foods")
        shot("11-foods")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Paneer"))
        clickText("Paneer")
        waitForText("Edit food")
        shot("12-food-editor")
        pressBack()
        waitForNoText("Edit food")

        clickText("Settings")
        waitForText("DAILY TARGETS")
        // Text fields inside dialogs never go idle under Robolectric, so set the weight directly.
        runBlocking { container.settings.update { it.copy(bodyWeightKg = 70.0) } }
        waitForText("Suggested", substring = true)
        shot("13-settings")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Export entries as CSV"))
        compose.waitForIdle()
        shot("14-settings-bottom")

        // Backup round trip keeps everything.
        runBlocking {
            val repository = container.repository
            val json = repository.exportJson()
            val before = BackupCodec.decode(json)
            val summary = repository.importJson(json)
            assertEquals(before.foods.size, summary.foods)
            assertEquals(before.entries.size, summary.entries)
            assertTrue(repository.exportCsv().lines().size > before.entries.size)
        }

        // Dark mode.
        runBlocking { container.settings.update { it.copy(themeMode = ThemeMode.DARK, dynamicColor = false) } }
        clickText("Today")
        waitForText("Protein bar")
        shot("15-today-dark")
        clickText("History")
        waitForText("7-DAY AVERAGE")
        shot("16-history-dark")
    }
}

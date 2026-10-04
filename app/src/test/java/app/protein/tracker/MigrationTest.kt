package app.protein.tracker

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import app.protein.tracker.data.db.AppDatabase
import app.protein.tracker.domain.SeedFoods
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Opens a database made by version 1 of the app with the current version, keeping all data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MigrationTest {
    @Test
    fun upgradesFromVersion1() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = context.getDatabasePath("protein.db").apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `foods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                    "`kcalPer100` REAL NOT NULL, `proteinPer100` REAL NOT NULL, `baseUnit` TEXT NOT NULL, `unitName` TEXT, " +
                    "`unitSize` REAL, `note` TEXT, `isFavorite` INTEGER NOT NULL, `lastUsedAt` INTEGER, `useCount` INTEGER NOT NULL, " +
                    "`lastAmount` REAL, `lastInUnits` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `day` INTEGER NOT NULL, " +
                    "`loggedAt` INTEGER NOT NULL, `meal` TEXT NOT NULL, `foodId` INTEGER, `name` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                    "`inUnits` INTEGER NOT NULL, `unitName` TEXT, `unitSize` REAL, `quantity` REAL NOT NULL, `baseUnit` TEXT NOT NULL, " +
                    "`proteinPer100` REAL, `kcalPer100` REAL, `protein` REAL NOT NULL, `kcal` REAL NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_day` ON `entries` (`day`)")
            db.execSQL(
                "INSERT INTO foods (name, kcalPer100, proteinPer100, baseUnit, isFavorite, useCount, lastInUnits) " +
                    "VALUES ('Paneer', 265, 18, 'GRAM', 1, 3, 0), ('My shake', 120, 20, 'ML', 0, 0, 0)"
            )
            db.execSQL(
                "INSERT INTO entries (day, loggedAt, meal, foodId, name, amount, inUnits, quantity, baseUnit, " +
                    "proteinPer100, kcalPer100, protein, kcal) VALUES (20000, 1, 'MORNING', 1, 'Paneer', 100, 0, 100, 'GRAM', 18, 265, 18, 265)"
            )
            db.version = 1
        }

        val database = AppDatabase.build(context)
        runBlocking {
            val foods = database.foodDao().getAll()
            val paneer = foods.single { it.name == "Paneer" }
            assertTrue(paneer.isFavorite)
            assertEquals(20.0, paneer.fatPer100!!, 0.0)
            assertTrue(foods.any { it.name == "My shake" && it.carbsPer100 == null })
            assertTrue(foods.any { it.name == "Butter chicken" })
            assertTrue(foods.any { it.name == "Seekh kebab" })
            assertEquals(2 + SeedFoods.indian.size + SeedFoods.nonVeg.size, foods.size)
            val entry = database.entryDao().getAll().single()
            assertEquals(18.0, entry.protein, 0.0)
            assertEquals(0.0, entry.carbs, 0.0)
            assertEquals(null, database.dayActivityDao().observe(20000).first())
        }
        database.close()
    }

    @Test
    fun freshInstallHasEveryFoodWithNutrients() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getDatabasePath("protein.db").delete()
        val database = AppDatabase.build(context)
        runBlocking {
            val foods = database.foodDao().getAll()
            assertEquals(SeedFoods.everything.size, foods.size)
            assertNotNull(foods.first().fiberPer100)
        }
        database.close()
    }
}

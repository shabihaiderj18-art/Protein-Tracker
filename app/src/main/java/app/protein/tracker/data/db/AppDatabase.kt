package app.protein.tracker.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.protein.tracker.domain.SeedFoods

@Database(
    entities = [Food::class, LogEntry::class, DayActivity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun entryDao(): EntryDao
    abstract fun backupDao(): BackupDao
    abstract fun dayActivityDao(): DayActivityDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "protein.db")
                .addCallback(SeedCallback)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}

/** Fills in the starter foods once, when the database file is first created. */
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        insertSeeds(db, SeedFoods.everything, withMacros = true)
    }
}

/** Version 2 adds the Indian foods to existing installs. Foods you already have are skipped. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        insertSeeds(db, SeedFoods.indian, withMacros = false)
    }
}

/**
 * Version 3 adds carbs, fat and fibre, the daily activity check-in, and more non-veg dishes.
 * Your foods and entries are kept; starter foods get their carbs, fat and fibre filled in.
 */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("carbsPer100", "fatPer100", "fiberPer100").forEach {
            db.execSQL("ALTER TABLE `foods` ADD COLUMN `$it` REAL")
            db.execSQL("ALTER TABLE `entries` ADD COLUMN `$it` REAL")
        }
        listOf("carbs", "fat", "fiber").forEach {
            db.execSQL("ALTER TABLE `entries` ADD COLUMN `$it` REAL NOT NULL DEFAULT 0")
        }
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_activity` (`day` INTEGER NOT NULL, `worked` INTEGER NOT NULL, " +
                "`workHours` REAL NOT NULL, `gym` TEXT NOT NULL, `gymMinutes` INTEGER NOT NULL, PRIMARY KEY(`day`))"
        )
        (SeedFoods.all + SeedFoods.indian).forEach { seed ->
            db.execSQL(
                "UPDATE `foods` SET `carbsPer100` = ?, `fatPer100` = ?, `fiberPer100` = ? " +
                    "WHERE `name` = ? COLLATE NOCASE AND `carbsPer100` IS NULL",
                arrayOf<Any?>(seed.carbsPer100, seed.fatPer100, seed.fiberPer100, seed.name),
            )
        }
        insertSeeds(db, SeedFoods.nonVeg, withMacros = true)
    }
}

private fun insertSeeds(db: SupportSQLiteDatabase, seeds: List<SeedFoods.Seed>, withMacros: Boolean) {
    seeds.forEach { seed ->
        val exists = db.query("SELECT 1 FROM foods WHERE name = ? COLLATE NOCASE", arrayOf<Any?>(seed.name))
            .use { it.moveToFirst() }
        if (exists) return@forEach
        val values = ContentValues().apply {
            put("name", seed.name)
            put("kcalPer100", seed.kcalPer100)
            put("proteinPer100", seed.proteinPer100)
            put("baseUnit", seed.baseUnit.name)
            if (seed.unitName != null) put("unitName", seed.unitName) else putNull("unitName")
            if (seed.unitSize != null) put("unitSize", seed.unitSize) else putNull("unitSize")
            if (seed.note != null) put("note", seed.note) else putNull("note")
            put("isFavorite", 0)
            putNull("lastUsedAt")
            put("useCount", 0)
            putNull("lastAmount")
            put("lastInUnits", 0)
            if (withMacros) {
                put("carbsPer100", seed.carbsPer100)
                put("fatPer100", seed.fatPer100)
                put("fiberPer100", seed.fiberPer100)
            }
        }
        db.insert("foods", SQLiteDatabase.CONFLICT_ABORT, values)
    }
}

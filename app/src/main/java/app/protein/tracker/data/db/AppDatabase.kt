package app.protein.tracker.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.protein.tracker.domain.SeedFoods

@Database(
    entities = [Food::class, LogEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun entryDao(): EntryDao
    abstract fun backupDao(): BackupDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "protein.db")
                .addCallback(SeedCallback)
                .build()
    }
}

/** Fills in the starter foods once, when the database file is first created. */
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        SeedFoods.all.forEach { seed ->
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
            }
            db.insert("foods", SQLiteDatabase.CONFLICT_ABORT, values)
        }
    }
}

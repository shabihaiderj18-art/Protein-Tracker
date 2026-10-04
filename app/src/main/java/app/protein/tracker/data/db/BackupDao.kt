package app.protein.tracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Replaces everything in one transaction, so a failed import never leaves half the data. */
@Dao
abstract class BackupDao {
    @Query("DELETE FROM entries")
    abstract suspend fun clearEntries()

    @Query("DELETE FROM foods")
    abstract suspend fun clearFoods()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertFoods(foods: List<Food>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertEntries(entries: List<LogEntry>)

    @Query("DELETE FROM day_activity")
    abstract suspend fun clearActivity()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertActivity(activity: List<DayActivity>)

    @Transaction
    open suspend fun replaceAll(foods: List<Food>, entries: List<LogEntry>, activity: List<DayActivity>) {
        clearEntries()
        clearFoods()
        clearActivity()
        insertFoods(foods)
        insertEntries(entries)
        insertActivity(activity)
    }
}

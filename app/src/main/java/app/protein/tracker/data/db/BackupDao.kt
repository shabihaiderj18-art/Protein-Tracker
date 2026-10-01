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

    @Transaction
    open suspend fun replaceAll(foods: List<Food>, entries: List<LogEntry>) {
        clearEntries()
        clearFoods()
        insertFoods(foods)
        insertEntries(entries)
    }
}

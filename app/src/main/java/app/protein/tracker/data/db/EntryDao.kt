package app.protein.tracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.protein.tracker.domain.DayTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE day = :day ORDER BY loggedAt")
    fun observeDay(day: Long): Flow<List<LogEntry>>

    @Query(
        "SELECT day, SUM(protein) AS protein, SUM(kcal) AS kcal, COUNT(*) AS entryCount " +
            "FROM entries GROUP BY day ORDER BY day"
    )
    fun observeDayTotals(): Flow<List<DayTotal>>

    @Query("SELECT * FROM entries ORDER BY day, loggedAt")
    suspend fun getAll(): List<LogEntry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): LogEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntry): Long

    @Update
    suspend fun update(entry: LogEntry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

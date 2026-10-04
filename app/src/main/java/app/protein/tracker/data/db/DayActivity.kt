package app.protein.tracker.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import app.protein.tracker.domain.ActivityInput
import app.protein.tracker.domain.GymLevel
import kotlinx.coroutines.flow.Flow

/** The daily check-in: did you work, for how long, and did you go to the gym. */
@Entity(tableName = "day_activity")
data class DayActivity(
    @PrimaryKey val day: Long,
    val worked: Boolean,
    val workHours: Double,
    val gym: GymLevel,
    val gymMinutes: Int,
) {
    fun toInput() = ActivityInput(worked, workHours, gym, gymMinutes)
}

@Dao
interface DayActivityDao {
    @Query("SELECT * FROM day_activity WHERE day = :day")
    fun observe(day: Long): Flow<DayActivity?>

    @Query("SELECT * FROM day_activity WHERE day = :day")
    suspend fun get(day: Long): DayActivity?

    @Query("SELECT * FROM day_activity")
    suspend fun getAll(): List<DayActivity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: DayActivity)

    @Query("DELETE FROM day_activity")
    suspend fun deleteAll()
}

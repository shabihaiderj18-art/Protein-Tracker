package app.protein.tracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Food>>

    @Query("SELECT * FROM foods")
    suspend fun getAll(): List<Food>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun get(id: Long): Food?

    @Insert
    suspend fun insert(food: Food): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(food: Food): Long

    @Update
    suspend fun update(food: Food)

    @Delete
    suspend fun delete(food: Food)

    @Query("UPDATE foods SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query(
        "UPDATE foods SET lastUsedAt = :time, useCount = useCount + 1, " +
            "lastAmount = :amount, lastInUnits = :inUnits WHERE id = :id"
    )
    suspend fun markUsed(id: Long, time: Long, amount: Double, inUnits: Boolean)
}

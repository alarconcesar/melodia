package com.melodia.app.data.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC") fun observeAll(): Flow<List<FavoriteEntity>>
    @Query("SELECT songId FROM favorites") suspend fun getAllIds(): List<Long>
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId=:id)") fun isFav(id: Long): Flow<Boolean>
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId=:id)") suspend fun existsNow(id: Long): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun add(e: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE songId=:id") suspend fun remove(id: Long)
}

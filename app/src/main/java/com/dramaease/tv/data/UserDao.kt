package com.dramaease.tv.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM favorites WHERE dramaId LIKE :prefix || '%' ORDER BY addedAt DESC")
    fun favorites(prefix: String): Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) FROM favorites WHERE dramaId = :dramaId")
    fun isFavorite(dramaId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE dramaId = :dramaId")
    suspend fun removeFavorite(dramaId: String)

    @Query("SELECT * FROM drama_history WHERE dramaId LIKE :prefix || '%' ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun history(prefix: String, limit: Int): Flow<List<DramaHistory>>

    @Query("SELECT * FROM drama_history WHERE dramaId = :dramaId")
    fun historyFor(dramaId: String): Flow<DramaHistory?>

    @Query("SELECT * FROM drama_history WHERE dramaId = :dramaId")
    suspend fun historyForOnce(dramaId: String): DramaHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: DramaHistory)

    @Query("SELECT * FROM watch_entries WHERE dramaId LIKE :prefix || '%'")
    fun allEntries(prefix: String): Flow<List<WatchEntry>>

    @Query("SELECT * FROM watch_entries WHERE dramaId = :dramaId")
    fun entriesFor(dramaId: String): Flow<List<WatchEntry>>

    @Query("SELECT * FROM watch_entries WHERE episodeId = :episodeId")
    suspend fun entry(episodeId: String): WatchEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entity: WatchEntry)
}

/**
 * Bump `version` and add a Migration whenever the schema changes. Never use fallbackToDestructiveMigration:
 * saved favorites and progress must survive app updates.
 */
@Database(
    entities = [WatchEntry::class, FavoriteEntity::class, DramaHistory::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

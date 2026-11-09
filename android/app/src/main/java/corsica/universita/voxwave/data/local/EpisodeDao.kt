package corsica.universita.voxwave.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes ORDER BY createdAt DESC")
    fun all(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): EpisodeEntity?

    @Query("SELECT COUNT(*) FROM episodes")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(episode: EpisodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Update
    suspend fun update(episode: EpisodeEntity)

    @Delete
    suspend fun delete(episode: EpisodeEntity)

    /**
     * TD A5 : L'outbox — les épisodes non encore synchronisés vers le serveur.
     */
    @Query("SELECT * FROM episodes WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun unsynced(): List<EpisodeEntity>

    @Query("UPDATE episodes SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

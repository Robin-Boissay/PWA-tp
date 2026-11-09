package corsica.universita.voxwave.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant une ligne dans la table `episodes`.
 * Survit à la fermeture de l'application et au mode avion (Offline-first).
 */
@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val summary: String,
    val transcript: String,
    val audioPath: String? = null,
    val duration: String = "00:02",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val synced: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isSeed: Boolean = false,
)

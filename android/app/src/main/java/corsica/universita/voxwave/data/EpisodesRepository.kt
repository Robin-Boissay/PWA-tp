package corsica.universita.voxwave.data

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import corsica.universita.voxwave.data.local.EpisodeDao
import corsica.universita.voxwave.data.local.EpisodeEntity
import corsica.universita.voxwave.data.remote.EpisodeDto
import corsica.universita.voxwave.data.remote.SyncApi
import corsica.universita.voxwave.work.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private const val TAG = "VoxWave"

/**
 * La seule source de vérité de l'application : l'UI ne parle qu'au repository,
 * jamais directement à Room ni au réseau.
 */
class EpisodesRepository(
    private val dao: EpisodeDao,
    private val api: SyncApi,
    private val workManager: WorkManager,
) {

    val episodes: Flow<List<Episode>> = dao.all().map { list -> list.map { it.toEpisode() } }

    /**
     * Écrit le mémo vocal sur le disque (Room), puis planifie la synchronisation Outbox.
     * Le mémo est immédiatement visible avec le statut « En attente de sync ».
     */
    suspend fun add(
        title: String,
        summary: String,
        transcript: String,
        audioPath: String? = null,
        duration: String = "00:02",
    ): Long {
        val entity = EpisodeEntity(
            title = title,
            summary = summary,
            transcript = transcript,
            audioPath = audioPath,
            duration = duration,
            createdAt = System.currentTimeMillis(),
            synced = false,
            isSeed = false,
        )
        val id = dao.upsert(entity)
        Log.d(TAG, "Épisode inséré en base locale (id: $id) → planification sync")
        enqueueSync()
        return id
    }

    suspend fun delete(id: Long) {
        dao.get(id)?.let { dao.delete(it) }
    }

    /**
     * TD A5 : Travail unique « sync » géré par WorkManager avec contrainte réseau.
     * Survit à la fermeture et au redémarrage de l'appareil.
     */
    fun enqueueSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(SyncWorker.UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
        Log.d(TAG, "Sync demandée (travail unique « ${SyncWorker.UNIQUE_NAME} », contrainte CONNECTED)")
    }

    /**
     * Appelé par SyncWorker en arrière-plan : envoie chaque mémo non synchronisé vers l'écho,
     * puis les marque 'synced' en base.
     */
    suspend fun syncPending(): Int {
        val pending = dao.unsynced()
        val sentIds = mutableListOf<Long>()
        for (entity in pending) {
            val echo = api.push(
                EpisodeDto(
                    id = entity.id,
                    title = entity.title,
                    summary = entity.summary,
                    transcript = entity.transcript,
                    createdAt = entity.createdAt,
                )
            )
            Log.d(TAG, "Écho de sync reçu pour l'épisode ${entity.id} : ${echo.url}")
            sentIds += entity.id
        }
        if (sentIds.isNotEmpty()) {
            dao.markSynced(sentIds)
        }
        return sentIds.size
    }
}

/** Conversions entre la ligne Room et le modèle UI */
internal fun EpisodeEntity.toEpisode() = Episode(
    id = id,
    title = title,
    summary = summary,
    transcript = transcript,
    audioPath = audioPath,
    duration = duration,
    createdAt = createdAt,
    synced = synced,
    isSeed = isSeed,
)

internal fun Episode.toEntity() = EpisodeEntity(
    id = id,
    title = title,
    summary = summary,
    transcript = transcript,
    audioPath = audioPath,
    duration = duration,
    createdAt = createdAt,
    synced = synced,
    isSeed = isSeed,
)

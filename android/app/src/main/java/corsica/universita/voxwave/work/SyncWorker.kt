package corsica.universita.voxwave.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import corsica.universita.voxwave.VoxWaveContainer
import corsica.universita.voxwave.notif.NotificationChannels
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "VoxWave"

/**
 * TD A5 : L'Outbox WorkManager — envoie les mémos non synchronisés vers le serveur.
 * Garanti par l'OS même si l'application est fermée ou tuée.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker démarré (tentative #$runAttemptCount)")
        return try {
            val count = VoxWaveContainer.repository.syncPending()
            Log.d(TAG, "SyncWorker terminé avec succès : $count mémo(s) synchronisé(s)")
            if (count > 0) {
                NotificationChannels.showSyncSuccessNotification(applicationContext, count)
                VoxWaveContainer.analytics.logSyncDone(count)
            }
            Result.success()
        } catch (e: IOException) {
            Log.w(TAG, "SyncWorker erreur réseau temporaire : ${e.message} → retry")
            Result.retry()
        } catch (e: HttpException) {
            Log.e(TAG, "SyncWorker erreur HTTP définitive : ${e.code()} → failure")
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker erreur inattendue : ${e.message} → failure")
            Result.failure()
        }
    }

    companion object {
        const val UNIQUE_NAME = "sync"
    }
}

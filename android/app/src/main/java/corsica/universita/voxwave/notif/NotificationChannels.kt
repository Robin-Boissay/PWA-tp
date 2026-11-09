package corsica.universita.voxwave.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import corsica.universita.voxwave.MainActivity
import corsica.universita.voxwave.R

private const val TAG = "VoxWaveNotif"

object NotificationChannels {

    const val CHANNEL_SYNC_ID = "voxwave_sync"
    const val CHANNEL_EPISODES_ID = "voxwave_episodes"
    const val CHANNEL_AUDIO_ID = "voxwave_audio_service"

    private const val NOTIF_SYNC_ID = 1001
    private const val NOTIF_NEW_EPISODE_ID = 1002
    private const val NOTIF_TEST_ID = 1003

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService<NotificationManager>() ?: return

            // Canal 1 : Nouveaux épisodes et Alertes (IMPORTANCE_HIGH pour bannière visuelle et son)
            val episodesChannel = NotificationChannel(
                CHANNEL_EPISODES_ID,
                context.getString(R.string.channel_episodes_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_episodes_desc)
                enableVibration(true)
            }

            // Canal 2 : Synchronisation Outbox
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC_ID,
                context.getString(R.string.channel_sync_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_sync_desc)
            }

            // Canal 3 : Service Audio de premier plan
            val audioChannel = NotificationChannel(
                CHANNEL_AUDIO_ID,
                context.getString(R.string.channel_service_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_service_desc)
            }

            manager.createNotificationChannels(listOf(episodesChannel, syncChannel, audioChannel))
            Log.d(TAG, "Canaux de notification créés avec succès")
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Notification instantanée lors de la publication d'un nouveau mémo vocal.
     */
    fun showNewEpisodeNotification(context: Context, title: String, summary: String, episodeId: Long) {
        if (!areNotificationsEnabled(context)) {
            Log.w(TAG, "Notifications désactivées au niveau système pour VoxWave !")
            return
        }

        val manager = context.getSystemService<NotificationManager>() ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("episodeId", episodeId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            episodeId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_EPISODES_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎙️ Nouveau mémo : $title")
            .setContentText(summary.ifBlank { "Votre mémo vocal a été enregistré et publié." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIF_NEW_EPISODE_ID, notif)
        Log.d(TAG, "Notification nouveau mémo postée pour episode #$episodeId")
    }

    /**
     * Notification de test déclenchée manuellement depuis le bouton cloche de l'UI.
     */
    fun showTestNotification(context: Context) {
        if (!areNotificationsEnabled(context)) {
            Log.w(TAG, "Notifications désactivées au niveau système pour VoxWave !")
            return
        }

        val manager = context.getSystemService<NotificationManager>() ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_EPISODES_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔔 VoxWave — Test de Notification")
            .setContentText("Le système de notifications fonctionne parfaitement sur votre appareil !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIF_TEST_ID, notif)
        Log.d(TAG, "Notification de test envoyée avec succès")
    }

    /**
     * TD A6 : Notification locale déclenchée à la fin d'une synchronisation réussie par WorkManager.
     */
    fun showSyncSuccessNotification(context: Context, count: Int) {
        if (!areNotificationsEnabled(context)) return

        val manager = context.getSystemService<NotificationManager>() ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val message = context.resources.getQuantityString(R.plurals.pending_count, count, count)
        val notif = NotificationCompat.Builder(context, CHANNEL_SYNC_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.sync_notif_title))
            .setContentText("$message synchronisé(s) avec succès !")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIF_SYNC_ID, notif)
        Log.d(TAG, "Notification de synchronisation envoyée pour $count élément(s)")
    }
}

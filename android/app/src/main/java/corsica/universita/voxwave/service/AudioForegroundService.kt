package corsica.universita.voxwave.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import corsica.universita.voxwave.MainActivity
import corsica.universita.voxwave.R
import corsica.universita.voxwave.notif.NotificationChannels

private const val TAG = "VoxWaveService"
private const val NOTIF_SERVICE_ID = 2001

/**
 * Slide 107 : La fonctionnalité native impossible en PWA.
 * Un Foreground Service qui maintient l'enregistrement audio ou la lecture multimédia
 * actif avec notification persistante dans la barre système, sans être tué par Doze.
 */
class AudioForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "AudioForegroundService onStartCommand : $action")

        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: getString(R.string.service_recording_title)
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: getString(R.string.recording_in_progress)

        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AudioForegroundService::class.java).apply { this.action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.CHANNEL_AUDIO_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.record_stop), stopIntent)
            .setOngoing(true)
            .build()

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIF_SERVICE_ID, notification, serviceType)
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_START_RECORD = "corsica.universita.voxwave.action.START_RECORD"
        const val ACTION_STOP = "corsica.universita.voxwave.action.STOP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"

        fun startRecordingService(context: Context, title: String, text: String) {
            val intent = Intent(context, AudioForegroundService::class.java).apply {
                action = ACTION_START_RECORD
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TEXT, text)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}

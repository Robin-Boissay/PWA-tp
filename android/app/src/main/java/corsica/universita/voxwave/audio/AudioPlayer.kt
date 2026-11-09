package corsica.universita.voxwave.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

private const val TAG = "VoxWaveAudio"

/**
 * Lecteur audio léger basé sur MediaPlayer Android.
 */
class AudioPlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    var currentlyPlayingPath: String? = null
        private set

    fun play(path: String, onFinished: () -> Unit) {
        stop()
        try {
            val mp = MediaPlayer()
            if (path.startsWith("content://") || path.startsWith("android.resource://")) {
                mp.setDataSource(context, Uri.parse(path))
            } else {
                val file = File(path)
                if (!file.exists()) {
                    Log.w(TAG, "Fichier audio introuvable : $path")
                    return
                }
                mp.setDataSource(file.absolutePath)
            }

            mp.prepare()
            mp.setOnCompletionListener {
                stop()
                onFinished()
            }
            mp.start()
            player = mp
            currentlyPlayingPath = path
            Log.d(TAG, "Lecture démarrée : $path")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la lecture audio : ${e.message}", e)
            stop()
            onFinished()
        }
    }

    fun stop() {
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            player = null
            currentlyPlayingPath = null
        }
    }

    val isPlaying: Boolean
        get() = player?.isPlaying == true
}

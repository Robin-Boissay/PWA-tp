package corsica.universita.voxwave.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

private const val TAG = "VoxWaveAudio"

/**
 * Gestionnaire d'enregistrement vocal natif basé sur MediaRecorder.
 * Enregistre au format AAC/M4A haute qualité dans le dossier cache/files de l'application.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var currentOutputFile: File? = null
        private set

    fun start(): File {
        val outputDir = File(context.filesDir, "audio").apply { mkdirs() }
        val file = File(outputDir, "memo_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128000)
            mr.setAudioSamplingRate(44100)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            Log.d(TAG, "Enregistrement vocal démarré vers ${file.name}")
        } catch (e: IOException) {
            Log.e(TAG, "Erreur démarrage enregistrement : ${e.message}", e)
            mr.release()
            throw e
        }
        return file
    }

    fun stop(): File? {
        val file = currentOutputFile
        try {
            recorder?.apply {
                stop()
                release()
            }
            Log.d(TAG, "Enregistrement vocal terminé avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur arrêt enregistreur : ${e.message}", e)
        } finally {
            recorder = null
        }
        return file
    }

    fun cancel() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }
}

package corsica.universita.voxwave.ui.episodes

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import corsica.universita.voxwave.VoxWaveContainer
import corsica.universita.voxwave.audio.AudioPlayer
import corsica.universita.voxwave.audio.AudioRecorder
import corsica.universita.voxwave.data.Episode
import corsica.universita.voxwave.data.EpisodesRepository
import corsica.universita.voxwave.notif.NotificationChannels
import corsica.universita.voxwave.service.AudioForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "VoxWaveVM"
private const val KEY_TITLE = "draft_title"
private const val KEY_SUMMARY = "draft_summary"
private const val KEY_TRANSCRIPT = "draft_transcript"

class EpisodesViewModel(
    private val savedState: SavedStateHandle,
    private val repository: EpisodesRepository,
    private val context: Context,
) : ViewModel() {

    private val recorder = AudioRecorder(context)
    private val player = AudioPlayer(context)

    /** Liste réactive des épisodes Room, survit à la rotation (+ 5 s sous WhileSubscribed) */
    val episodes: StateFlow<List<Episode>> = repository.episodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Badge Outbox : nombre d'épisodes en attente de synchronisation */
    val pendingCount: StateFlow<Int> = episodes.map { list -> list.count { !it.synced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Brouillons sauvés dans SavedStateHandle (survie à la mort du process)
    val draftTitle: StateFlow<String> = savedState.getStateFlow(KEY_TITLE, "")
    val draftSummary: StateFlow<String> = savedState.getStateFlow(KEY_SUMMARY, "")
    val draftTranscript: StateFlow<String> = savedState.getStateFlow(KEY_TRANSCRIPT, "")

    // État d'enregistrement
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _recordedAudioFile = MutableStateFlow<File?>(null)
    val recordedAudioFile: StateFlow<File?> = _recordedAudioFile.asStateFlow()

    // État de lecture audio
    private val _playingEpisodeId = MutableStateFlow<Long?>(null)
    val playingEpisodeId: StateFlow<Long?> = _playingEpisodeId.asStateFlow()

    private var timerJob: Job? = null

    fun onTitleChange(text: String) { savedState[KEY_TITLE] = text }
    fun onSummaryChange(text: String) { savedState[KEY_SUMMARY] = text }
    fun onTranscriptChange(text: String) { savedState[KEY_TRANSCRIPT] = text }

    fun startRecording() {
        try {
            val file = recorder.start()
            _recordedAudioFile.value = file
            _isRecording.value = true
            _recordingSeconds.value = 0

            // Démarre le Foreground Service (impossible en PWA : maintient l'enregistrement en arrière-plan)
            AudioForegroundService.startRecordingService(
                context,
                "Enregistrement VoxWave",
                "Microphone actif en arrière-plan…",
            )

            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingSeconds.value += 1
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur démarrage enregistrement : ${e.message}")
            _isRecording.value = false
        }
    }

    fun stopRecording() {
        val file = recorder.stop()
        _isRecording.value = false
        timerJob?.cancel()
        _recordedAudioFile.value = file
        AudioForegroundService.stopService(context)
    }

    fun cancelRecording() {
        recorder.cancel()
        _isRecording.value = false
        timerJob?.cancel()
        _recordingSeconds.value = 0
        _recordedAudioFile.value = null
        AudioForegroundService.stopService(context)
    }

    fun publishEpisode() {
        val title = draftTitle.value.trim()
        val summary = draftSummary.value.trim()
        val transcript = draftTranscript.value.trim()
        val file = _recordedAudioFile.value

        if (title.isEmpty() || summary.isEmpty()) return

        val durationSec = _recordingSeconds.value
        val mins = durationSec / 60
        val secs = durationSec % 60
        val durationStr = String.format("%02d:%02d", mins, secs)

        viewModelScope.launch {
            val id = repository.add(
                title = title,
                summary = summary,
                transcript = transcript.ifEmpty { "Mémo enregistré le ${System.currentTimeMillis()}" },
                audioPath = file?.absolutePath,
                duration = if (durationSec > 0) durationStr else "00:02",
            )
            // Déclenche la notification locale immédiate
            NotificationChannels.showNewEpisodeNotification(context, title, summary, id)

            // Annexe 4 (slide 10) : Événement d'activation memo_created (avec paramètre offline)
            val isOffline = try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                cm?.activeNetwork == null
            } catch (_: Exception) { false }
            VoxWaveContainer.analytics.logMemoCreated(offline = isOffline)

            // Réinitialiser les champs
            savedState[KEY_TITLE] = ""
            savedState[KEY_SUMMARY] = ""
            savedState[KEY_TRANSCRIPT] = ""
            _recordedAudioFile.value = null
            _recordingSeconds.value = 0
        }
    }

    fun triggerTestNotification() {
        NotificationChannels.showTestNotification(context)
    }

    fun deleteEpisode(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun togglePlayEpisode(episode: Episode) {
        if (_playingEpisodeId.value == episode.id) {
            player.stop()
            _playingEpisodeId.value = null
        } else {
            // Annexe 4 (slide 10) : Événement d'usage audio_played
            VoxWaveContainer.analytics.logAudioPlayed(isSeed = episode.isSeed)

            val path = episode.audioPath
            if (path != null) {
                _playingEpisodeId.value = episode.id
                player.play(path) {
                    _playingEpisodeId.value = null
                }
            } else {
                Log.d(TAG, "Épisode seed ou sans fichier local : simulation de lecture")
                _playingEpisodeId.value = episode.id
                viewModelScope.launch {
                    delay(3000)
                    if (_playingEpisodeId.value == episode.id) {
                        _playingEpisodeId.value = null
                    }
                }
            }
        }
    }

    /** TD A3 : Réception d'un Intent (partage audio ou texte depuis une autre app) */
    fun receiveSharedAudio(uriString: String, titleHint: String? = null) {
        Log.d(TAG, "Audio reçu par Intent : $uriString")
        savedState[KEY_TITLE] = titleHint ?: "Mémo importé"
        savedState[KEY_SUMMARY] = "Fichier audio reçu depuis une autre application"
        savedState[KEY_TRANSCRIPT] = "Source: $uriString"
    }

    fun receiveSharedText(text: String) {
        Log.d(TAG, "Texte reçu par Intent : $text")
        savedState[KEY_TRANSCRIPT] = text
        if (draftTitle.value.isEmpty()) {
            savedState[KEY_TITLE] = "Lien ou note partagée"
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorder.cancel()
        player.stop()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                EpisodesViewModel(
                    savedState = handle,
                    repository = VoxWaveContainer.repository,
                    context = VoxWaveContainer.appContext,
                )
            }
        }
    }
}

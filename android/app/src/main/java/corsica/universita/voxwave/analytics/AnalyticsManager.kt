package corsica.universita.voxwave.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

private const val TAG = "VoxWaveAnalytics"
private const val PREFS_NAME = "voxwave_analytics_prefs"
private const val KEY_ANALYTICS_CONSENT = "analytics_consent"

/**
 * Gestionnaire d'Analytics conforme à l'Annexe 4 du cours :
 * - Rien n'est envoyé avant le consentement explicite de l'utilisateur (RGPD/CNIL).
 * - Plan de marquage strict sans aucune donnée personnelle.
 * - Écrans nommés en dur pour résister à l'obfuscation R8.
 */
class AnalyticsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * null = non encore demandé
     * true = accepté
     * false = refusé
     */
    fun getConsent(): Boolean? {
        return if (!prefs.contains(KEY_ANALYTICS_CONSENT)) {
            null
        } else {
            prefs.getBoolean(KEY_ANALYTICS_CONSENT, false)
        }
    }

    fun setConsent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANALYTICS_CONSENT, enabled).apply()
        try {
            Firebase.analytics.setAnalyticsCollectionEnabled(enabled)
            Log.d(TAG, "Consentement Analytics enregistré : $enabled")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase non initialisé : ${e.message}")
        }
    }

    fun initAtStartup() {
        val consent = getConsent()
        try {
            Firebase.analytics.setAnalyticsCollectionEnabled(consent == true)
        } catch (e: Exception) {
            Log.w(TAG, "Erreur initialisation Firebase Analytics : ${e.message}")
        }
    }

    /**
     * Slide 8 : Écran vu à la main pour Jetpack Compose (évite MainActivity automatique)
     */
    fun logScreenView(screenName: String) {
        if (getConsent() != true) return
        try {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            }
            Log.d(TAG, "Screen view : $screenName")
        } catch (e: Exception) {
            Log.w(TAG, "Erreur logScreenView : ${e.message}")
        }
    }

    /**
     * Plan de marquage (Slide 10) :
     * 1. Activation : mémo créé (en ligne ou hors ligne)
     */
    fun logMemoCreated(offline: Boolean) {
        if (getConsent() != true) return
        try {
            Firebase.analytics.logEvent("memo_created") {
                param("offline", if (offline) 1L else 0L)
            }
            Log.d(TAG, "Event : memo_created (offline = $offline)")
        } catch (e: Exception) {
            Log.w(TAG, "Erreur logMemoCreated : ${e.message}")
        }
    }

    /**
     * 2. Rétention / Engagement : écoute d'un mémo
     */
    fun logAudioPlayed(isSeed: Boolean) {
        if (getConsent() != true) return
        try {
            Firebase.analytics.logEvent("audio_played") {
                param("is_seed", if (isSeed) 1L else 0L)
            }
            Log.d(TAG, "Event : audio_played (isSeed = $isSeed)")
        } catch (e: Exception) {
            Log.w(TAG, "Erreur logAudioPlayed : ${e.message}")
        }
    }

    /**
     * 3. Arrière-plan : synchronisation WorkManager
     */
    fun logSyncDone(count: Int) {
        if (getConsent() != true) return
        try {
            Firebase.analytics.logEvent("sync_done") {
                param("count", count.toLong())
            }
            Log.d(TAG, "Event : sync_done (count = $count)")
        } catch (e: Exception) {
            Log.w(TAG, "Erreur logSyncDone : ${e.message}")
        }
    }

    /**
     * 4. Capacité native : utilisation du microphone
     */
    fun logMicPermission(granted: Boolean) {
        if (getConsent() != true) return
        try {
            Firebase.analytics.logEvent("mic_permission") {
                param("result", if (granted) "granted" else "denied")
            }
            Log.d(TAG, "Event : mic_permission (result = $granted)")
        } catch (e: Exception) {
            Log.w(TAG, "Erreur logMicPermission : ${e.message}")
        }
    }
}

package corsica.universita.voxwave

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LifecycleEventObserver
import corsica.universita.voxwave.ui.VoxWaveNavigation
import corsica.universita.voxwave.ui.episodes.EpisodesViewModel
import corsica.universita.voxwave.ui.theme.VoxWaveTheme

private const val TAG = "VoxWave"

class MainActivity : ComponentActivity() {

    private val viewModel: EpisodesViewModel by viewModels { EpisodesViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Jour 4 : Observation du cycle de vie
        Log.d(TAG, "onCreate — instance ${System.identityHashCode(this)}, savedInstanceState : ${savedInstanceState != null}")
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                Log.d(TAG, "Activity ${event.name} — instance ${System.identityHashCode(this)}")
            },
        )

        // Traitement de l'Intent initial au premier lancement seulement
        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            VoxWaveTheme {
                VoxWaveNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            // TD A3 : Partage de fichier audio depuis une autre app (WhatsApp, Dictaphone, etc.)
            Intent.ACTION_SEND -> {
                val type = intent.type ?: ""
                if (type.startsWith("audio/")) {
                    @Suppress("DEPRECATION")
                    val audioUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    Log.d(TAG, "ACTION_SEND reçu (audio) : $audioUri")
                    audioUri?.let { uri ->
                        viewModel.receiveSharedAudio(uri.toString(), "Audio partagé")
                    }
                } else if (type == "text/plain") {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    Log.d(TAG, "ACTION_SEND reçu (texte) : $text")
                    viewModel.receiveSharedText(text)
                }
            }

            // Jour 4 (TD A3) : App Link — un clic sur le lien web ouvre l'application native !
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    Log.d(TAG, "ACTION_VIEW reçu (App Link) : $uri")
                    viewModel.receiveSharedText("Ouvert via App Link : $uri")
                }
            }
        }
    }
}

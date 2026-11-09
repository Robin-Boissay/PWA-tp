package corsica.universita.voxwave.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import corsica.universita.voxwave.ui.episodes.EpisodesListScreen
import corsica.universita.voxwave.ui.episodes.openPwa
import corsica.universita.voxwave.ui.episodes.shareEpisode
import kotlinx.serialization.Serializable

@Serializable
data object EpisodesList : NavKey

/**
 * Navigation 3 : le backstack est un état sauvegardé à la rotation.
 */
@Composable
fun VoxWaveNavigation() {
    val backStack = rememberNavBackStack(EpisodesList)
    val context = LocalContext.current

    // Annexe 4 (slide 8) : Suivi d'écran manuel pour Compose (noms écrits en dur pour R8)
    val current = backStack.lastOrNull()
    androidx.compose.runtime.LaunchedEffect(current) {
        val name = when (current) {
            EpisodesList -> "episodes_list"
            else -> return@LaunchedEffect
        }
        corsica.universita.voxwave.VoxWaveContainer.analytics.logScreenView(name)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<EpisodesList> {
                EpisodesListScreen(
                    onOpenPwa = { context.openPwa() },
                    onShare = { episode -> context.shareEpisode(episode) },
                )
            }
        },
    )
}

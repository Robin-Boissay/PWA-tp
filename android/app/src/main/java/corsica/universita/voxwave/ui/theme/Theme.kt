package corsica.universita.voxwave.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = TextPrimary,
    primaryContainer = Indigo600,
    onPrimaryContainer = TextPrimary,
    secondary = Violet400,
    onSecondary = TextPrimary,
    background = Slate900,
    onBackground = TextPrimary,
    surface = Slate800,
    onSurface = TextPrimary,
    surfaceVariant = Slate700,
    onSurfaceVariant = TextMuted,
    outline = Slate600,
    error = Rose500,
    onError = TextPrimary,
)

@Composable
fun VoxWaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}

package corsica.universita.voxwave.data

/**
 * Modèle de données exposé à l'UI Compose.
 * Représente un mémo vocal ou un épisode de micro-podcast.
 */
data class Episode(
    val id: Long = 0,
    val title: String,
    val summary: String,
    val transcript: String,
    val audioPath: String? = null,
    val duration: String = "00:02",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val isSeed: Boolean = false,
)

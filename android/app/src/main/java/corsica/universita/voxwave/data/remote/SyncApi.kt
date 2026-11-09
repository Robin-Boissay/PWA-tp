package corsica.universita.voxwave.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class EpisodeDto(
    val id: Long,
    val title: String,
    val summary: String,
    val transcript: String,
    val createdAt: Long,
)

@Serializable
data class EchoResponse(
    val url: String = "",
    val json: EpisodeDto? = null,
)

/**
 * Interface Retrofit vers le service d'écho (par exemple https://httpbin.org/post).
 * Exécutée dans WorkManager en arrière-plan sans bloquer le Main Thread.
 */
interface SyncApi {
    @POST("post")
    suspend fun push(@Body body: EpisodeDto): EchoResponse
}

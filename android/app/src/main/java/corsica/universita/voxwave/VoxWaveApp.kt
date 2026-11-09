package corsica.universita.voxwave

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import corsica.universita.voxwave.data.EpisodesRepository
import corsica.universita.voxwave.data.local.VoxWaveDatabase
import corsica.universita.voxwave.data.remote.Network
import corsica.universita.voxwave.data.remote.SyncApi
import corsica.universita.voxwave.notif.NotificationChannels

/**
 * Pas de framework d'injection lourd (Hilt) : conteneur d'inversion de contrôle
 * construit à la main, une fois par process (comme dans le cours TD A4-A5).
 */
object VoxWaveContainer {
    lateinit var appContext: Context
        private set
    lateinit var database: VoxWaveDatabase
        private set
    lateinit var api: SyncApi
        private set
    lateinit var workManager: WorkManager
        private set
    lateinit var repository: EpisodesRepository
        private set
    lateinit var analytics: corsica.universita.voxwave.analytics.AnalyticsManager
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        database = VoxWaveDatabase.build(appContext)
        api = Network.syncApi(Network.okHttp(appContext))
        workManager = WorkManager.getInstance(appContext)
        repository = EpisodesRepository(database.episodeDao(), api, workManager)
        analytics = corsica.universita.voxwave.analytics.AnalyticsManager(appContext).apply {
            initAtStartup()
        }
    }
}

/**
 * Classe Application : instanciée avant toute Activity ou Worker.
 * Initialise le conteneur de dépendances et les canaux de notification (TD A6).
 */
class VoxWaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VoxWaveContainer.init(this)
        NotificationChannels.createChannels(this)
    }
}

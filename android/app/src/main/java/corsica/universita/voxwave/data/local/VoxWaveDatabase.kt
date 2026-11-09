package corsica.universita.voxwave.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [EpisodeEntity::class], version = 1, exportSchema = true)
abstract class VoxWaveDatabase : RoomDatabase() {

    abstract fun episodeDao(): EpisodeDao

    companion object {
        private const val DB_NAME = "voxwave.db"

        fun build(context: Context): VoxWaveDatabase {
            lateinit var database: VoxWaveDatabase
            database = Room.databaseBuilder(
                context.applicationContext,
                VoxWaveDatabase::class.java,
                DB_NAME,
            ).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Pré-remplissage avec les 3 épisodes du cours sur Dispatchers.IO
                    CoroutineScope(Dispatchers.IO).launch {
                        val seeds = listOf(
                            EpisodeEntity(
                                title = "Épisode 1 : Le Manifeste du Son Libre",
                                summary = "Pourquoi les formats audio courts et ouverts transforment notre manière de partager des idées sur le Web.",
                                transcript = "Bienvenue sur VoxWave ! La voix est le moyen le plus direct et spontané d'exprimer une pensée. Dans un monde saturé d'écrans, ce micro-podcast défend une diffusion sonore universelle et accessible à tous, même hors ligne.",
                                duration = "00:02",
                                createdAt = System.currentTimeMillis() - 86400000L * 2,
                                synced = true,
                                isSeed = true,
                            ),
                            EpisodeEntity(
                                title = "Épisode 2 : Débranché mais branché — Le mode avion",
                                summary = "Comment continuer à enregistrer et écouter des idées au sommet d'une montagne sans réseau 4G.",
                                transcript = "Être coupé du réseau n'est plus un frein à la création. Grâce à Room et WorkManager, votre voix est mise en sécurité locale. Dès que le signal revient, la magie de la synchronisation opère en toute transparence.",
                                duration = "00:02",
                                createdAt = System.currentTimeMillis() - 86400000L,
                                synced = true,
                                isSeed = true,
                            ),
                            EpisodeEntity(
                                title = "Épisode 3 : Les coulisses du Web Audio & Media Session",
                                summary = "Contrôler la lecture d'un mémo vocal directement depuis l'écran de verrouillage ou sa montre connectée.",
                                transcript = "L'intégration multimédia native transforme une application en un véritable lecteur système. Pochette, métadonnées, boutons de contrôle : tout s'intègre harmonieusement dans les commandes multimédias de l'OS.",
                                duration = "00:02",
                                createdAt = System.currentTimeMillis(),
                                synced = true,
                                isSeed = true,
                            ),
                        )
                        database.episodeDao().insertAll(seeds)
                    }
                }
            }).build()
            return database
        }
    }
}

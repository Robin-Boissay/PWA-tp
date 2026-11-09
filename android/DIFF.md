# Étape A5 — L'outbox WorkManager

État du projet Carnet **à la fin du TD A5** (Jour 5, chapitre 4). Par rapport à `android/etapes/A4/` :

## Fichiers ajoutés

- `app/src/main/java/corsica/universita/carnet/data/remote/SyncApi.kt` — `@Serializable NoteDto(id, text, createdAt)`, `@Serializable EchoResponse(url, json)`, `interface SyncApi { @POST("post") suspend fun push(@Body body: NoteDto): EchoResponse }`.
- `app/src/main/java/corsica/universita/carnet/data/remote/Network.kt` — `object Network` : `okHttp(context)` (cache HTTP 10 Mo dans `cacheDir/http`, `HttpLoggingInterceptor` BASIC vers Logcat `Carnet`, délais 15 s / 20 s) ; `syncApi(client)` (Retrofit, `baseUrl(BuildConfig.SYNC_BASE_URL)`, convertisseur kotlinx.serialization avec `ignoreUnknownKeys`).
- `app/src/main/java/corsica/universita/carnet/work/SyncWorker.kt` — `class SyncWorker : CoroutineWorker` : `repository.syncPending()` ; `Result.success(count)` ; `Result.retry()` sur `IOException` ; `Result.failure()` sur `HttpException` ; `UNIQUE_NAME = "sync"`.
- `app/schemas/…/3.json` — schéma v3.

## Fichiers modifiés

- `data/local/NoteEntity.kt` — colonne **`synced`** (`@ColumnInfo(defaultValue = "0")`).
- `data/local/NoteDao.kt` — `unsynced(): List<NoteEntity>` (`WHERE synced = 0 ORDER BY createdAt`), `markSynced(ids)` (`UPDATE … SET synced = 1 WHERE id IN (:ids)`).
- `data/local/CarnetDatabase.kt` — `version = 3`, `MIGRATION_2_3` (`ALTER TABLE notes ADD COLUMN synced INTEGER NOT NULL DEFAULT 0`) — **la migration que l'étudiant écrit seul**.
- `data/NotesRepository.kt` — `NotesRepository(dao, api, workManager)` ; `add()` écrit puis `enqueueSync()` ; `enqueueSync()` = `OneTimeWorkRequestBuilder<SyncWorker>` + contrainte `NetworkType.CONNECTED` + backoff exponentiel 30 s, `enqueueUniqueWork("sync", KEEP, …)` ; `syncPending(): Int` = POST de chaque note non synchronisée vers l'écho, puis `markSynced`.
- `data/Note.kt` — `synced: Boolean = false`.
- `CarnetApp.kt` — `CarnetContainer` construit aussi `api` (Retrofit) et `workManager` ; le repository reçoit les trois.
- `ui/notes/NotesViewModel.kt` — `pendingCount: StateFlow<Int>` dérivé de `notes`.
- `ui/notes/NotesListScreen.kt` — badge « n en attente » à côté du compteur ; « · en attente » sous chaque note non partie.
- `res/values/strings.xml` — `pending`, `pending_count`.

## Fichiers supprimés

- `work/README.md` (« vide jusqu'au TD A5 »).

## Ce que fait Carnet à la fin

Chaque note créée est écrite sur le disque puis demandée en synchronisation ; quand le réseau est là, `SyncWorker` l'envoie à `https://httpbin.org/post` (ou `SYNC_BASE_URL` de `local.properties`) et la marque `synced` — app ouverte, fermée ou tuée. En mode avion elle attend (badge « n en attente ») ; sous Doze elle attend plus longtemps. Logcat (`tag:Carnet`) montre la requête, la réponse de l'écho et le nombre de notes envoyées ; Background Task Inspector montre le travail `sync`.

L'échec préalable du TD (un `URL(...).readText()` dans le ViewModel → `NetworkOnMainThreadException`) n'est pas dans ce corrigé : il est à provoquer, puis à remplacer par ce qui précède.

## Compilation

`./gradlew assembleDebug --no-daemon --console=plain --max-workers=1` → `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk`.

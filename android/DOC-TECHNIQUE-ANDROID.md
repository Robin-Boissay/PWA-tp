# 🎙️ VoxWave Android — Document Technique de Présentation

**Projet** : VoxWave — Portage PWA vers Application Android Native  
**Module** : Développement d'applications mobiles (M2 Informatique · Università di Corsica)  
**Auteur** : Robin  
**Date** : Lundi 9 novembre 2026  
**Stack** : Kotlin 2.4 · Jetpack Compose · Material 3 · Room 2.8 · WorkManager 2.12 · Navigation 3 · API 37 (Android 17)

---

## 1. 📱 La Démo : Ce que fait l'application

VoxWave est une application native de **mémos vocaux et de mini-podcasts publics**.

- **Écoute & Consultation** : Présentation d'un catalogue d'épisodes et de mémos locaux. Chaque carte propose le titre, le résumé, une transcription déroulante et un lecteur audio avec bouton Play/Pause.
- **Studio Vocal Intégré** : Enregistrement de mémos sonores au microphone avec chronomètre en temps réel, pulsation animée et formulaire de publication (titre, résumé court, transcription).
- **Mode Hors Ligne Intégral (Offline-first)** : En mode avion, l'application s'ouvre instantanément, conserve tous les mémos locaux et permet d'en enregistrer de nouveaux.
- **Synchronisation transparente (Outbox)** : Les mémos créés hors-ligne sont tagués `⏳ En attente de sync`. Dès le retour du réseau, ils sont expédiés au serveur et passent en `✅ Synchronisé`.
- **Intents du système** :
  - **App Link** : L'URL web de la PWA (`https://mini-podcast.netlify.app/`) ouvre l'application native.
  - **Cible de partage audio (`ACTION_SEND`)** : Recevoir un fichier audio depuis WhatsApp ou l'enregistreur système pour créer un épisode.
  - **Partage sortant** : Partager un mémo vers d'autres applications avec son résumé et son lien.

---

## 2. 🏗️ L'Architecture : Les Couches et les Données

L'application respecte les principes de la **Clean Architecture** sans framework magique :

```text
[ UI (Jetpack Compose) ]
       │  ▲
       │  │ StateFlow (survie rotation + WhileSubscribed 5s)
       ▼  │
[ EpisodesViewModel ] (SavedStateHandle : survie mort du process)
       │  ▲
       ▼  │
[ EpisodesRepository ] (Source unique de vérité)
       │                     │
       ▼                     ▼
[ Room (VoxWaveDatabase) ]  [ WorkManager (SyncWorker) ]
       │                             │
 (SQLite local)                      ▼
                           [ Retrofit (SyncApi) ]
                                     │
                             (Écho HTTP distant)
```

- **Le Conteneur manuel (`VoxWaveContainer`)** : Conforme aux TD A4/A5, aucun framework d'injection lourd (Hilt) n'est utilisé. Un conteneur singleton instancie la base Room, le client Retrofit/OkHttp, WorkManager et le Repository une seule fois au niveau de `Application.onCreate()`.
- **D'où viennent les données ?** La base locale Room est la seule source de vérité pour l'interface. L'UI ne communique qu'avec le Repository. Le réseau n'est qu'un canal d'écho asynchrone orchestré par WorkManager.

---

## 3. ⚖️ Les Choix Techniques Argumentés

- **Room plutôt que DataStore** : Les mémos vocaux sont des données relationnelles structurées comportant plusieurs champs (`id`, `title`, `summary`, `transcript`, `audioPath`, `duration`, `createdAt`, `synced`) et des opérations de tri et de filtrage (`WHERE synced = 0`). Room est indispensable ; DataStore ne sert qu'aux préférences clés-valeurs scalaires.
- **Quand part l'envoi ?** L'envoi ne part **jamais directement sur le Main Thread** de l'UI. Dès qu'un mémo est créé, il est immédiatement écrit dans Room, puis une tâche unique `SyncWorker` est planifiée dans WorkManager avec la contrainte `NetworkType.CONNECTED` et un backoff exponentiel. L'OS décide du moment optimal pour envoyer, même si l'application est en arrière-plan ou fermée.
- **Quelle permission et pourquoi ?**
  - `RECORD_AUDIO` (Microphone) : Demandée **uniquement au moment où l'utilisateur clique sur le bouton d'enregistrement**.
  - Traitée avec `PermissionFlow` : gestion des 4 états (`NotAsked`, `Granted`, `DeniedOnce`, `DeniedForever`).
  - Si l'utilisateur refuse une première fois : affichage d'un dialogue explicatif (rationale).
  - Si refus définitif : redirection vers les Paramètres système (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`).

---

## 4. 🚀 Le Natif : Ce que l'OS accorde (Impossible en PWA)

Par rapport à la version PWA, Android accorde des capacités impossibles sur le web :

1. **Foreground Service Audio (`AudioForegroundService`)** :
   - En PWA, dès que l'écran s'éteint ou que l'utilisateur quitte le navigateur, le Service Worker est suspendu par le système et l'enregistrement microphone est coupé.
   - En natif, le `ForegroundService` avec type `microphone` maintient l'enregistrement en continu en arrière-plan avec une notification système persistante.
2. **Cible de partage de fichiers réels (`ACTION_SEND` avec `audio/*`)** :
   - L'application est enregistrée auprès de l'OS comme récepteur de flux audio. N'importe quel fichier audio partagé depuis une autre application peut être importé directement dans VoxWave.
3. **App Link vérifié au niveau OS (`android:autoVerify="true"`)** :
   - L'OS intercepte l'URL de la PWA et démarre l'application native directement sans passer par le navigateur.
4. **Arrière-plan garanti par l'OS (`WorkManager`)** :
   - Contrairement au Background Sync web (qui n'est supporté que sur Chrome et peut être abandonné par le navigateur), WorkManager garantit l'exécution de l'Outbox même après redémarrage du smartphone.

---

## 5. 💥 Les Échecs Vécus et Réparés

1. **`NetworkOnMainThreadException`** :
   - *Le problème* : Tenter d'effectuer une requête réseau ou un accès disque directement dans le ViewModel bloque l'UI et fait crasher l'OS.
   - *La réparation* : Toutes les requêtes Room et Retrofit sont suspendues et exécutées sur `Dispatchers.IO` ou déléguées au `CoroutineWorker` de WorkManager.
2. **Le refus définitif de permission** :
   - *Le problème* : Après 2 refus de la permission microphone, l'appel à `launcher.launch()` ne fait plus rien et renvoie silencieusement `false`.
   - *La réparation* : `PermissionFlow` détecte cet état (`DeniedForever`) et remplace la demande par un dialogue invitant l'utilisateur à réactiver le micro dans les Paramètres système de l'application.
3. **La perte d'état lors de la rotation et mort du process** :
   - *Le problème* : Un enregistrement en cours de saisie ou un brouillon est détruit si l'utilisateur tourne son écran ou si l'OS tue le process pour libérer de la mémoire sous Doze.
   - *La réparation* : Les champs de saisie sont persistés dans `SavedStateHandle`, et la liste d'épisodes est exposée via un `StateFlow` réactif avec `SharingStarted.WhileSubscribed(5_000)`.

---

## 6. 📊 Analytics & Mesure d'Audience (Annexe 4)

L'application intègre une solution de mesure d'audience strictement conforme aux exigences du cours et à la réglementation européenne (RGPD / CNIL) :

### A. Outil et Dépendances
- **SDK** : `com.google.firebase:firebase-analytics` via BoM Firebase.
- **Suppression de l'AD_ID** : Conformément à la recommandation CNIL (Slide 5), l'identifiant publicitaire Android est explicitement révoqué dans le manifeste pour interdire le pistage publicitaire :
  ```xml
  <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove" />
  ```

### B. Conformité RGPD / CNIL (Consentement Préalable)
1. **Désactivation par défaut** dans le manifeste :
   ```xml
   <meta-data android:name="firebase_analytics_collection_enabled" android:value="false" />
   ```
2. **Bannière de consentement explicite** :
   - Au premier lancement de l'application, un `AlertDialog` Material 3 demande l'accord de l'utilisateur pour la mesure d'audience anonyme.
   - Le choix est persisté dans les préférences locales (`SharedPreferences`).
   - L'activation n'a lieu qu'après acceptation explicite via `Firebase.analytics.setAnalyticsCollectionEnabled(true)`. Si l'utilisateur refuse ou ignore le dialogue, aucune donnée n'est collectée.
3. **Anonymat strict (Zero PII)** : Aucun titre de mémo, aucun extrait de transcription, ni aucun fichier audio n'est envoyé aux serveurs d'analytics.

### C. Gestion du Single Activity / Jetpack Compose
Puisque toute l'application tourne dans une unique `MainActivity`, l'auto-tracking par défaut de Firebase remonterait tous les écrans sous le nom technique `MainActivity`.
- L'auto-reporting est désactivé dans le manifeste :
  ```xml
  <meta-data android:name="google_analytics_automatic_screen_reporting_enabled" android:value="false" />
  ```
- Les écrans sont trackés **manuellement** via `logEvent(FirebaseAnalytics.Event.SCREEN_VIEW)` dans la navigation avec des noms explicites en dur (`episodes_list`, `studio_record`), préservés même en cas d'obfuscation R8.

### D. Plan de Marquage (Tagging Plan)
| Événement | Catégorie | Déclencheur | Paramètres |
|---|---|---|---|
| `screen_view` | Navigation | Changement de destination Compose | `screen_name` : `"episodes_list"`, `"studio_record"` |
| `memo_created` | Activation | Enregistrement et publication d'un mémo | `offline` : `1L` (hors-ligne) ou `0L` (en ligne) |
| `audio_played` | Rétention / Engagement | Démarrage de la lecture d'un mémo | `is_seed` : `1L` (préchargé) ou `0L` (créé par l'utilisateur) |
| `sync_done` | Arrière-plan | Fin de la synchronisation WorkManager | `count` : nombre d'épisodes synchronisés |
| `mic_permission` | Capacité Native | Réponse à la demande de micro | `result` : `"granted"` ou `"denied"` |

### E. Test et Validation (DebugView)
Pour vérifier la bonne remontée des événements en temps réel sans attendre le délai d'agrégation standard de Firebase :
```bash
# Activer le mode DebugView pour VoxWave
adb shell setprop debug.firebase.analytics.app corsica.universita.voxwave

# Désactiver le mode après test
adb shell setprop debug.firebase.analytics.app .none.
```


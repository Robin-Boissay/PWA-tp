# 🎙️ VoxWave — Plateforme Audio (PWA & Android Native)

> **Mini-podcast & Mémos Vocaux Publics**  
> Cours M2 Informatique · Università di Corsica · 2026.  
> Ce dépôt regroupe les deux implémentations du projet : l'application web progressive (**PWA**) et l'application mobile native (**Android**).

---

## 🧭 Navigation & Séparation des Projets (Android vs PWA)

Le projet comporte deux volets autonomes et complémentaires. Chaque volet dispose de son propre code source, de son propre guide de démarrage et de sa propre documentation technique :

| Projet | Dossier | Guide de démarrage | Document Technique Détaillé | Description |
| :--- | :--- | :--- | :--- | :--- |
| 🤖 **Application Android** | [`android/`](./android/) | [**`android/README.md`**](./android/README.md) | [**`android/DOC-TECHNIQUE-ANDROID.md`**](./android/DOC-TECHNIQUE-ANDROID.md) | Application native moderne en **Kotlin & Jetpack Compose** |
| 🌐 **Progressive Web App** | Racine (`./`) | [Section PWA ci-dessous](#-partie-pwa--progressive-web-app-racine) | [**`DOC-TECHNIQUE.md`**](./DOC-TECHNIQUE.md) | PWA Offline-First avec **Service Worker, Web Audio & IndexedDB** |

---

## 🤖 Partie Android : Application Native (`android/`)

L'application native Android a été entièrement conçue et implémentée dans le sous-dossier [**`android/`**](./android/). Elle constitue le portage natif de VoxWave en exploitant au maximum les capacités matérielles et architecturales modernes du système Android.

### 🛠️ Ce qui a été réalisé sur Android
- **Interface déclarative moderne** : Conçue à 100% en **Jetpack Compose** et **Material 3** avec gestion d'état réactive via `StateFlow` et survie aux changements de configuration (`ViewModel` + `SavedStateHandle`).
- **Architecture Clean & Local-First** : Injection manuelle via conteneur applicatif (`VoxWaveContainer`) sans framework lourd. Base de données relationnelle locale **Room** (`VoxWaveDatabase`, DAO, migrations) servant de source unique de vérité, permettant une utilisation complète sans connexion.
- **Studio Vocal & Enregistrement Micro** : Capture audio locale via `MediaRecorder` (`AudioRecorder`), calcul de durée en direct, prévisualisation audio et gestion fine des permissions d'enregistrement (`RECORD_AUDIO`) via un flux d'états réactif (`PermissionFlow`) distinguant refus temporaire et refus permanent.
- **Outbox & Synchronisation garantie en arrière-plan** : Les mémos enregistrés hors-ligne sont marqués en attente (`synced = false`). Une tâche unique est confiée au **WorkManager** (`SyncWorker`) avec contrainte réseau `CONNECTED` et politique de réessai exponentielle vers une API REST (Retrofit/OkHttp). L'envoi est garanti même si l'application est en arrière-plan ou fermée.
- **Intégrations profondes avec l'OS Android** :
  - **App Links** : Prise en charge des liens profonds (l'URL de la PWA `https://mini-podcast.netlify.app/` ouvre directement l'application native).
  - **Cible de partage audio (`ACTION_SEND`)** : Réception de flux audio envoyés depuis d'autres applications (messageries, dictaphone) pour créer un épisode.
  - **Partage sortant** : Diffusion native des mémos (titre, résumé, lien) via l'Android Sharesheet.

### 🎥 Démonstration Vidéo
> 📹 **Lien de la vidéo de démonstration** : [Ajouter ici le lien vers votre vidéo YouTube / Loom / Drive](https://)  
*(Remplacer ce lien par l'URL de votre vidéo présentant le parcours utilisateur, l'enregistrement micro, le mode hors-ligne et la synchronisation).*

### 🚀 Lancement & Compilation du projet Android

#### 1. Prérequis SDK & Outils
- **Android Studio** : Quail 4 (2026.1.4) ou plus récent (avec JDK 21 inclus).
- **Android SDK Platform** : `Android 17 (API 37, compileSdk 37)`.
- **SDK Build-Tools** : Version 36 ou plus récente.
- **Min SDK** : API 26 (Android 8.0 Oreo).
- **Appareil** : Smartphone physique (débogage USB activé) ou émulateur Android (API 34 à 37).

#### 2. Configuration (`local.properties`)
À la racine du dossier `android/`, vérifiez ou créez le fichier `local.properties` :
```properties
sdk.dir=/chemin/vers/votre/Android/Sdk
# Options de build personnalisées :
PWA_HOST=mini-podcast.netlify.app
SYNC_BASE_URL=https://httpbin.org/
```

#### 3. Compilation et Génération de l'APK via le terminal
Rendez-vous dans le dossier `android` et utilisez le wrapper Gradle :

```bash
cd android

# Compiler et vérifier le build debug :
./gradlew assembleDebug --no-daemon

# L'APK compilé sera généré dans :
# android/app/build/outputs/apk/debug/app-debug.apk
```

#### 4. Lancement sur appareil ou émulateur
```bash
# Vérifier la détection de l'appareil par adb :
adb devices

# Installer et lancer l'APK directement :
./gradlew installDebug
adb shell am start -n corsica.universita.voxwave/.MainActivity
```
*(Ou ouvrez directement le dossier `android/` dans **Android Studio** puis cliquez sur **Run 'app'** ▶️).*

👉 **Consultez la documentation spécifique Android :**
- [**Guide de démarrage & configuration Android Studio**](./android/README.md)
- [**Document technique d'architecture Android**](./android/DOC-TECHNIQUE-ANDROID.md)

---

## 🌐 Partie PWA : Progressive Web App (Racine)

### 🌟 Présentation
**VoxWave** est une Progressive Web App (PWA) complète, moderne et installable, permettant d'enregistrer des mémos audio et des micro-podcasts avec le microphone de son smartphone, de leur associer un titre, un résumé et une transcription texte complète, et de les diffuser sur le Web.

L'application est conçue selon les principes du **Local-First** et de l'**Offline-First** : elle fonctionne en mode avion, stocke les enregistrements dans IndexedDB, et synchronise automatiquement les mémos via une Outbox au retour de la connexion réseau.

---

### ✨ Fonctionnalités & Capacités Matérielles PWA

- 📱 **Installable** : Manifest Web App complet, affichage `standalone`, icônes 192x192 et 512x512 adaptatives (maskable), bouton d'installation in-app (`beforeinstallprompt`).
- 📴 **100% Hors ligne (Offline-First)** : App Shell en cache avec le Service Worker versionné (`voxwave-v1`), page de secours `/offline.html` en cas de navigation non-mise en cache.
- 🎙️ **Microphone (`MediaRecorder` & `getUserMedia`)** : Enregistrement audio en direct avec visualiseur d'ondes, minuteur et prévisualisation. Permission demandée **uniquement après un geste volontaire** de l'utilisateur avec gestion élégante du refus.
- 🎵 **Media Session API (`navigator.mediaSession`)** : Contrôles de lecture (Play, Pause, avance/recul, pochette, titre) intégrés directement sur l'**écran de verrouillage** et le centre de contrôle du smartphone.
- 🔗 **Web Share API (`navigator.share`)** : Partage natif en 1 clic vers les messageries et réseaux sociaux (avec repli automatique sur le presse-papier).
- 📤 **Outbox & Synchronisation Différée** : Les mémos créés hors-ligne sont enregistrés dans une file d'attente IndexedDB (`outbox`) et envoyés automatiquement dès le retour du réseau (Background Sync + événement `online`).
- 🔄 **Mise à jour maîtrisée** : Bannière interactive informant l'utilisateur d'une nouvelle version disponible sans couper l'audio en cours.
- 🤖 **Optimisation SEO & Agents IA (GEO)** :
  - Contenu public présent directement dans le HTML initial (lisible par `curl` sans exécuter JavaScript).
  - Jumelle Markdown pour crawlers d'IA : [`episodes.md`](./episodes.md).
  - Métadonnées complètes : Open Graph, Twitter Cards, Schema.org JSON-LD (`PodcastSeries`, `PodcastEpisode`).
  - Fichiers d'indexation : [`robots.txt`](./robots.txt), [`sitemap.xml`](./sitemap.xml) et [`llms.txt`](./llms.txt).

---

### 🚀 Lancement de la PWA en local

L'application web ne nécessite aucun build ni compilation complexe. Vous pouvez la lancer avec n'importe quel serveur HTTP statique :

#### Option 1 : Avec `serve` (recommandé avec le fichier `serve.json` fourni)
```bash
npx serve .
# Ouvrez ensuite http://localhost:3000 dans votre navigateur
```

#### Option 2 : Avec Python 3
```bash
python3 -m http.server 3000
# Ouvrez ensuite http://localhost:3000 dans votre navigateur
```

---

### 🧪 Commandes de vérification SEO et Agents IA

Ces commandes permettent de valider que les exigences du sujet de TP sont respectées :

```bash
# 1. Vérifier que les 3 épisodes publics sont bien présents dans le HTML sans exécuter JS :
curl -s -A "GPTBot" http://localhost:3000/ | grep -c "<article[ >]"
# Résultat attendu : 3

# 2. Vérifier que la jumelle Markdown répond correctement :
curl -s -A "ClaudeBot" http://localhost:3000/episodes.md

# 3. Vérifier les en-têtes et les fichiers de découverte :
curl -sI http://localhost:3000/robots.txt
curl -sI http://localhost:3000/sitemap.xml
curl -sI http://localhost:3000/llms.txt
```

---

### 📋 Checklist de conformité du TP PWA

- [x] Déployable en HTTPS, testable sur smartphone et émulateur mobile.
- [x] Installable (Manifest complet, icônes 192/512 + maskable, `display: standalone`).
- [x] Hors ligne vérifié (App Shell en cache, consultation des mémos, page `offline.html`).
- [x] Mise à jour implémentée et justifiée (bannière utilisateur non-intrusive).
- [x] Synchronisation après retour en ligne (Outbox IndexedDB + Background Sync / `online`).
- [x] Visibilité SEO & agents IA (Contenu initial dans le HTML, Open Graph, JSON-LD, `robots.txt`, `sitemap.xml`, `llms.txt`, `episodes.md`).
- [x] Capacités de l'appareil intégrées (Microphone, Media Session, Web Share) avec permissions demandées après action et refus géré sans crash.
- [x] Document technique rédigé ([`DOC-TECHNIQUE.md`](./DOC-TECHNIQUE.md)) selon le modèle fourni.

---

## 📁 Structure globale du dépôt

```text
├── android/                    # 🤖 Application Android Native (Kotlin, Compose, Room, WorkManager)
│   ├── app/                    # Code source Android (data, ui, audio, work, services)
│   ├── README.md               # Guide d'installation et d'exécution Android Studio / Gradle
│   └── DOC-TECHNIQUE-ANDROID.md# Dossier technique d'architecture et choix d'implémentation Android
│
├── index.html                  # 🌐 Shell HTML PWA, épisodes publics & balisage SEO / JSON-LD
├── offline.html                # Page de repli PWA lors d'une navigation hors-ligne
├── style.css                   # Feuilles de styles modernes, responsive et visualiseur audio
├── app.js                      # Logique client PWA : MediaRecorder, MediaSession, WebShare, outbox
├── db.js                       # Gestion IndexedDB avec 'idb' (episodes & outbox)
├── idb.js                      # Bibliothèque utilitaire IndexedDB
├── sw.js                       # Service Worker PWA : mise en cache shell, stratégies SWR, outbox sync
├── manifest.json               # Web App Manifest PWA pour installation
├── serve.json                  # Configuration serveur local 'serve'
├── _headers                    # Configuration des en-têtes HTTP de déploiement (Netlify)
├── robots.txt                  # Directives crawlers et bots d'IA (GPTBot, ClaudeBot, Google-Extended)
├── sitemap.xml                 # Plan du site avec lastmod
├── llms.txt                    # Spécification descriptive pour agents LLM
├── episodes.md                 # Jumelle Markdown du flux public PWA
└── DOC-TECHNIQUE.md            # Document technique PWA complet
```

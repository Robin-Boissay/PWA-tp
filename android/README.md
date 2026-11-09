# Carnet — starter de la Partie II (Android)

Projet de départ des TD A1 à A7 et du TP Android du module « Développement d'applications mobiles » (M2 Informatique, Università di Corsica). Ce dépôt est **le même projet** du premier TD au TP final : chaque TD l'enrichit.

Ce fichier reprend, en texte, l'annexe de mise en place (`annexe-setup-android.md`). **À faire avant le lundi 2 novembre 2026**, et à terminer par le smoke test (section 6).

## 1. La machine

- **RAM** : Google indique 12 Go au minimum et 32 Go recommandés pour Android Studio Quail 4 (2026) ; en pratique 16 Go suffisent, 8 Go sans émulateur.
- **Disque** : 20 Go libres (Android Studio ≈ 3 Go, SDK ≈ 4 Go, image d'émulateur ≈ 2 Go, caches Gradle ≈ 3 Go).
- **Système** : macOS 12+, Windows 10/11 64 bits, Linux 64 bits (glibc 2.31+).
- **Virtualisation** activée dans le BIOS/UEFI (Intel VT-x / AMD-V) pour l'émulateur. Sous Windows : Hyper-V ou Windows Hypervisor Platform activé (Fonctionnalités Windows).

## 2. Android Studio

1. Télécharger **Android Studio Quail 4 Patch 1 (2026.1.4) ou plus récent — stable actuelle : Rabbit 2** sur [developer.android.com/studio](https://developer.android.com/studio).
2. Installer avec les options par défaut (le JDK — « JetBrains Runtime 21 » — est inclus : rien d'autre à installer).
3. Premier lancement : assistant « Standard », accepter les licences, laisser télécharger le SDK.
4. **SDK Manager** (More Actions → SDK Manager) :
   - onglet *SDK Platforms* : cocher **Android 17 (API 37)** ;
   - onglet *SDK Tools* : **Android SDK Build-Tools 36**, **Android SDK Platform-Tools**, **Android Emulator**, **Android SDK Command-line Tools** ;
   - Apply.

Vérifier que `adb` est accessible depuis un terminal :

```bash
# macOS / Linux (à ajouter dans ~/.zshrc ou ~/.bashrc)
export ANDROID_HOME="$HOME/Library/Android/sdk"      # macOS
export ANDROID_HOME="$HOME/Android/Sdk"              # Linux
export PATH="$PATH:$ANDROID_HOME/platform-tools"
```

```powershell
# Windows (PowerShell, puis rouvrir le terminal)
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";$env:LOCALAPPDATA\Android\Sdk\platform-tools", "User")
```

```bash
adb version     # → Android Debug Bridge version 1.0.41 (ou plus)
```

## 3. Un téléphone Android (recommandé)

1. **Options pour les développeurs** : Paramètres → À propos du téléphone → appuyer 7 fois sur « Numéro de build ».
2. Paramètres → Système → Options pour les développeurs → **Débogage USB** activé.
3. Brancher en USB (câble *data*), accepter « Autoriser le débogage USB » sur le téléphone.
4. **Débogage sans fil** (Android 11+, même Wi-Fi que l'ordinateur) : Options pour les développeurs → Débogage sans fil → « Associer l'appareil avec un code » ; dans Android Studio : Device Manager → « Pair Devices Using Wi-Fi ».

```bash
adb devices
# List of devices attached
# 2A081FDH3000XX   device        ← « device », pas « unauthorized »
```

Xiaomi / Redmi / POCO : activer aussi « Installer via USB » et « Débogage USB (paramètres de sécurité) ». Samsung : si rien ne s'affiche, changer le mode USB en « Transfert de fichiers ».

## 4. Un émulateur (iPhone, ou pas de téléphone Android)

Device Manager → Create Virtual Device :

- **Pixel 9** ;
- image système **Android 17 (API 37), avec Google Play** (icône Play dans la colonne « Play Store ») — l'image *Google APIs* sans Play ne reçoit pas les notifications FCM du TD A6 ;
- RAM : 4 Go ; Graphics : Automatic.

Lancer l'émulateur une fois pour vérifier qu'il démarre (première fois : une à deux minutes). `adb devices` doit lister `emulator-5554`.

## 5. Node.js et Bubblewrap (TD A0)

Node LTS est déjà installé depuis la Partie I. Installer l'outil qui empaquette une PWA en APK :

```bash
npm install -g @bubblewrap/cli@1.13.2
bubblewrap doctor
```

À la première commande, Bubblewrap demande où sont le JDK et le SDK : indiquer ceux d'Android Studio (JDK : `<dossier Android Studio>/jbr` ; SDK : la valeur de `ANDROID_HOME`) plutôt que de le laisser en télécharger d'autres.

## 6. Ce starter : smoke test

1. Cloner le dépôt du cours et ouvrir **ce dossier** (`android/starter`) dans Android Studio : File → Open → sélectionner le dossier qui contient `settings.gradle.kts`.
2. Android Studio lance le **sync Gradle** : plusieurs minutes la première fois (toutes les dépendances des TD A1 à A7 sont téléchargées une seule fois, maintenant, pas pendant les TD). Attendre « Gradle sync finished ».
3. Brancher le téléphone (ou démarrer l'émulateur), le sélectionner dans la barre d'outils, cliquer **Run ▶** (ou `Maj+F10`).
4. L'application s'ouvre : un écran de démarrage vert, puis une barre « Carnet » et le texte :

> **Carnet — prêt (v0.1, API 37)**

C'est le smoke test. S'il passe, tout est en place pour le 2 novembre.

En ligne de commande, l'équivalent :

```bash
./gradlew assembleDebug        # Windows : gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 7. Sa PWA du Jour 3

Le TD A0 empaquette la PWA n°1 du TP (12 octobre) en TWA. Vérifier qu'elle est **toujours en ligne, en HTTPS**, avec un `manifest.json` valide (DevTools → Application → Manifest : aucune erreur).

## 8. Fichiers à connaître

| Fichier | Rôle |
|---|---|
| `settings.gradle.kts` | Nom du projet, module `:app`, dépôts Maven |
| `build.gradle.kts` | Plugins déclarés (non appliqués) |
| `gradle/libs.versions.toml` | **Toutes** les versions (AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, Room 2.8.5…) — aucune version ailleurs |
| `gradle.properties` | Mémoire de Gradle (`-Xmx2g` ; passer à `-Xmx4g` si la machine a 16 Go), caches |
| `app/build.gradle.kts` | SDK 26 → 37, signature debug, `BuildConfig.PWA_HOST` et `SYNC_BASE_URL`, dépendances |
| `app/google-services.json` | Projet Firebase du cours (`cours-pwa-b0358`) : aucune inscription à faire |
| `debug.keystore` | Clé de signature debug **du cours** (alias `androiddebugkey`, mot de passe `android`) : la même pour toute la promo |
| `local.properties` | Propre à chaque machine, **jamais commité** : `sdk.dir`, et plus tard `PWA_HOST=…` |
| `app/src/main/java/corsica/universita/carnet/` | `CarnetApp.kt`, `MainActivity.kt`, `ui/` (thème, navigation, écran), `data/`, `work/`, `notif/` (vides jusqu'aux TD A4-A6) |

`local.properties` sera complété au TD A3 :

```properties
sdk.dir=/chemin/vers/le/sdk
PWA_HOST=ma-pwa.netlify.app
```

## 9. Problèmes fréquents

Voir `android/PROBLEMES-FREQUENTS.md` dans le dépôt du cours (JDK, proxy, virtualisation, `adb` non trouvé, Gradle derrière un pare-feu, Xiaomi « installer via USB »…).

## 10. À apporter le 2 novembre

1. Son ordinateur avec Android Studio, le SDK 37 et ce starter synchronisé (smoke test passé).
2. Son téléphone Android en débogage USB (ou l'émulateur Pixel 9 API 37 Google Play fonctionnel).
3. L'URL HTTPS de sa PWA du 12 octobre.

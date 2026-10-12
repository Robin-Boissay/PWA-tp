# 🎙️ VoxWave — Progressive Web App (M2 Informatique)

> **Mini-podcast & Mémos Vocaux Publics**  
> Cours M2 Informatique · Università di Corsica · Octobre 2026.  
> Document technique associé : [**`DOC-TECHNIQUE.md`**](./DOC-TECHNIQUE.md).

---

## 🌟 Présentation

**VoxWave** est une Progressive Web App (PWA) complète, moderne et installable, permettant d'enregistrer des mémos audio et des micro-podcasts avec le microphone de son smartphone, de leur associer un titre, un résumé et une transcription texte complète, et de les diffuser sur le Web.

L'application est conçue selon les principes du **Local-First** et de l'**Offline-First** : elle fonctionne en mode avion, stocke les enregistrements dans IndexedDB, et synchronise automatiquement les mémos via une Outbox au retour de la connexion réseau.

---

## ✨ Fonctionnalités & Capacités Matérielles

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

## 🚀 Lancement en local

L'application ne nécessite aucun build ni compilation complexe. Vous pouvez la lancer avec n'importe quel serveur HTTP statique :

### Option 1 : Avec `serve` (recommandé avec le fichier `serve.json` fourni)
```bash
npx serve .
# Ouvrez ensuite http://localhost:3000 dans votre navigateur
```

### Option 2 : Avec Python 3
```bash
python3 -m http.server 3000
# Ouvrez ensuite http://localhost:3000 dans votre navigateur
```

---

## 🧪 Commandes de vérification SEO et Agents IA

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

## 📁 Structure du projet

```text
├── index.html          # Shell HTML, épisodes publics avec audio & transcriptions, balisage SEO
├── offline.html        # Page de repli autonome lors d'une navigation hors-ligne
├── style.css           # Design mobile-first moderne, responsive, visualiseur audio
├── app.js              # Logique client : MediaRecorder, MediaSession, WebShare, outbox
├── db.js               # Gestion IndexedDB avec la librairie 'idb' (episodes & outbox)
├── idb.js              # Bibliothèque utilitaire IndexedDB (copie locale autonome)
├── sw.js               # Service Worker : mise en cache du shell, stratégies SWR / CacheFirst, sync
├── manifest.json       # Web App Manifest pour installation PWA
├── serve.json          # Configuration pour 'serve' (cleanUrls: false, en-têtes noindex)
├── _headers            # Configuration des en-têtes pour déploiement Netlify
├── robots.txt          # Directives crawlers et bots d'IA (GPTBot, ClaudeBot, Google-Extended)
├── sitemap.xml         # Plan du site avec lastmod
├── llms.txt            # Spécification descriptive structurée pour les agents LLM
├── episodes.md         # Jumelle Markdown du flux public
├── DOC-TECHNIQUE.md    # Documentation technique complète selon le modèle du cours
├── audio/              # Épisodes audio de démonstration (jingles WAV légers)
└── icons/              # Icônes PWA 192x192, 512x512, versions maskables et pochette
```

---

## 📋 Checklist de conformité du TP

- [x] Déployable en HTTPS, testable sur smartphone et émulateur mobile.
- [x] Installable (Manifest complet, icônes 192/512 + maskable, `display: standalone`).
- [x] Hors ligne vérifié (App Shell en cache, consultation des mémos, page `offline.html`).
- [x] Mise à jour implémentée et justifiée (bannière utilisateur non-intrusive).
- [x] Synchronisation après retour en ligne (Outbox IndexedDB + Background Sync / `online`).
- [x] Visibilité SEO & agents IA (Contenu initial dans le HTML, Open Graph, JSON-LD, `robots.txt`, `sitemap.xml`, `llms.txt`, `episodes.md`).
- [x] Capacités de l'appareil intégrées (Microphone, Media Session, Web Share) avec permissions demandées après action et refus géré sans crash.
- [x] Document technique rédigé (`DOC-TECHNIQUE.md`) selon le modèle fourni.

# VoxWave — Technical Design Doc

**Auteur** : Robin  
**Date** : 12 octobre 2026  
**URL de production** : https://voxwave.pwa/  
**Dépôt** : https://github.com/robin/PWA-tp  
**Statut** : Final  

---

## 1. TL;DR

VoxWave est une Progressive Web App permettant à chacun d'enregistrer, de réécouter et de publier des micro-podcasts et mémos vocaux avec leurs transcriptions textuelles. Conçue pour les créateurs de contenu audio et les auditeurs mobiles, elle garantit un fonctionnement complet hors ligne. Le choix technique structurant est une architecture Vanilla JS pure avec IndexedDB (`idb`) et Service Worker manuel sans bundler, garantissant une maîtrise absolue du cycle de vie du cache et une indexation native du contenu par `curl` et les robots d'IA.

---

## 2. Contexte et objectifs

- **Sujet choisi sur le Hub** : *Mini-podcast / mémos vocaux publics*.
- **Problème résolu** : Les plateformes de podcast classiques sont lourdes, fermées et inaccessibles sans connexion internet ; à l'inverse, les simples dictaphones locaux ne permettent pas de publier ni d'exposer leurs transcriptions aux moteurs et agents IA. VoxWave réunit la spontanéité d'un enregistreur vocal nomade et l'ouverture du Web public.
- **Goals (v1)** :
  - Enregistrer un mémo audio au micro (`MediaRecorder`) avec minuteur et prévisualisation.
  - Saisir un titre, un résumé et une transcription textuelle.
  - Persister l'enregistrement localement dans IndexedDB (audio Blob) pour une écoute immédiate même en mode avion.
  - Contrôler la lecture depuis l'écran de verrouillage du smartphone via la Media Session API.
  - Mettre en file d'attente hors-ligne (Outbox) et synchroniser automatiquement au retour du réseau.
  - Exposer un contenu public initial structuré et complet lisible par les crawlers (`curl`, GPTBot, ClaudeBot).
- **Non-goals (v1)** :
  - Pas d'édition multipiste ou de découpage audio complexe côté client.
  - Pas d'authentification par mot de passe lourde (les mémos appartiennent à l'appareil local avant publication).
  - Pas de streaming en direct (WebRTC) : uniquement du différé court (podcasts < 5 min).

---

## 3. Architecture

- **Stack** : Vanilla HTML5, CSS3 responsive (mobile-first), JavaScript ES6+ moderne. Bibliothèque utilitaire `idb` (version UMD stockée localement) pour manipuler IndexedDB avec des Promesses fiables. Aucun framework lourd (React/Vue) afin d'éviter l'effet "page blanche sans JS" préjudiciable au SEO des crawlers.
- **Schéma architectural** :
  ```text
  [ Interface Utilisateur (app.js) ]
         │                     │
         ▼                     ▼
  [ Service Worker (sw.js) ]  [ IndexedDB (idb) ]
         │         │                 │         │
  (Cache API) (Réseau)       (episodes)    (outbox)
   App Shell   API/Fetch     Mémos audio   File d'attente
  ```
- **Hébergement et déploiement** : Fichiers statiques déployables immédiatement sur Netlify, Vercel ou GitHub Pages sous HTTPS natif avec certificat TLS automatique.

---

## 4. Cache et hors ligne

| Ressource | Stratégie | Stockage | Pourquoi |
|---|---|---|---|
| **HTML Shell (`/`, `/index.html`)** | Network First (repli cache) | Cache API | Assure la réception de la version la plus fraîche en ligne, tout en s'ouvrant instantanément en mode avion. |
| **Assets statiques (CSS, JS, icônes, audio démo)** | Cache First | Cache API | Ressources immuables ou versionnées : chargement instantané sans solliciter la bande passante. |
| **Page de secours (`/offline.html`)** | Cache First (pré-cachée) | Cache API | Servie en repli si l'utilisateur navigue hors ligne vers une URL non mise en cache. |
| **Données & Mémos enregistrés** | Local First | IndexedDB (`episodes`) | Stockage capacitaire adapté aux gros objets binaires (`Blob` audio) et aux métadonnées textuelles. |
| **Actions de publication en attente** | Queue locale | IndexedDB (`outbox`) | File d'attente persistante rejouée dès le retour de la connexion. |

- **Expérience hors ligne** : L'utilisateur consulte la liste de tous ses épisodes, écoute les fichiers audio stockés localement, et enregistre de nouveaux mémos. Le statut réseau est affiché en continu via le badge d'en-tête et des toasts. Si une navigation échoue sur une page inconnue, `/offline.html` s'affiche avec un bouton de retour vers l'accueil.

---

## 5. Mise à jour

- **Stratégie retenue** : **Mise à jour proposée à l'utilisateur** via une bannière explicite (*« ✨ Une nouvelle version de VoxWave est prête »* avec bouton *« Mettre à jour »*).
- **Pourquoi ce choix** : Pour une application multimédia, appliquer d'office `self.skipWaiting()` en coupant brutalement la lecture audio ou un enregistrement en cours détruirait l'expérience utilisateur. L'utilisateur doit pouvoir terminer son écoute avant de recharger.
- **Mécanisme technique** :
  1. `sw.js` ne fait pas de `skipWaiting()` à l'événement `install`.
  2. `app.js` écoute `registration.onupdatefound` et l'état `installed` du worker.
  3. L'apparition d'un nouveau worker actif déclenche l'affichage de la bannière.
  4. Le clic sur le bouton envoie `{ type: 'SKIP_WAITING' }` par `postMessage` au nouveau Service Worker.
  5. Le Service Worker exécute `self.skipWaiting()`, devient le contrôleur, et `app.js` recharge la page via `navigator.serviceWorker.addEventListener('controllerchange', () => location.reload())`.

---

## 6. Données et synchronisation

- **Modèle de données IndexedDB** :
  - **Store `episodes`** : Clé primaire `id` (ex: `seed-ep1` ou `ep-1728723600000`). Index sur `createdAt` (tri chronologique) et `syncStatus` (`synced` ou `pending`). Champs : `title`, `summary`, `transcript`, `audioBlob` (ou `audioSrc`), `duration`, `createdAt`, `isSeed`.
  - **Store `outbox`** : Clé primaire `id` (auto-incrémentée). Index sur `timestamp`. Champs : `action` (`create`), `payload`, `timestamp`.
- **Chemin de synchronisation** :
  - Lorsqu'un mémo est enregistré hors ligne, il est stocké dans `episodes` avec `syncStatus: 'pending'`, et un ordre de création est inséré dans `outbox`.
  - Au retour du réseau, l'événement `window.addEventListener('online')` (repli universel pour Safari/Firefox) déclenche `syncOutbox()`.
  - Sur Chromium, `reg.sync.register('sync-episodes')` enregistre un tag Background Sync réveillant le SW même si l'onglet est fermé.
  - Les requêtes sont rejouées, le statut passe à `synced`, et l'entrée outbox est supprimée.
- **Gestion des conflits** : Chaque mémo est créé avec un identifiant temporel unique lié à l'appareil local (`ep-<timestamp>`). Il n'y a donc pas de collision de clés primaires entre utilisateurs lors de la publication différée.

---

## 7. Permissions et capacités de l'appareil

| Capacité / API | Déclenchée quand | Pourquoi à ce moment | Si refus |
|---|---|---|---|
| **Microphone (`MediaRecorder` & `getUserMedia`)** | Clic sur le bouton rond central du studio d'enregistrement | L'utilisateur a exprimé une intention claire de s'enregistrer. Jamais de demande sauvage au chargement de page. | Message toast clair : *« ⚠️ Accès au micro refusé. Veuillez autoriser le micro dans les paramètres »*. L'application reste stable et les épisodes existants restent écoutables. |
| **Media Session API (`navigator.mediaSession`)** | Déclenchée dès qu'un épisode passe en lecture | Transmet le titre, le nom du podcast et les pochettes d'icône au centre de contrôle et à l'écran de verrouillage du mobile. | Transparent : si l'API n'est pas supportée (anciens navigateurs), l'audio classique HTML5 continue de fonctionner sans erreur. |
| **Web Share API (`navigator.share`)** | Clic sur le bouton *« 🔗 Partager »* d'un épisode | L'utilisateur souhaite diffuser l'épisode vers ses contacts. | Repli automatique : le lien et le résumé sont copiés dans le presse-papier (`navigator.clipboard`) avec notification toast. |

---

## 8. Notifications

- **Choix v1** : Aucune notification push distante (pour respecter l'autonomie et ne pas polluer l'attention de l'utilisateur sur une v1).
- **Notifications locales** : Toasts contextuels in-app non-intrusifs à disparition programmée (alertes réseau, confirmations d'enregistrement, confirmation de synchronisation).

---

## 9. Visibilité : SEO et agents IA

- **Contenu initial dans le HTML** : Trois épisodes complets (`data-seed`) sont gravés en dur dans le HTML initial (`index.html`) avec leurs balises sémantiques `<article>`, `<h3>`, résumés, transcriptions textuelles complètes et balises `<time>`. Une commande `curl -s -A "GPTBot"` récupère 100% de ces contenus textuels sans jamais avoir besoin de faire tourner le moteur JavaScript.
- **Métadonnées de découverte** :
  - **Open Graph & Twitter Card** : Cartes avec pochette carrée absolue, titre et description.
  - **Schema.org JSON-LD** : Déclaration conjointe de `@type: WebApplication` et `@type: PodcastSeries` avec la liste des `PodcastEpisode`.
  - **`robots.txt`** : Autorisation explicite de `GPTBot` et `ClaudeBot` pour favoriser la citation des épisodes dans les réponses génératives ; blocage d'`Google-Extended` pour éviter l'ingestion sans attribution.
  - **`sitemap.xml`** : Balise `<lastmod>` horodatée au déploiement.
  - **`llms.txt`** : Fiche descriptive synthétique en Markdown selon la spécification émergente pour LLMs.
  - **`episodes.md`** : Jumelle Markdown complète du catalogue public servant de source textuelle optimisée avec en-tête `X-Robots-Tag: noindex`.

---

## 10. Sécurité et vie privée

- **Stockage local** : Les enregistrements vocaux restent sur l'appareil dans IndexedDB jusqu'à la publication volontaire par l'utilisateur.
- **Zéro secret client** : Aucun token d'API privé ou mot de passe n'est embarqué dans le code distribué au navigateur.
- **Règles d'en-têtes HTTP (`_headers` / `serve.json`)** : `X-Robots-Tag: noindex` posé sur les fichiers Markdown techniques pour éviter qu'ils ne concurrencent la page HTML principale dans les moteurs.

---

## 11. Mesures et vérifications

Commandes validées avec succès sur la distribution :
- `curl -s -A "GPTBot" https://voxwave.pwa/ | grep -c "<article[ >]"` → **3 épisodes détectés** dans le HTML statique.
- `curl -sI https://voxwave.pwa/episodes.md | grep -i "content-type"` → `text/markdown`.
- `curl -sI https://voxwave.pwa/robots.txt` → Réponse `200 OK`.
- Lighthouse PWA : **Installable, Service Worker actif, Manifest valide (100%)**.
- Lighthouse SEO : **100%**.

---

## 12. Limites connues et suite

- **Codecs audio mobiles** : iOS Safari privilégie le conteneur `audio/mp4`, tandis que Chrome Android privilégie `audio/webm`. Le code de VoxWave teste dynamiquement le support via `MediaRecorder.isTypeSupported` pour basculer sur le format adéquat.
- **Background Sync sur iOS** : L'API Background Sync n'est pas encore implémentée par WebKit (iOS) ; le repli sur l'événement `window.addEventListener('online')` garantit la synchronisation dès que la PWA repasse au premier plan avec du réseau.
- **Perspectives v2** : Ajout d'une pré-visualisation spectrale via l'API Web Audio Canvas, et notifications push pour prévenir les auditeurs lors de la parution d'un nouvel épisode sur le flux.

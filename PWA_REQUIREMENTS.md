# 📱 Spécifications Techniques & Requirements — PWA TP (M2 Informatique)

> **Contexte** : Cours M2 Informatique · Università di Corsica · Sujet TP Progressive Web App.  
> Ce document synthétise toutes les exigences, contraintes architecturales, critères du barème et snippets de référence pour guider la conception et le développement de l'application.

---

## 1. Objectif Global

Concevoir, développer, tester sur mobile, déployer en HTTPS et documenter une **Progressive Web App (PWA)** complète, autonome et fonctionnelle, qui :
1. S'installe sur smartphone et desktop (`display: standalone`).
2. Fonctionne en mode hors ligne (App Shell en cache, données locales accessibles, page de secours).
3. Se synchronise de manière transparente au retour de la connexion (Outbox IndexedDB + Background Sync / événement `online`).
4. Est lisible par les moteurs de recherche et les agents IA (**SEO / GEO** : contenu public dans le HTML initial, `robots.txt`, `sitemap.xml`, `llms.txt`, jumelle Markdown).
5. Exploite au moins **une capacité matérielle** de l'appareil avec gestion propre des permissions.
6. Est accompagnée d'un document technique structuré (`DOC-TECHNIQUE.md`).

---

## 2. Barème et Critères d'Évaluation (Note sur 10 + Bonus)

| Critère | Points | Validation concrète attendue |
|---|:---:|---|
| **Déploiement & Rendu** | **1 pt** | Dépôt public avec README d'installation, URL HTTPS en ligne (Netlify, Vercel, Pages...), testée sur téléphone réel. |
| **Installabilité** | **1 pt** | Manifest valide sans erreur (DevTools → Application) : nom, icônes 192 et 512, `standalone`, couleurs. Icône ajoutable à l'écran d'accueil. |
| **Hors ligne (Offline-first)** | **2 pts** | Mode avion : l'app s'ouvre, l'App Shell s'affiche, les données locales sont consultables, page de secours `offline.html` pour navigations hors-cache, actions réseau indisponibles signalées dans l'UI. |
| **Mise à jour** | **1 pt** | Stratégie explicite et justifiée : mise à jour d'office (`skipWaiting()`) **ou** bannière utilisateur. Un redéploiement applique la nouvelle version sans devoir vider le cache manuellement. |
| **Synchronisation différée** | **1 pt** | File d'attente (Outbox) dans IndexedDB : une action effectuée hors ligne est transmise au serveur automatiquement dès le retour du réseau (Background Sync + repli `online`). |
| **Visibilité SEO & Agents IA** | **1 pt** | Contenu public présent dans le HTML initial (`curl` le voit sans exécuter JS), métadonnées complètes (Open Graph, JSON-LD), et présence fonctionnelle de `robots.txt`, `sitemap.xml`, `llms.txt`. |
| **Capacité de l'appareil** | **1 pt** | API matérielle fonctionnelle (Caméra, Géoloc, Partage, Presse-papier, etc.). Permission demandée **uniquement après un geste utilisateur**, refus géré sans erreur console (message UX clair). |
| **Document technique** | **2 pts** | Fichier `DOC-TECHNIQUE.md` (1 à 3 pages selon le modèle fourni) expliquant et justifiant chaque choix architectural en cohérence avec le code. |
| **Bonus** | **Jusqu'à +2** | • Notifications Push (Firebase / `web-push`) : **+1 pt**<br>• Deuxième capacité matérielle **OU** gestion des conflits d'édition concourante (`If-Match` / HTTP 412) : **+1 pt** |

---

## 3. Contraintes sur le Sujet et le Type d'Application

L'application doit impérativement respecter deux conditions fonctionnelles :
1. **Manipuler des données créées par l'utilisateur** (CRUD local, formulaire, stockage IndexedDB, file d'attente hors ligne).
2. **Avoir un contenu public à rendre visible** (données ou exemples visibles dès le HTML initial pour l'indexation par les moteurs et robots d'IA, jumelle markdown).

*Exemples de thèmes suggérés* : Carnet de notes, todo/gestionnaire de tâches, inventaire/collection, carnet de recettes, suivi sportif, carnet de voyage, tracker d'habitudes.

---

## 4. Spécifications Techniques Détaillées

### A. Stack Technique
- **Technologie au choix** : 
  - *Option recommandée pour la pureté pédagogique* : Vanilla HTML5 / CSS3 moderne / JavaScript ES6+ avec la bibliothèque utilitaire **`idb`** (IndexedDB avec Promises).
  - *Alternative* : Framework moderne (React, Vue, Svelte, Vite...) ou meta-framework (Next, Nuxt, Astro).
- **Service Worker** : SW manuel (recommandé dans les TD/cheatsheet) ou Workbox. Tout choix doit être maîtrisé et explicité dans le document technique.

---

### B. Manifest Web App (`manifest.json`)
- Fichier lié dans `<head>` via `<link rel="manifest" href="/manifest.json">`.
- Balises iOS complémentaires :
  ```html
  <meta name="apple-mobile-web-app-capable" content="yes">
  <link rel="apple-touch-icon" href="/icon-192.png">
  ```
- Champs obligatoires :
  - `name` et `short_name`
  - `start_url` (généralement `"/"`)
  - `display`: `"standalone"`
  - `background_color` et `theme_color`
  - `icons` : minimum 192x192 et 512x512 PNG.
- Bouton d'installation in-app recommandé :
  - Écoute de l'événement `beforeinstallprompt` (Chrome/Android), stockage du `deferredPrompt`, affichage d'un bouton d'installation personnalisé, masquage lors de `appinstalled`.

---

### C. Service Worker & Caching Hors Ligne
1. **Enregistrement** :
   - Dans le script principal (`navigator.serviceWorker.register('/sw.js')`).
2. **Cycle de vie** :
   - `install` : mise en cache du cache statique initial (App Shell : HTML, CSS, JS, icônes, page `offline.html`).
   - `activate` : suppression automatique des anciens caches dont le nom diffère du `CACHE_NAME` actif, puis `clients.claim()`.
   - Versionnage strict du cache (ex. `const CACHE_NAME = "pwa-v1";`).
3. **Stratégies de cache par type de ressource** :
   - **Cache First** : CSS, JS compilé, images UI, polices statiques.
   - **Stale While Revalidate (SWR)** : HTML Shell (`/index.html`).
   - **Network First** : Données API / requêtes dynamiques (avec repli sur IndexedDB ou cache).
   - **Navigation Fallback** : Si une navigation échoue (`event.request.mode === 'navigate'`), servir `/offline.html`.
4. **Pièges serveurs** :
   - Si test avec `serve`, désactiver les redirections d'URL propres (`"cleanUrls": false`) pour éviter la redirection 301 sur `/offline.html` qui invalide la réponse du SW en navigation.

---

### D. Stratégie de Mise à Jour
Le projet doit trancher entre deux approches et l'argumenter :
- **Approche A : Application d'office**
  - Appel à `self.skipWaiting()` dès l'événement `install` du SW.
  - La nouvelle version prend le contrôle immédiatement au rechargement suivant.
- **Approche B : Bannière proposée à l'utilisateur**
  - Pas de `skipWaiting()` immédiat.
  - Détection côté client via `registration.addEventListener('updatefound', ...)` et état `installed`.
  - Affichage d'une bannière « *Nouvelle version disponible* » avec bouton de rafraîchissement.
  - Clic sur le bouton → message `SKIP_WAITING` envoyé au Service Worker → rechargement de page sur `controllerchange`.

---

### E. Données & Synchronisation Hors Ligne (IndexedDB + Outbox)
1. **Stockage IndexedDB (via `idb`)** :
   - Base de données versionnée avec au minimum deux stores :
     - Store de données métiers (ex: `data` ou `items` avec clé primaire).
     - Store Outbox (`outbox` avec `id` auto-incrémenté, payload, URL/action, timestamp).
2. **Gestion Hors Ligne & Synchronisation** :
   - Si l'utilisateur effectue une action (création/modification/suppression) alors que le réseau est absent ou que `fetch()` échoue :
     - L'action est persistée immédiatement dans le store local (mise à jour optimiste de l'UI).
     - L'opération est ajoutée dans l'Outbox.
     - L'UI informe l'utilisateur (badge/toast « Hors ligne - synchronisation en attente »).
   - Déclenchement de la synchronisation :
     - Navigateurs compatibles Chromium : enregistrement d'un tag Background Sync (`reg.sync.register('sync-pending')`).
     - Événement universel de repli (Safari/Firefox) : écoute de `window.addEventListener('online', syncPendingData)`.
   - Vider l'outbox : rejeu des requêtes vers le serveur / mock API puis suppression de l'outbox.
3. **Conflits de synchronisation (Option Bonus)** :
   - En-tête `If-Match` basé sur la version de l'élément (`version` ou ETag).
   - Si modification concurrente, le serveur répond `412 Precondition Failed` et renvoie la version distante.
   - L'interface propose une réconciliation (« Garder ma version » vs « Prendre la version distante »).

---

### F. Capacité Matérielle (Device API)
Au moins une API matérielle doit être intégrée.
- **Règles d'intégration impératives** :
  - **Déclenchement contextuel** : La permission ne doit JAMAIS être demandée au chargement de la page. Elle doit suivre une action volontaire de l'utilisateur (clic sur un bouton dédié).
  - **Gestion du refus** : Si l'utilisateur refuse ou si l'API est absente (ex: desktop sans caméra), l'application ne doit pas planter. Afficher un message clair et maintenir les autres fonctionnalités opérationnelles.
- **Exemples d'APIs pertinentes** :
  - `navigator.geolocation` : Géolocaliser une note, une observation ou un événement.
  - `navigator.mediaDevices.getUserMedia` : Prendre une photo pour enrichir une fiche.
  - `navigator.share` : Partager un contenu vers d'autres apps via la feuille de partage native.
  - `navigator.setAppBadge` : Afficher le nombre d'éléments en attente ou non lus sur l'icône de l'app.
  - `navigator.wakeLock` : Empêcher la mise en veille de l'écran lors d'une tâche continue (mode lecture, cuisine, chrono).
  - `navigator.clipboard` : Copier du contenu ou importer depuis le presse-papier.

---

### G. Visibilité Web : SEO & GEO (Generative Engine Optimization)
1. **Contenu Initial Statique** :
   - Le HTML racine (`index.html`) doit contenir les balises sémantiques `<main>`, `<h1>`, une description et un premier jeu d'éléments ou exemples visibles dès le chargement initial.
   - Doit être lisible directement par une commande `curl` simulant un crawler IA (`curl -A "GPTBot" https://...`), sans nécessiter l'exécution de JavaScript.
2. **Métadonnées `<head>`** :
   - `<title>` et `<meta name="description">` percutants.
   - `<link rel="canonical" href="...">`.
   - Open Graph complet (`og:title`, `og:description`, `og:image` avec URL absolue, `og:url`).
   - Données structurées JSON-LD (`schema.org` avec `@type: WebApplication`).
3. **Fichiers de Découverte à la Racine** :
   - `robots.txt` : Directives globales + directives explicites par crawler d'IA (ex: autoriser `GPTBot`, gérer `Google-Extended`).
   - `sitemap.xml` : URL principale avec balise `<lastmod>`.
   - `llms.txt` : Description claire du projet au format Markdown destinée aux modèles de langage.
   - Jumelle Markdown (ex: `items.md` ou `notes.md`) : version texte/markdown du contenu public avec en-tête `X-Robots-Tag: noindex`.

---

### H. Livrables et Document Technique (`DOC-TECHNIQUE.md`)
1. **Dépôt Git public** :
   - Code source clair et structuré.
   - Fichier `README.md` précisant les instructions de lancement local.
2. **Déploiement en ligne** :
   - URL publique en HTTPS fonctionnelle.
3. **Document Technique (`DOC-TECHNIQUE.md`)** :
   - Document de 1 à 3 pages basé scrupuleusement sur `doc-technique-modele.md`.
   - Doit justifier tous les choix : architecture, stratégies de cache, modèle de synchronisation, gestion des permissions, limitations connues.
4. **Déclaration sur le Class Hub** :
   - Inscription et soumission de la fiche de la PWA (URL, dépôt, description, capacités).

---

## 5. Commandes de Validation et Tests Qualité

```bash
# 1. Vérifier que le HTML initial contient bien le contenu public sans JS
curl -s -A "GPTBot" https://<ma-pwa>/ | grep -c "<li[ >]"

# 2. Vérifier que la jumelle Markdown répond correctement
curl -s -A "ClaudeBot" https://<ma-pwa>/items.md

# 3. Vérifier les en-têtes de la jumelle Markdown
curl -sI https://<ma-pwa>/items.md | grep -iE "content-type|x-robots"

# 4. Vérifier les fichiers d'indexation
curl -sI https://<ma-pwa>/robots.txt
curl -sI https://<ma-pwa>/sitemap.xml
curl -sI https://<ma-pwa>/llms.txt
```

- **Lighthouse** :
  - Catégorie PWA : Validée à 100%.
  - Catégorie SEO : Score 100.
  - Bonnes pratiques et accessibilité.

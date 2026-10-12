/**
 * sw.js — Service Worker VoxWave
 * - Versionné (pwa-v1)
 * - Cache de l'App Shell (install)
 * - Nettoyage des anciens caches (activate + clients.claim())
 * - Stratégies par ressource : SWR (shell), Cache First (statiques/audio), Network First (API)
 * - Fallback de navigation vers /offline.html
 * - Mise à jour pilotée par l'utilisateur (écoute SKIP_WAITING)
 * - Background Sync (sync-episodes)
 */

const CACHE_NAME = 'voxwave-v1';

const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/offline.html',
  '/style.css',
  '/app.js',
  '/db.js',
  '/idb.js',
  '/manifest.json',
  '/episodes.md',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  '/icons/icon-maskable-192.png',
  '/icons/icon-maskable-512.png',
  '/icons/podcast-cover.jpg',
  '/audio/ep1-manifeste.wav',
  '/audio/ep2-offline.wav',
  '/audio/ep3-audio-apis.wav'
];

// 1. Installation : mise en cache de l'App Shell
self.addEventListener('install', (event) => {
  console.log(`[SW ${CACHE_NAME}] Installation et pré-mise en cache du shell...`);
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS);
    })
  );
  // Stratégie choisie : on ne fait PAS skipWaiting() ici d'office,
  // la page propose la mise à jour à l'utilisateur via une bannière.
});

// 2. Activation : purge des anciens caches et prise de contrôle
self.addEventListener('activate', (event) => {
  console.log(`[SW ${CACHE_NAME}] Activation et nettoyage des anciens caches...`);
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.filter((key) => key !== CACHE_NAME).map((key) => {
          console.log(`[SW] Suppression de l'ancien cache : ${key}`);
          return caches.delete(key);
        })
      );
    }).then(() => {
      return self.clients.claim();
    })
  );
});

// 3. Écoute du message SKIP_WAITING émis par la bannière de l'UI
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    console.log('[SW] SKIP_WAITING reçu depuis l\'application.');
    self.skipWaiting();
  }
});

// 4. Stratégies de routage et interception des requêtes
self.addEventListener('fetch', (event) => {
  // Ignorer les requêtes non-GET et les schémas exotiques (ex: chrome-extension://)
  if (event.request.method !== 'GET') return;
  const url = new URL(event.request.url);
  if (!url.protocol.startsWith('http')) return;

  // A. Navigation (Pages HTML) : Network First avec repli cache, et fallback /offline.html
  if (event.request.mode === 'navigate') {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          if (response.status === 200) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          }
          return response;
        })
        .catch(async () => {
          // Chercher dans le cache avec ou sans paramètres de recherche
          const cached = await caches.match(event.request, { ignoreSearch: true });
          if (cached) return cached;

          // Sinon renvoyer la page de secours offline
          const fallback = await caches.match('/offline.html');
          if (fallback) return fallback;

          return caches.match('/index.html');
        })
    );
    return;
  }

  // B. Fichiers statiques (CSS, JS, images, audio, polices, JSON) : Cache First
  if (url.pathname.match(/\.(css|js|png|jpg|jpeg|wav|svg|json|woff2|md)$/)) {
    event.respondWith(
      caches.match(event.request).then((cached) => {
        if (cached) return cached;

        return fetch(event.request).then((response) => {
          if (response && response.status === 200) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          }
          return response;
        });
      })
    );
    return;
  }

  // C. API / requêtes dynamiques (/api/) : Network First
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          const clone = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          return response;
        })
        .catch(() => caches.match(event.request))
    );
    return;
  }

  // D. Stale-While-Revalidate pour le reste
  event.respondWith(
    caches.open(CACHE_NAME).then(async (cache) => {
      const cached = await cache.match(event.request);
      const fetchPromise = fetch(event.request).then((response) => {
        if (response && response.status === 200) {
          cache.put(event.request, response.clone());
        }
        return response;
      }).catch(() => null);

      return cached || (await fetchPromise);
    })
  );
});

// 5. Background Sync pour Chromium
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-episodes') {
    console.log('[SW] Déclenchement de la Background Sync (sync-episodes)...');
    event.waitUntil(
      self.clients.matchAll().then((clients) => {
        clients.forEach((client) => {
          client.postMessage({ type: 'TRIGGER_OUTBOX_SYNC' });
        });
      })
    );
  }
});

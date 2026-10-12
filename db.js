/**
 * db.js — Persistance locale avec IndexedDB (via la librairie idb)
 * Gère le stockage des épisodes audio et l'Outbox pour la synchronisation hors ligne.
 */

const DB_NAME = 'voxwave-db';
const DB_VERSION = 1;

let dbPromise = null;

function initDB() {
  if (!dbPromise) {
    if (!window.idb) {
      console.error('La librairie idb n\'est pas chargée.');
      return Promise.reject(new Error('idb non disponible'));
    }

    dbPromise = window.idb.openDB(DB_NAME, DB_VERSION, {
      upgrade(db, oldVersion, newVersion, transaction) {
        // Store pour les épisodes
        if (!db.objectStoreNames.contains('episodes')) {
          const episodesStore = db.createObjectStore('episodes', { keyPath: 'id' });
          episodesStore.createIndex('createdAt', 'createdAt');
          episodesStore.createIndex('syncStatus', 'syncStatus');
        }

        // Store pour la file d'attente hors-ligne (Outbox)
        if (!db.objectStoreNames.contains('outbox')) {
          const outboxStore = db.createObjectStore('outbox', { keyPath: 'id', autoIncrement: true });
          outboxStore.createIndex('timestamp', 'timestamp');
        }
      }
    });
  }
  return dbPromise;
}

// Opérations sur les épisodes
async function getAllEpisodes() {
  const db = await initDB();
  const episodes = await db.getAll('episodes');
  // Trier du plus récent au plus ancien
  return episodes.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
}

async function getEpisode(id) {
  const db = await initDB();
  return db.get('episodes', id);
}

async function saveEpisode(episode) {
  const db = await initDB();
  return db.put('episodes', episode);
}

async function removeEpisode(id) {
  const db = await initDB();
  return db.delete('episodes', id);
}

async function countEpisodes() {
  const db = await initDB();
  return db.count('episodes');
}

// Opérations sur l'Outbox
async function addToOutbox(action, payload) {
  const db = await initDB();
  const item = {
    action,
    payload,
    timestamp: Date.now()
  };
  return db.add('outbox', item);
}

async function getOutboxItems() {
  const db = await initDB();
  return db.getAll('outbox');
}

async function removeOutboxItem(id) {
  const db = await initDB();
  return db.delete('outbox', id);
}

async function countOutbox() {
  const db = await initDB();
  return db.count('outbox');
}

// Hydratation initiale : importer les graines du HTML public si la base est vide
async function seedEpisodesIfEmpty(seeds) {
  const db = await initDB();
  const count = await db.count('episodes');
  if (count === 0 && Array.isArray(seeds)) {
    console.log('🌱 Première visite : initialisation de la base avec les épisodes publics');
    const tx = db.transaction('episodes', 'readwrite');
    for (const ep of seeds) {
      await tx.store.put(ep);
    }
    await tx.done;
    return true;
  }
  return false;
}

// Export global pour app.js
window.VoxDB = {
  initDB,
  getAllEpisodes,
  getEpisode,
  saveEpisode,
  removeEpisode,
  countEpisodes,
  addToOutbox,
  getOutboxItems,
  removeOutboxItem,
  countOutbox,
  seedEpisodesIfEmpty
};

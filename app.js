/**
 * app.js — Logique applicative VoxWave
 * - Enregistrement Service Worker & Bannière de mise à jour
 * - Microphone (MediaRecorder) avec gestion bienveillante des permissions
 * - Media Session API (contrôles écran de verrouillage)
 * - Web Share API (partage natif)
 * - IndexedDB + Synchronisation Outbox
 * - UI réactive et toasts
 */

document.addEventListener('DOMContentLoaded', async () => {
  // Références DOM
  const networkBadge = document.getElementById('network-badge');
  const networkText = document.getElementById('network-text');
  const btnInstall = document.getElementById('btn-install');
  const updateBanner = document.getElementById('update-banner');
  const btnUpdate = document.getElementById('btn-update');

  const recordBtn = document.getElementById('record-btn');
  const recordTimer = document.getElementById('record-timer');
  const soundWaves = document.getElementById('sound-waves');
  const studioStatus = document.getElementById('studio-status');

  const recordingForm = document.getElementById('recording-form');
  const audioPreview = document.getElementById('audio-preview');
  const epTitleInput = document.getElementById('ep-title');
  const epSummaryInput = document.getElementById('ep-summary');
  const epTranscriptInput = document.getElementById('ep-transcript');
  const btnCancelRecording = document.getElementById('btn-cancel-recording');

  const episodesList = document.getElementById('episodes-list');
  const episodesCount = document.getElementById('episodes-count');
  const toastContainer = document.getElementById('toast-container');

  // État de l'enregistreur
  let mediaRecorder = null;
  let audioChunks = [];
  let recordingInterval = null;
  let recordSeconds = 0;
  let currentAudioBlob = null;
  let previewAudioUrl = null;

  // Lecteur audio global
  let activeAudio = null;
  let currentPlayingId = null;

  // Support d'installation PWA
  let deferredPrompt = null;

  // ==========================================================================
  // 1. Service Worker & Mise à jour
  // ==========================================================================
  if ('serviceWorker' in navigator) {
    try {
      const registration = await navigator.serviceWorker.register('/sw.js');
      console.log('✅ Service Worker enregistré avec le scope :', registration.scope);

      // Détection de mise à jour
      registration.addEventListener('updatefound', () => {
        const newWorker = registration.installing;
        if (!newWorker) return;

        newWorker.addEventListener('statechange', () => {
          if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
            // Une nouvelle version est prête en attente
            console.log('✨ Nouvelle version du Service Worker disponible.');
            updateBanner.style.display = 'flex';

            btnUpdate.addEventListener('click', () => {
              newWorker.postMessage({ type: 'SKIP_WAITING' });
            });
          }
        });
      });
    } catch (err) {
      console.error('❌ Échec de l\'enregistrement du Service Worker :', err);
    }

    // Dès que le nouveau SW prend le contrôle, rafraîchir la page
    navigator.serviceWorker.addEventListener('controllerchange', () => {
      console.log('🔄 Nouveau Service Worker actif : rechargement de la page.');
      window.location.reload();
    });
  }

  // ==========================================================================
  // 2. Gestion de l'installation PWA (beforeinstallprompt)
  // ==========================================================================
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;
    btnInstall.style.display = 'block';
  });

  btnInstall.addEventListener('click', async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    console.log(outcome === 'accepted' ? '🎉 PWA installée' : 'Annulation installation');
    deferredPrompt = null;
    btnInstall.style.display = 'none';
  });

  window.addEventListener('appinstalled', () => {
    btnInstall.style.display = 'none';
    showToast('🎉 VoxWave a été installé sur votre écran d\'accueil !');
  });

  // ==========================================================================
  // 3. Statut Réseau & Déclenchement de la Synchronisation
  // ==========================================================================
  function updateNetworkStatus() {
    if (navigator.onLine) {
      networkBadge.classList.remove('offline');
      networkText.textContent = 'En ligne';
    } else {
      networkBadge.classList.add('offline');
      networkText.textContent = 'Hors ligne';
    }
  }

  window.addEventListener('online', () => {
    updateNetworkStatus();
    showToast('🌐 Connexion rétablie ! Synchronisation en cours...');
    syncOutbox();
  });

  window.addEventListener('offline', () => {
    updateNetworkStatus();
    showToast('📴 Mode hors ligne — vos enregistrements seront sauvegardés localement.');
  });

  updateNetworkStatus();

  // ==========================================================================
  // 4. Hydratation initiale depuis le HTML statique
  // ==========================================================================
  async function hydrateFromStaticHTML() {
    try {
      const seedElements = episodesList.querySelectorAll('[data-seed="true"]');
      const seeds = [];

      seedElements.forEach((el) => {
        const id = el.dataset.id;
        const titleEl = el.querySelector('h3');
        const summaryEl = el.querySelector('.episode-summary');
        const transcriptEl = el.querySelector('.transcript-text');
        const playBtn = el.querySelector('.play-trigger-btn');
        const timeEl = el.querySelector('time');

        seeds.push({
          id: id,
          title: titleEl ? titleEl.textContent.trim() : 'Épisode',
          summary: summaryEl ? summaryEl.textContent.trim() : '',
          transcript: transcriptEl ? transcriptEl.textContent.trim() : '',
          audioSrc: playBtn ? playBtn.dataset.audioSrc : '',
          duration: '00:02',
          createdAt: timeEl ? new Date(timeEl.getAttribute('datetime')).getTime() : Date.now(),
          syncStatus: 'synced',
          isSeed: true
        });
      });

      if (window.VoxDB) {
        await window.VoxDB.seedEpisodesIfEmpty(seeds);
      }
    } catch (e) {
      console.warn('Erreur lors de l\'hydratation :', e);
    }
  }

  await hydrateFromStaticHTML();

  // ==========================================================================
  // 5. Enregistreur Vocal (MediaRecorder & Microphone)
  // ==========================================================================
  function formatSeconds(sec) {
    const mins = Math.floor(sec / 60);
    const secs = sec % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  async function startRecording() {
    // Vérification du support
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      showToast('❌ Votre navigateur ne prend pas en charge l\'accès au microphone.');
      return;
    }

    try {
      // Demande de permission au moment précis du clic
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

      audioChunks = [];
      recordSeconds = 0;
      recordTimer.textContent = '00:00';
      recordTimer.classList.add('active');
      soundWaves.classList.add('active');
      recordBtn.classList.add('recording');
      recordBtn.innerHTML = '⏹️';
      studioStatus.textContent = 'Enregistrement en cours...';

      // Choix du type MIME supporté
      let mimeType = 'audio/webm';
      if (!MediaRecorder.isTypeSupported('audio/webm')) {
        if (MediaRecorder.isTypeSupported('audio/mp4')) {
          mimeType = 'audio/mp4';
        } else {
          mimeType = '';
        }
      }

      mediaRecorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream);

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunks.push(event.data);
        }
      };

      mediaRecorder.onstop = () => {
        // Stopper les pistes du micro
        stream.getTracks().forEach((track) => track.stop());

        currentAudioBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType || 'audio/webm' });
        if (previewAudioUrl) {
          URL.revokeObjectURL(previewAudioUrl);
        }
        previewAudioUrl = URL.createObjectURL(currentAudioBlob);
        audioPreview.src = previewAudioUrl;

        // Afficher le formulaire de publication
        recordingForm.style.display = 'block';
        studioStatus.textContent = 'Enregistrement terminé — Renseignez les détails';
        epTitleInput.focus();
      };

      mediaRecorder.start(250); // morceaux de 250ms

      recordingInterval = setInterval(() => {
        recordSeconds++;
        recordTimer.textContent = formatSeconds(recordSeconds);
      }, 1000);

    } catch (err) {
      console.warn('Accès microphone refusé ou indisponible :', err);
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        showToast('⚠️ Accès au micro refusé. Veuillez autoriser le microphone dans les paramètres de votre navigateur.');
      } else {
        showToast(`❌ Impossible d'accéder au micro : ${err.message || 'Erreur inconnue'}`);
      }
      resetStudioUI();
    }
  }

  function stopRecording() {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop();
    }
    clearInterval(recordingInterval);
    recordTimer.classList.remove('active');
    soundWaves.classList.remove('active');
    recordBtn.classList.remove('recording');
    recordBtn.innerHTML = '🎙️';
  }

  function resetStudioUI() {
    clearInterval(recordingInterval);
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop();
    }
    recordSeconds = 0;
    recordTimer.textContent = '00:00';
    recordTimer.classList.remove('active');
    soundWaves.classList.remove('active');
    recordBtn.classList.remove('recording');
    recordBtn.innerHTML = '🎙️';
    studioStatus.textContent = 'Prêt à enregistrer';
    recordingForm.style.display = 'none';
    recordingForm.reset();
    audioPreview.src = '';
    currentAudioBlob = null;
    if (previewAudioUrl) {
      URL.revokeObjectURL(previewAudioUrl);
      previewAudioUrl = null;
    }
  }

  recordBtn.addEventListener('click', () => {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
      stopRecording();
    } else {
      startRecording();
    }
  });

  btnCancelRecording.addEventListener('click', () => {
    resetStudioUI();
    showToast('Enregistrement annulé.');
  });

  // Soumission de l'épisode
  recordingForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    if (!currentAudioBlob) {
      showToast('❌ Aucun audio enregistré.');
      return;
    }

    const title = epTitleInput.value.trim();
    const summary = epSummaryInput.value.trim();
    const transcript = epTranscriptInput.value.trim();

    if (!title || !summary) {
      showToast('⚠️ Veuillez renseigner au moins un titre et un résumé.');
      return;
    }

    const durationStr = formatSeconds(recordSeconds || 1);
    const isOnline = navigator.onLine;

    const newEpisode = {
      id: 'ep-' + Date.now(),
      title,
      summary,
      transcript: transcript || 'Aucune transcription fournie.',
      audioBlob: currentAudioBlob,
      duration: durationStr,
      createdAt: Date.now(),
      syncStatus: isOnline ? 'synced' : 'pending',
      isSeed: false
    };

    try {
      // 1. Sauvegarde locale immédiate dans IndexedDB
      await window.VoxDB.saveEpisode(newEpisode);

      // 2. Si hors-ligne (ou pour archivage de sync), ajouter dans l'outbox
      if (!isOnline) {
        await window.VoxDB.addToOutbox('create', { id: newEpisode.id, title: newEpisode.title });
        showToast('📴 Mémo sauvegardé localement (envoi en attente de réseau).');
      } else {
        showToast('✅ Mémo publié et enregistré avec succès !');
      }

      // Enregistrer une Background Sync si supportée
      if ('serviceWorker' in navigator) {
        const reg = await navigator.serviceWorker.ready;
        if ('sync' in reg) {
          try {
            await reg.sync.register('sync-episodes');
          } catch (syncErr) {
            console.log('Background Sync indisponible :', syncErr);
          }
        }
      }

      resetStudioUI();
      await renderEpisodesList();
    } catch (err) {
      console.error('Erreur lors de la sauvegarde :', err);
      showToast('❌ Erreur lors de la sauvegarde du mémo.');
    }
  });

  // ==========================================================================
  // 6. Synchronisation de l'Outbox (retour du réseau ou manuel)
  // ==========================================================================
  async function syncOutbox() {
    if (!window.VoxDB) return;
    try {
      const pendingItems = await window.VoxDB.getOutboxItems();
      if (pendingItems.length === 0) return;

      console.log(`📤 Synchronisation de ${pendingItems.length} action(s) en attente...`);

      for (const item of pendingItems) {
        // Simulation d'appel API vers le serveur de synchronisation
        console.log(`Rejeu de l'action Outbox : ${item.action}`, item.payload);

        // Mettre à jour l'épisode correspondant dans la base locale
        if (item.payload && item.payload.id) {
          const ep = await window.VoxDB.getEpisode(item.payload.id);
          if (ep) {
            ep.syncStatus = 'synced';
            await window.VoxDB.saveEpisode(ep);
          }
        }

        // Retirer de l'outbox
        await window.VoxDB.removeOutboxItem(item.id);
      }

      showToast('🎉 Tous vos mémos hors-ligne ont été synchronisés !');
      await renderEpisodesList();
    } catch (err) {
      console.error('Erreur lors de la synchronisation de l\'outbox :', err);
    }
  }

  // ==========================================================================
  // 7. Rendu de la liste des épisodes
  // ==========================================================================
  async function renderEpisodesList() {
    if (!window.VoxDB) return;
    const episodes = await window.VoxDB.getAllEpisodes();

    episodesCount.textContent = `${episodes.length} épisode${episodes.length > 1 ? 's' : ''}`;
    episodesList.replaceChildren();

    for (const ep of episodes) {
      const li = document.createElement('li');
      li.className = 'episode-card';
      li.dataset.id = ep.id;

      const article = document.createElement('article');

      // Meta row
      const metaRow = document.createElement('div');
      metaRow.className = 'episode-meta-row';

      const badges = document.createElement('div');
      badges.className = 'episode-badges';

      const tagStatus = document.createElement('span');
      if (ep.isSeed) {
        tagStatus.className = 'tag-badge public';
        tagStatus.textContent = 'Public';
      } else if (ep.syncStatus === 'pending') {
        tagStatus.className = 'tag-badge outbox';
        tagStatus.textContent = '⏳ En attente de sync';
      } else {
        tagStatus.className = 'tag-badge local';
        tagStatus.textContent = '✅ Synchronisé';
      }
      badges.appendChild(tagStatus);

      const timeEl = document.createElement('time');
      const date = new Date(ep.createdAt);
      timeEl.textContent = date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
      metaRow.appendChild(badges);
      metaRow.appendChild(timeEl);
      article.appendChild(metaRow);

      // Titre
      const h3 = document.createElement('h3');
      h3.textContent = ep.title;
      article.appendChild(h3);

      // Résumé
      const pSummary = document.createElement('p');
      pSummary.className = 'episode-summary';
      pSummary.textContent = ep.summary;
      article.appendChild(pSummary);

      // Lecteur container
      const playerContainer = document.createElement('div');
      playerContainer.className = 'player-container';

      const playBtn = document.createElement('button');
      playBtn.className = 'play-trigger-btn';
      playBtn.textContent = '▶';
      playBtn.setAttribute('aria-label', `Écouter ${ep.title}`);
      playBtn.addEventListener('click', () => togglePlayEpisode(ep, playBtn, progressBarFill));

      const playerInfo = document.createElement('div');
      playerInfo.className = 'player-info';

      const durationInfo = document.createElement('div');
      durationInfo.className = 'player-duration';
      durationInfo.textContent = `Durée : ${ep.duration || '00:02'}`;

      const progressTrack = document.createElement('div');
      progressTrack.className = 'progress-track';

      const progressBarFill = document.createElement('div');
      progressBarFill.className = 'progress-bar-fill';
      progressTrack.appendChild(progressBarFill);

      playerInfo.appendChild(durationInfo);
      playerInfo.appendChild(progressTrack);

      playerContainer.appendChild(playBtn);
      playerContainer.appendChild(playerInfo);
      article.appendChild(playerContainer);

      // Transcription (details/summary)
      const details = document.createElement('details');
      details.className = 'transcript-details';

      const summaryTag = document.createElement('summary');
      summaryTag.textContent = '📝 Lire la transcription complète';

      const pTranscript = document.createElement('p');
      pTranscript.className = 'transcript-text';
      pTranscript.textContent = ep.transcript || 'Aucune transcription.';

      details.appendChild(summaryTag);
      details.appendChild(pTranscript);
      article.appendChild(details);

      // Barre d'actions (Partage + Suppression si local)
      const actionsBar = document.createElement('div');
      actionsBar.className = 'card-actions-bar';

      // Bouton Partage (Web Share API)
      const shareBtn = document.createElement('button');
      shareBtn.className = 'action-btn share-btn';
      shareBtn.textContent = '🔗 Partager';
      shareBtn.addEventListener('click', () => shareEpisode(ep));
      actionsBar.appendChild(shareBtn);

      // Bouton Supprimer si mémo utilisateur
      if (!ep.isSeed) {
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'action-btn delete';
        deleteBtn.textContent = '🗑️ Supprimer';
        deleteBtn.addEventListener('click', async () => {
          if (confirm(`Supprimer le mémo "${ep.title}" ?`)) {
            await window.VoxDB.removeEpisode(ep.id);
            showToast('Mémo supprimé.');
            await renderEpisodesList();
          }
        });
        actionsBar.appendChild(deleteBtn);
      }

      article.appendChild(actionsBar);
      li.appendChild(article);
      episodesList.appendChild(li);
    }
  }

  // ==========================================================================
  // 8. Gestion Audio & Capacité Media Session API
  // ==========================================================================
  function togglePlayEpisode(episode, btnEl, progressFillEl) {
    if (activeAudio && currentPlayingId === episode.id) {
      if (activeAudio.paused) {
        activeAudio.play();
        btnEl.textContent = '⏸';
      } else {
        activeAudio.pause();
        btnEl.textContent = '▶';
      }
      return;
    }

    // Si un autre audio jouait, le stopper
    if (activeAudio) {
      activeAudio.pause();
      document.querySelectorAll('.play-trigger-btn').forEach((b) => (b.textContent = '▶'));
    }

    // Créer la source audio (Blob ou URL)
    let srcUrl = episode.audioSrc;
    if (episode.audioBlob) {
      srcUrl = URL.createObjectURL(episode.audioBlob);
    }

    if (!srcUrl) {
      showToast('❌ Fichier audio introuvable pour cet épisode.');
      return;
    }

    activeAudio = new Audio(srcUrl);
    currentPlayingId = episode.id;
    btnEl.textContent = '⏸';

    // Suivi de progression
    activeAudio.addEventListener('timeupdate', () => {
      if (activeAudio.duration) {
        const pct = (activeAudio.currentTime / activeAudio.duration) * 100;
        progressFillEl.style.width = `${pct}%`;
      }
    });

    activeAudio.addEventListener('ended', () => {
      btnEl.textContent = '▶';
      progressFillEl.style.width = '0%';
      currentPlayingId = null;
    });

    activeAudio.addEventListener('pause', () => {
      btnEl.textContent = '▶';
    });

    activeAudio.addEventListener('play', () => {
      btnEl.textContent = '⏸';
    });

    // Configuration de la Media Session API (contrôles écran de verrouillage)
    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: episode.title,
        artist: 'VoxWave Community',
        album: 'VoxWave Mini-Podcasts',
        artwork: [
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' }
        ]
      });

      // Actions multimédias de l'OS
      navigator.mediaSession.setActionHandler('play', () => {
        if (activeAudio) activeAudio.play();
      });

      navigator.mediaSession.setActionHandler('pause', () => {
        if (activeAudio) activeAudio.pause();
      });

      navigator.mediaSession.setActionHandler('seekbackward', (details) => {
        if (activeAudio) {
          activeAudio.currentTime = Math.max(activeAudio.currentTime - (details.seekOffset || 5), 0);
        }
      });

      navigator.mediaSession.setActionHandler('seekforward', (details) => {
        if (activeAudio) {
          activeAudio.currentTime = Math.min(activeAudio.currentTime + (details.seekOffset || 5), activeAudio.duration || 0);
        }
      });
    }

    activeAudio.play().catch((err) => {
      console.warn('Lecture automatique bloquée ou erreur audio :', err);
      btnEl.textContent = '▶';
    });
  }

  // ==========================================================================
  // 9. Capacité Web Share API
  // ==========================================================================
  async function shareEpisode(episode) {
    const shareData = {
      title: episode.title,
      text: `Écoutez le mémo audio "${episode.title}" sur VoxWave !`,
      url: window.location.origin + '/#episodes-section'
    };

    if (navigator.share && navigator.canShare && navigator.canShare(shareData)) {
      try {
        await navigator.share(shareData);
        showToast('🎉 Épisode partagé avec succès !');
      } catch (err) {
        if (err.name !== 'AbortError') {
          console.warn('Erreur lors du partage :', err);
        }
      }
    } else {
      // Repli : Presse-papier
      try {
        await navigator.clipboard.writeText(`${shareData.text} ${shareData.url}`);
        showToast('📋 Lien de l\'épisode copié dans le presse-papier !');
      } catch (clipErr) {
        showToast('Lien : ' + shareData.url);
      }
    }
  }

  // ==========================================================================
  // 10. Toasts de notification
  // ==========================================================================
  function showToast(message, duration = 4000) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = message;

    toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('toast-out');
      toast.addEventListener('animationend', () => toast.remove());
    }, duration);
  }

  // Premier rendu de la liste
  await renderEpisodesList();
});

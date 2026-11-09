package corsica.universita.voxwave.ui.episodes

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import corsica.universita.voxwave.BuildConfig
import corsica.universita.voxwave.R
import corsica.universita.voxwave.data.Episode
import corsica.universita.voxwave.ui.theme.Amber500
import corsica.universita.voxwave.ui.theme.Emerald500
import corsica.universita.voxwave.ui.theme.Indigo500
import corsica.universita.voxwave.ui.theme.Rose500
import corsica.universita.voxwave.ui.theme.Slate700
import corsica.universita.voxwave.ui.theme.Slate800
import corsica.universita.voxwave.ui.theme.Slate900
import corsica.universita.voxwave.ui.theme.TextDim
import corsica.universita.voxwave.ui.theme.TextMuted
import corsica.universita.voxwave.ui.theme.TextPrimary
import corsica.universita.voxwave.ui.theme.Violet400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodesListScreen(
    onOpenPwa: () -> Unit,
    onShare: (Episode) -> Unit,
    viewModel: EpisodesViewModel = viewModel(factory = EpisodesViewModel.Factory),
) {
    val episodes by viewModel.episodes.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val playingId by viewModel.playingEpisodeId.collectAsState()

    // Gestion de la permission POST_NOTIFICATIONS (Android 13+ / API 33+)
    val notifPermissionFlow = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        corsica.universita.voxwave.ui.permissions.rememberPermissionFlow(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    var showNotifRationale by remember { mutableStateOf(false) }

    fun handleNotificationClick() {
        if (notifPermissionFlow != null) {
            when (notifPermissionFlow.state) {
                corsica.universita.voxwave.ui.permissions.PermissionState.Granted -> viewModel.triggerTestNotification()
                corsica.universita.voxwave.ui.permissions.PermissionState.NotAsked -> notifPermissionFlow.request()
                corsica.universita.voxwave.ui.permissions.PermissionState.DeniedOnce -> showNotifRationale = true
                corsica.universita.voxwave.ui.permissions.PermissionState.DeniedForever -> notifPermissionFlow.openSettings()
            }
        } else {
            viewModel.triggerTestNotification()
        }
    }

    // Boîte de dialogue de consentement Analytics (Annexe 4 - RGPD / CNIL)
    var showAnalyticsConsent by remember {
        mutableStateOf(corsica.universita.voxwave.VoxWaveContainer.analytics.getConsent() == null)
    }

    if (showAnalyticsConsent) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                corsica.universita.voxwave.VoxWaveContainer.analytics.setConsent(false)
                showAnalyticsConsent = false
            },
            title = { Text(stringResource(R.string.analytics_consent_title)) },
            text = { Text(stringResource(R.string.analytics_consent_text)) },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    corsica.universita.voxwave.VoxWaveContainer.analytics.setConsent(true)
                    showAnalyticsConsent = false
                }) {
                    Text(stringResource(R.string.analytics_accept))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    corsica.universita.voxwave.VoxWaveContainer.analytics.setConsent(false)
                    showAnalyticsConsent = false
                }) {
                    Text(stringResource(R.string.analytics_refuse))
                }
            },
        )
    }

    if (showNotifRationale && notifPermissionFlow != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNotifRationale = false },
            title = { Text(stringResource(R.string.permission_notif_title)) },
            text = { Text(stringResource(R.string.permission_notif_rationale)) },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    showNotifRationale = false
                    notifPermissionFlow.request()
                }) {
                    Text(stringResource(R.string.permission_grant))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showNotifRationale = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.tagline),
                            fontSize = 12.sp,
                            color = TextDim,
                        )
                    }
                },
                actions = {
                    // Bouton Test de Notification (TD A6)
                    IconButton(onClick = { handleNotificationClick() }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.test_notif),
                            tint = Amber500,
                        )
                    }

                    // Badge Outbox : « n en attente » (TD A5)
                    if (pendingCount > 0) {
                        Badge(
                            containerColor = Amber500,
                            contentColor = Slate900,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.pending_count, pendingCount, pendingCount),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    // Bouton pour ouvrir la PWA Web (TD A3)
                    IconButton(onClick = onOpenPwa) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.open_pwa),
                            tint = Violet400,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900),
            )
        },
        containerColor = Slate900,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Carte du studio d'enregistrement
            item {
                Spacer(modifier = Modifier.height(4.dp))
                StudioRecordCard(viewModel = viewModel)
            }

            // En-tête de section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.episodes_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.episodes_count, episodes.size, episodes.size),
                        fontSize = 13.sp,
                        color = TextDim,
                    )
                }
            }

            // Liste des épisodes
            items(episodes, key = { it.id }) { episode ->
                EpisodeItemCard(
                    episode = episode,
                    isPlaying = playingId == episode.id,
                    onTogglePlay = { viewModel.togglePlayEpisode(episode) },
                    onShare = { onShare(episode) },
                    onDelete = { viewModel.deleteEpisode(episode.id) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun EpisodeItemCard(
    episode: Episode,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Badges d'état (Public, Synchronisé ou En attente)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (episode.isSeed) {
                        Badge(containerColor = Indigo500.copy(alpha = 0.2f), contentColor = Indigo500) {
                            Text("Public", fontWeight = FontWeight.Bold)
                        }
                    } else if (!episode.synced) {
                        Badge(containerColor = Amber500.copy(alpha = 0.2f), contentColor = Amber500) {
                            Text("⏳ En attente", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Badge(containerColor = Emerald500.copy(alpha = 0.2f), contentColor = Emerald500) {
                            Text("✅ Synchronisé", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(text = episode.duration, fontSize = 12.sp, color = TextDim)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Titre & Résumé
            Text(
                text = episode.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = episode.summary,
                fontSize = 14.sp,
                color = TextMuted,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lecteur audio intégré
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate700)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Indigo500)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isPlaying) stringResource(R.string.playback_in_progress) else "Écouter l'enregistrement",
                    fontSize = 13.sp,
                    color = if (isPlaying) Indigo500 else TextMuted,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transcription déroulante
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "📝 Transcription complète",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDim,
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextDim,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = episode.transcript,
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate900.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    lineHeight = 18.sp,
                )
            }

            // Actions : Partage et Suppression
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = TextDim,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (!episode.isSeed) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = Rose500.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Intents du TD A3 : ouvrir la PWA ou partager un mémo */
fun Context.openPwa() {
    val url = "https://${BuildConfig.PWA_HOST}/"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
}

fun Context.shareEpisode(episode: Episode) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TITLE, episode.title)
        putExtra(
            Intent.EXTRA_TEXT,
            "🎙️ « ${episode.title} » sur VoxWave\n\n${episode.summary}\n\nhttps://${BuildConfig.PWA_HOST}/",
        )
        type = "text/plain"
    }
    startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)))
}

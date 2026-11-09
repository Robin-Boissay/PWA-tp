package corsica.universita.voxwave.ui.episodes

import android.Manifest
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import corsica.universita.voxwave.R
import corsica.universita.voxwave.ui.permissions.PermissionState
import corsica.universita.voxwave.ui.permissions.rememberPermissionFlow
import corsica.universita.voxwave.ui.theme.Indigo500
import corsica.universita.voxwave.ui.theme.Rose500
import corsica.universita.voxwave.ui.theme.Slate700
import corsica.universita.voxwave.ui.theme.Slate800
import corsica.universita.voxwave.ui.theme.TextMuted
import corsica.universita.voxwave.ui.theme.TextPrimary
import corsica.universita.voxwave.ui.theme.Violet400

@Composable
fun StudioRecordCard(
    viewModel: EpisodesViewModel,
    modifier: Modifier = Modifier,
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    val recordedFile by viewModel.recordedAudioFile.collectAsState()

    val draftTitle by viewModel.draftTitle.collectAsState()
    val draftSummary by viewModel.draftSummary.collectAsState()
    val draftTranscript by viewModel.draftTranscript.collectAsState()

    // Gestion de la permission runtime avec PermissionFlow (TD A7)
    val micPermissionFlow = rememberPermissionFlow(Manifest.permission.RECORD_AUDIO)
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    fun handleRecordClick() {
        if (isRecording) {
            viewModel.stopRecording()
        } else {
            when (micPermissionFlow.state) {
                PermissionState.Granted -> {
                    corsica.universita.voxwave.VoxWaveContainer.analytics.logMicPermission(true)
                    viewModel.startRecording()
                }
                PermissionState.NotAsked -> micPermissionFlow.request()
                PermissionState.DeniedOnce -> {
                    corsica.universita.voxwave.VoxWaveContainer.analytics.logMicPermission(false)
                    showRationaleDialog = true
                }
                PermissionState.DeniedForever -> {
                    corsica.universita.voxwave.VoxWaveContainer.analytics.logMicPermission(false)
                    showSettingsDialog = true
                }
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text(stringResource(R.string.permission_mic_title)) },
            text = { Text(stringResource(R.string.permission_mic_rationale)) },
            confirmButton = {
                Button(onClick = {
                    showRationaleDialog = false
                    micPermissionFlow.request()
                }) {
                    Text(stringResource(R.string.permission_grant))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(stringResource(R.string.permission_mic_title)) },
            text = { Text(stringResource(R.string.permission_mic_denied_forever)) },
            confirmButton = {
                Button(onClick = {
                    showSettingsDialog = false
                    micPermissionFlow.openSettings()
                }) {
                    Text(stringResource(R.string.permission_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.studio_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isRecording) stringResource(R.string.recording_in_progress)
                else if (recordedFile != null) stringResource(R.string.recording_finished)
                else stringResource(R.string.recording_ready),
                fontSize = 13.sp,
                color = TextMuted,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bouton Micro d'enregistrement avec animation de pulsation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isRecording) 1.15f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "scale",
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(if (isRecording) Rose500 else Indigo500),
            ) {
                IconButton(
                    onClick = { handleRecordClick() },
                    modifier = Modifier.size(80.dp),
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = stringResource(if (isRecording) R.string.record_stop else R.string.record_start),
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timer
            val mins = recordingSeconds / 60
            val secs = recordingSeconds % 60
            Text(
                text = String.format("%02d:%02d", mins, secs),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRecording) Rose500 else TextMuted,
            )

            // Formulaire de publication (visible dès qu'on a enregistré ou si brouillon)
            if (recordedFile != null || draftTitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { viewModel.onTitleChange(it) },
                    label = { Text(stringResource(R.string.draft_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = draftSummary,
                    onValueChange = { viewModel.onSummaryChange(it) },
                    label = { Text(stringResource(R.string.draft_summary_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = draftTranscript,
                    onValueChange = { viewModel.onTranscriptChange(it) },
                    label = { Text(stringResource(R.string.draft_transcript_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = { viewModel.cancelRecording() }) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.publishEpisode() },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                        enabled = draftTitle.isNotBlank() && draftSummary.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.publish))
                    }
                }
            }
        }
    }
}

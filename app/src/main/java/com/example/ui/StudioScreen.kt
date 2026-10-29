package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showMixerDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Audio recording permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setMicPermission(isGranted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setMicPermission(granted)
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioBg,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioBg)
        ) {
            // Sticky Studio Top Header
            StudioHeader(
                isPlaying = uiState.isPlaying,
                isRecording = uiState.isRecording,
                isLive = uiState.isLive,
                vuLevels = uiState.vuLevels
            )

            // Scrollable Studio Racks
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 11.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                // Status info chip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurface2.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "▶ ${uiState.statusMessage}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StudioGreen,
                        maxLines = 1
                    )
                }

                // 1. Import Audio
                ImportSection(
                    beatName = uiState.beatTrack.name,
                    voiceName = uiState.voiceClips.firstOrNull()?.name,
                    onImportBeat = { uri -> viewModel.importBeat(uri) },
                    onImportVoice = { uri -> viewModel.importVoice(uri) }
                )

                // 2. Waveforms & Playhead
                WaveformView(
                    beatTrack = uiState.beatTrack,
                    voiceClips = uiState.voiceClips,
                    currentPositionSec = uiState.currentPositionSec,
                    totalDurationSec = uiState.totalDurationSec,
                    onSeek = { sec -> viewModel.seekTo(sec) }
                )

                // 3. Transport Controls
                TransportControls(
                    isPlaying = uiState.isPlaying,
                    isRecording = uiState.isRecording,
                    isLive = uiState.isLive,
                    hasMicPermission = uiState.hasMicPermission,
                    onRequestMicPermission = {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onTogglePlay = { viewModel.togglePlay() },
                    onToggleRecord = { viewModel.toggleRecord() },
                    onStopAll = { viewModel.stopAll() },
                    onToggleLive = { viewModel.toggleLive() },
                    onOpenMixer = { showMixerDialog = true },
                    onOpenExport = { showExportDialog = true }
                )

                // 4. Volumes
                VolumeSection(
                    beatVolume = uiState.beatVolume,
                    voiceVolume = uiState.voiceVolume,
                    onBeatVolumeChange = { v -> viewModel.setBeatVolume(v) },
                    onVoiceVolumeChange = { v -> viewModel.setVoiceVolume(v) }
                )

                // 5. Vocal Effects Rack
                EffectsRack(
                    effects = uiState.effects,
                    onUpdateEffects = { reducer -> viewModel.updateEffects(reducer) }
                )

                // 6. Mastering Limiter
                MasteringSection(
                    effects = uiState.effects,
                    onUpdateEffects = { reducer -> viewModel.updateEffects(reducer) }
                )

                // 7. Multi-track Timeline (6 tracks)
                MultiTrackTimeline(
                    beatTrack = uiState.beatTrack,
                    voiceClips = uiState.voiceClips,
                    activeRecordTrack = uiState.activeRecordTrack,
                    selectedClipId = uiState.selectedClipId,
                    currentPositionSec = uiState.currentPositionSec,
                    totalDurationSec = uiState.totalDurationSec,
                    onSelectRecordTrack = { t -> viewModel.setActiveRecordTrack(t) },
                    onSelectClip = { id -> viewModel.selectClip(id) },
                    onAddClip = { viewModel.addEmptyClip() },
                    onMoveClipTrack = { id, t -> viewModel.moveClipTrack(id, t) },
                    onMoveClipTime = { id, s -> viewModel.moveClipTime(id, s) },
                    onSplitClip = { viewModel.splitClip() },
                    onDeleteClip = { viewModel.deleteClip() },
                    onSeek = { sec -> viewModel.seekTo(sec) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Mixer Dialog
        if (showMixerDialog) {
            MixerDialog(
                beatPan = uiState.beatPan,
                voicePan = uiState.voicePan,
                bpm = uiState.bpm,
                onBeatPanChange = { p -> viewModel.setBeatPan(p) },
                onVoicePanChange = { p -> viewModel.setVoicePan(p) },
                onBpmChange = { bpm -> viewModel.setBpm(bpm) },
                onDismiss = { showMixerDialog = false }
            )
        }

        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                totalDurationSec = uiState.totalDurationSec,
                onExportWav = {
                    showExportDialog = false
                    viewModel.exportMix()
                },
                onDismiss = { showExportDialog = false }
            )
        }
    }
}

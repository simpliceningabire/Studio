package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioClip
import com.example.audio.BeatTrack
import com.example.ui.theme.*

@Composable
fun MultiTrackTimeline(
    beatTrack: BeatTrack,
    voiceClips: List<AudioClip>,
    activeRecordTrack: Int,
    selectedClipId: String?,
    currentPositionSec: Float,
    totalDurationSec: Float,
    onSelectRecordTrack: (Int) -> Unit,
    onSelectClip: (String?) -> Unit,
    onAddClip: () -> Unit,
    onMoveClipTrack: (String, Int) -> Unit,
    onMoveClipTime: (String, Float) -> Unit,
    onSplitClip: () -> Unit,
    onDeleteClip: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val timelineWidthDp = 500.dp
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(StudioSurface.copy(alpha = 0.85f))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Toolbar with 4 Timeline action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TIMELINE — 6 PISTES",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = StudioMuted,
                letterSpacing = 2.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TimelineToolbarButton(
                    text = "➕ Ajouter",
                    textColor = StudioGreen,
                    borderColor = StudioGreen.copy(alpha = 0.4f),
                    onClick = onAddClip,
                    modifier = Modifier.testTag("timeline_add_clip")
                )

                TimelineToolbarButton(
                    text = "↕ Déplacer",
                    textColor = StudioPurple,
                    borderColor = StudioPurple.copy(alpha = 0.4f),
                    onClick = {
                        val sel = voiceClips.find { it.id == selectedClipId }
                        if (sel != null) {
                            val nextTrack = (sel.trackIndex + 1) % 6
                            onMoveClipTrack(sel.id, nextTrack)
                        }
                    },
                    modifier = Modifier.testTag("timeline_move_clip")
                )

                TimelineToolbarButton(
                    text = "✂ Diviser",
                    textColor = StudioText,
                    borderColor = StudioBorder,
                    onClick = onSplitClip,
                    modifier = Modifier.testTag("timeline_split_clip")
                )

                TimelineToolbarButton(
                    text = "🗑 Suppr.",
                    textColor = StudioRed,
                    borderColor = StudioRed.copy(alpha = 0.4f),
                    onClick = onDeleteClip,
                    modifier = Modifier.testTag("timeline_delete_clip")
                )
            }
        }

        // Recording Track Selector (V1 to V6)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "REC →",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = StudioMuted
            )

            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 0 until 6) {
                    val isSelected = i == activeRecordTrack
                    val trackBg = if (isSelected) StudioRed else StudioSurface2
                    val trackBorder = if (isSelected) StudioRed else StudioBorder

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(trackBg)
                            .border(1.dp, trackBorder, RoundedCornerShape(5.dp))
                            .clickable { onSelectRecordTrack(i) }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V${i + 1}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else StudioMuted
                        )
                    }
                }
            }
        }

        // Info Line
        val selectedClip = voiceClips.find { it.id == selectedClipId }
        val infoText = if (selectedClip != null) {
            "✅ V${selectedClip.trackIndex + 1} | Début: ${String.format("%.2fs", selectedClip.startSec)} | Durée: ${String.format("%.2fs", selectedClip.durationSec)}"
        } else {
            "Appuie sur un clip • Déplace le playhead pour vous positionner"
        }

        Text(
            text = infoText,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = if (selectedClip != null) StudioPurple else StudioMuted,
            maxLines = 1
        )

        // Scrollable Multi-track Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .clip(RoundedCornerShape(6.dp))
                .background(StudioBg)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(timelineWidthDp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 1. Time Ruler
                TimeRuler(
                    durationSec = totalDurationSec,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Beat Track Row
                TrackRow(
                    label = "BT",
                    labelColor = StudioAmber,
                    onRowTap = { frac -> onSeek(frac * totalDurationSec) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioAmber.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "🎵 ${beatTrack.name}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                // 3. 6 Voice Track Rows (V1 to V6)
                for (trackIdx in 0 until 6) {
                    val trackClips = voiceClips.filter { it.trackIndex == trackIdx }
                    val trackColor = TrackColors[trackIdx % TrackColors.size]
                    val isRecordTarget = trackIdx == activeRecordTrack

                    TrackRow(
                        label = if (isRecordTarget) "V${trackIdx + 1}🔴" else "V${trackIdx + 1}",
                        labelColor = if (isRecordTarget) StudioRed else StudioMuted,
                        onRowTap = { frac -> onSeek(frac * totalDurationSec) }
                    ) {
                        // Place clips inside track
                        Box(modifier = Modifier.fillMaxSize()) {
                            for (clip in trackClips) {
                                val startFraction = (clip.startSec / totalDurationSec).coerceIn(0f, 1f)
                                val widthFraction = (clip.durationSec / totalDurationSec).coerceIn(0.05f, 1f - startFraction)
                                val isSelected = clip.id == selectedClipId

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = startFraction + widthFraction)
                                        .padding(start = (timelineWidthDp - 36.dp) * startFraction)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(trackColor)
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, Color.White, RoundedCornerShape(4.dp))
                                            else Modifier
                                        )
                                        .clickable { onSelectClip(clip.id) }
                                        .pointerInput(clip.id) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaSec = (dragAmount.x / (timelineWidthDp.toPx() - 36.dp.toPx())) * totalDurationSec
                                                onMoveClipTime(clip.id, clip.startSec + deltaSec)
                                            }
                                        }
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "🎤 ${clip.name}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Global Playhead Line across all tracks
            val progressFrac = if (totalDurationSec > 0f) (currentPositionSec / totalDurationSec).coerceIn(0f, 1f) else 0f
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(totalDurationSec) {
                        detectTapGestures { offset ->
                            val frac = ((offset.x - 36.dp.toPx()) / (size.width - 36.dp.toPx())).coerceIn(0f, 1f)
                            onSeek(frac * totalDurationSec)
                        }
                    }
            ) {
                val availableWidth = size.width - 36.dp.toPx()
                val lineX = 36.dp.toPx() + availableWidth * progressFrac

                drawLine(
                    color = Color.White,
                    start = Offset(lineX, 0f),
                    end = Offset(lineX, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    label: String,
    labelColor: Color,
    onRowTap: (Float) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header Label
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(34.dp)
        )

        // Track Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(StudioSurface2)
                .border(0.5.dp, StudioBorder, RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width).coerceIn(0f, 1f)
                        onRowTap(frac)
                    }
                }
        ) {
            content()
        }
    }
}

@Composable
private fun TimeRuler(
    durationSec: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(start = 34.dp)
            .height(16.dp)
            .border(width = 0.5.dp, color = StudioCyan.copy(alpha = 0.2f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val steps = 8
        for (i in 0..steps) {
            val sec = (durationSec / steps) * i
            Text(
                text = formatTime(sec),
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                color = StudioMuted
            )
        }
    }
}

@Composable
private fun TimelineToolbarButton(
    text: String,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(StudioSurface2)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

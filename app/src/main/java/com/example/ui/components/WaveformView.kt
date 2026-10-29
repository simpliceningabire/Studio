package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.abs
import kotlin.math.max

@Composable
fun WaveformView(
    beatTrack: BeatTrack,
    voiceClips: List<AudioClip>,
    currentPositionSec: Float,
    totalDurationSec: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalDurationSec > 0f) (currentPositionSec / totalDurationSec).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(StudioSurface.copy(alpha = 0.85f))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "WAVEFORM",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = StudioMuted,
            letterSpacing = 2.sp
        )

        // Beat Waveform Display
        SingleWaveformBox(
            label = "BEAT",
            labelColor = StudioAmber,
            samples = beatTrack.samples,
            progress = progress,
            waveformColor = StudioAmber,
            onSeekFraction = { fraction -> onSeek(fraction * totalDurationSec) },
            modifier = Modifier.testTag("beat_waveform")
        )

        // Voice Waveform Display (Aggregated active voice clips)
        val voiceSamples = voiceClips.firstOrNull()?.samples ?: FloatArray(0)
        SingleWaveformBox(
            label = "VOIX",
            labelColor = StudioPink,
            samples = voiceSamples,
            progress = progress,
            waveformColor = StudioPink,
            onSeekFraction = { fraction -> onSeek(fraction * totalDurationSec) },
            modifier = Modifier.testTag("voice_waveform")
        )

        // Time / Scrubbing Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatTime(currentPositionSec),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = StudioText
            )

            // Scrubber
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(StudioBorder)
                    .pointerInput(totalDurationSec) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek(frac * totalDurationSec)
                        }
                    }
                    .testTag("timeline_progress_bar")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(StudioPurple, StudioPink)
                            )
                        )
                )
            }

            Text(
                text = formatTime(totalDurationSec),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = StudioMuted
            )
        }
    }
}

@Composable
private fun SingleWaveformBox(
    label: String,
    labelColor: Color,
    samples: FloatArray,
    progress: Float,
    waveformColor: Color,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(StudioSurface2)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeekFraction(frac)
                }
            }
    ) {
        // Draw Waveform Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            if (samples.isEmpty()) {
                // Draw decorative subtle ambient wave
                val bars = 64
                val barWidth = width / bars
                for (b in 0 until bars) {
                    val fakeAmp = 0.2f + 0.5f * abs(kotlin.math.sin(b * 0.35f))
                    val h = fakeAmp * (height * 0.35f)
                    val x = b * barWidth + barWidth / 2f
                    drawLine(
                        color = waveformColor.copy(alpha = 0.3f),
                        start = Offset(x, midY - h),
                        end = Offset(x, midY + h),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            } else {
                val step = max(1, samples.size / width.toInt())
                var x = 0f
                var idx = 0
                while (x < width && idx < samples.size) {
                    var maxAmp = 0f
                    for (k in 0 until step) {
                        if (idx + k < samples.size) {
                            val a = abs(samples[idx + k])
                            if (a > maxAmp) maxAmp = a
                        }
                    }
                    val h = maxAmp * (height * 0.45f).coerceAtLeast(1f)
                    drawLine(
                        color = waveformColor.copy(alpha = 0.85f),
                        start = Offset(x, midY - h),
                        end = Offset(x, midY + h),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    x += 2.dp.toPx()
                    idx += step
                }
            }

            // Draw white playhead vertical line
            val playheadX = width * progress
            drawLine(
                color = Color.White,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Top-left Track Label
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp)
        )
    }
}

fun formatTime(sec: Float): String {
    val totalSeconds = sec.coerceAtLeast(0f).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

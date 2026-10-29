package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StudioHeader(
    isPlaying: Boolean,
    isRecording: Boolean,
    isLive: Boolean,
    vuLevels: FloatArray,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val recordingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recPulse"
    )

    val dotColor by animateColorAsState(
        targetValue = when {
            isRecording -> StudioRed.copy(alpha = recordingAlpha)
            isPlaying || isLive -> StudioGreen
            else -> StudioMuted
        },
        label = "dotColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF0090912),
                        Color(0xB3090912)
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(StudioCyan, StudioPurple)
                        )
                    )
                    .border(1.dp, StudioCyanGlow, RoundedCornerShape(8.dp))
                    .shadow(12.dp, RoundedCornerShape(8.dp), spotColor = StudioCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Studio Icon",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "NingabireVocalStudio Pro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = when {
                        isRecording -> "● ENREGISTREMENT EN DIRECT"
                        isLive -> "● MONITORING MICRO ACTIF"
                        isPlaying -> "● LECTURE MULTIPISTE"
                        else -> "DAW VOCAL PROFESSIONNEL"
                    },
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRecording) StudioRed else StudioCyan,
                    letterSpacing = 1.sp
                )
            }
        }

        // VU Meter & Status Dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 10-Bar VU Meter
            Row(
                modifier = Modifier
                    .height(20.dp)
                    .testTag("vu_meter"),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                for (i in 0 until 10) {
                    val rawLevel = if (i < vuLevels.size) vuLevels[i] else 0.1f
                    val barHeight = (4 + rawLevel * 16).dp
                    val isPeak = i >= 8 && rawLevel > 0.8f
                    val barColor = if (isPeak) StudioRed else if (i >= 6) StudioAmber else StudioGreen

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(1.dp))
                            .background(barColor)
                    )
                }
            }

            // Status Indicator Dot
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
}

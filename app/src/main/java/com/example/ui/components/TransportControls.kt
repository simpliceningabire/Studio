package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun TransportControls(
    isPlaying: Boolean,
    isRecording: Boolean,
    isLive: Boolean,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleRecord: () -> Unit,
    onStopAll: () -> Unit,
    onToggleLive: () -> Unit,
    onOpenMixer: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recPulse")
    val recGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recGlow"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(StudioSurface.copy(alpha = 0.85f))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "TRANSPORT",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = StudioMuted,
            letterSpacing = 2.sp
        )

        // 1. Mic Permission Banner (if not granted)
        AnimatedVisibility(visible = !hasMicPermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioRed.copy(alpha = 0.12f))
                    .border(1.dp, StudioRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚠️ Micro non autorisé",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                    Text(
                        text = "Autorise le micro pour enregistrer et chanter en live.",
                        fontSize = 9.sp,
                        color = Color(0xFFFCA5A5).copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = onRequestMicPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioRed),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("grant_mic_button")
                ) {
                    Text("🎤 Autoriser", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Live Monitoring Indicator
        AnimatedVisibility(visible = isLive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioGreen.copy(alpha = 0.12f))
                    .border(1.dp, StudioGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StudioGreen)
                    )
                    Text(
                        text = "Micro actif — Chant en direct avec effets",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioGreen
                    )
                }

                Text(
                    text = "⏹ Stop Live",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCA5A5),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onToggleLive)
                        .padding(4.dp)
                )
            }
        }

        // 3. Main Action Buttons Row (Lire, Enregistrer, Stop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Play / Pause Button
            TransportButton(
                icon = if (isPlaying) "⏸" else "▶",
                label = if (isPlaying) "Pause" else "Lire",
                containerBrush = Brush.verticalGradient(listOf(StudioPurple, Color(0xFF6D28D9))),
                textColor = Color.White,
                glowColor = StudioPurpleGlow,
                onClick = onTogglePlay,
                modifier = Modifier
                    .weight(1f)
                    .testTag("play_pause_button")
            )

            // Record Button
            val recBg = if (isRecording) {
                StudioRed.copy(alpha = recGlow)
            } else {
                StudioSurface2
            }
            val recBorder = if (isRecording) StudioRed else StudioBorder
            val recText = if (isRecording) "⏹ Stop Rec" else "Enregistrer"

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(recBg)
                    .border(1.dp, recBorder, RoundedCornerShape(9.dp))
                    .clickable(onClick = {
                        if (hasMicPermission) onToggleRecord() else onRequestMicPermission()
                    })
                    .testTag("record_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = "⏺", fontSize = 16.sp, color = if (isRecording) Color.White else StudioRed)
                    Text(
                        text = recText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Stop All Button
            TransportButton(
                icon = "⏹",
                label = "Stop",
                containerBrush = Brush.verticalGradient(listOf(StudioSurface2, StudioSurface)),
                textColor = StudioText,
                glowColor = Color.Transparent,
                border = StudioBorder,
                onClick = onStopAll,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stop_all_button")
            )
        }

        // 4. "Chanter en Live" Wide Button
        val liveBorder = if (isLive) StudioCyan else StudioCyan.copy(alpha = 0.4f)
        val liveBg = if (isLive) StudioCyan.copy(alpha = 0.22f) else StudioCyan.copy(alpha = 0.08f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(liveBg)
                .border(1.5.dp, liveBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = {
                    if (hasMicPermission) onToggleLive() else onRequestMicPermission()
                })
                .testTag("chanter_en_live_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🎤", fontSize = 16.sp)
                Text(
                    text = if (isLive) "⏹ Arrêter le Live" else "Chanter en Live",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioCyan
                )
            }
        }

        // 5. Secondary Utility Buttons (Mixer, Exporter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Mixer Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF181828), Color(0xFF261838))))
                    .border(1.dp, StudioPurple, RoundedCornerShape(9.dp))
                    .clickable(onClick = onOpenMixer)
                    .testTag("open_mixer_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("🎚", fontSize = 14.sp)
                    Text("Mixer", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioText)
                }
            }

            // Exporter Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.linearGradient(listOf(StudioGreen, Color(0xFF059669))))
                    .shadow(8.dp, RoundedCornerShape(9.dp), spotColor = StudioGreenGlow)
                    .clickable(onClick = onOpenExport)
                    .testTag("open_export_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("⬇", fontSize = 14.sp, color = Color.White)
                    Text("Exporter WAV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TransportButton(
    icon: String,
    label: String,
    containerBrush: Brush,
    textColor: Color,
    glowColor: Color,
    border: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(containerBrush)
            .then(if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(9.dp)) else Modifier)
            .shadow(6.dp, RoundedCornerShape(9.dp), spotColor = glowColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = icon, fontSize = 16.sp, color = textColor)
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

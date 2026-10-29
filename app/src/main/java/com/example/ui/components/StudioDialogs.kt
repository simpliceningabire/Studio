package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun MixerDialog(
    beatPan: Float,
    voicePan: Float,
    bpm: Int,
    onBeatPanChange: (Float) -> Unit,
    onVoicePanChange: (Float) -> Unit,
    onBpmChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(StudioSurface)
                .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                .padding(20.dp)
                .testTag("mixer_dialog"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "🎚 Table de Mixage",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Pan Beat
            PanControl(
                title = "Pan Beat",
                pan = beatPan,
                onPanChange = onBeatPanChange,
                accentColor = StudioAmber
            )

            // Pan Voice
            PanControl(
                title = "Pan Voix",
                pan = voicePan,
                onPanChange = onVoicePanChange,
                accentColor = StudioPink
            )

            // BPM Référence
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tempo BPM Référence",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioMuted
                    )
                    Text(
                        text = "$bpm BPM",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioCyan
                    )
                }

                Slider(
                    value = bpm.toFloat(),
                    onValueChange = { onBpmChange(it.toInt()) },
                    valueRange = 60f..200f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = StudioCyan,
                        inactiveTrackColor = StudioBorder
                    )
                )
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = StudioPurple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("mixer_close_button")
            ) {
                Text("Fermer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PanControl(
    title: String,
    pan: Float,
    onPanChange: (Float) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = StudioMuted
            )
            val panLabel = when {
                pan < -0.05f -> "G ${(pan * -100).toInt()}%"
                pan > 0.05f -> "D ${(pan * 100).toInt()}%"
                else -> "Centre"
            }
            Text(
                text = panLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("G", fontSize = 10.sp, color = StudioMuted)
            Slider(
                value = pan,
                onValueChange = onPanChange,
                valueRange = -1.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier.weight(1f)
            )
            Text("D", fontSize = 10.sp, color = StudioMuted)
        }
    }
}

@Composable
fun ExportDialog(
    totalDurationSec: Float,
    onExportWav: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(StudioSurface)
                .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                .padding(20.dp)
                .testTag("export_dialog"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "⬇ Exporter le Mix",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Génère un fichier WAV haute fidélité (44.1 kHz, 16-bit Stéréo) mixant le beat, toutes les pistes de voix et l'ensemble de vos effets et du mastering.",
                fontSize = 11.sp,
                color = StudioMuted,
                lineHeight = 16.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurface2)
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Format:", fontSize = 10.sp, color = StudioMuted)
                        Text("WAV Audio (Non compressé)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Durée du mix:", fontSize = 10.sp, color = StudioMuted)
                        Text(formatTime(totalDurationSec), fontSize = 10.sp, color = StudioCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = onExportWav,
                colors = ButtonDefaults.buttonColors(containerColor = StudioGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_wav_confirm_button")
            ) {
                Text("⬇ Télécharger & Partager WAV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("Annuler", fontSize = 11.sp, color = StudioMuted)
            }
        }
    }
}

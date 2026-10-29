package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.example.audio.EffectsState
import com.example.ui.theme.*

@Composable
fun MasteringSection(
    effects: EffectsState,
    onUpdateEffects: ((EffectsState) -> EffectsState) -> Unit,
    modifier: Modifier = Modifier
) {
    val masterGold = Color(0xFFFDE68A)
    val masterBorder = Color(0x66FACC15)
    val masterBg = Color(0x10FACC15)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(masterBg)
            .border(1.5.dp, masterBorder, RoundedCornerShape(13.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🎚 MASTERING LIMITER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = masterGold,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (effects.masterOn) masterGold else StudioBorder)
                    .clickable { onUpdateEffects { it.copy(masterOn = !it.masterOn) } }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("master_limiter_toggle")
            ) {
                Text(
                    text = if (effects.masterOn) "ON" else "OFF",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (effects.masterOn) Color.Black else StudioMuted
                )
            }
        }

        AnimatedVisibility(visible = effects.masterOn) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Ceiling
                FxParamSlider(
                    label = "Ceiling",
                    value = effects.masterCeilingDb,
                    valueRange = -12f..0f,
                    displayValue = "${effects.masterCeilingDb.toInt()}dB",
                    onValueChange = { v -> onUpdateEffects { it.copy(masterCeilingDb = v) } }
                )

                // Threshold
                FxParamSlider(
                    label = "Threshold",
                    value = effects.masterThresholdDb,
                    valueRange = -24f..-1f,
                    displayValue = "${effects.masterThresholdDb.toInt()}dB",
                    onValueChange = { v -> onUpdateEffects { it.copy(masterThresholdDb = v) } }
                )

                // Release
                FxParamSlider(
                    label = "Release",
                    value = effects.masterReleaseMs,
                    valueRange = 10f..300f,
                    displayValue = "${effects.masterReleaseMs.toInt()}ms",
                    onValueChange = { v -> onUpdateEffects { it.copy(masterReleaseMs = v) } }
                )

                // Output
                FxParamSlider(
                    label = "Output",
                    value = effects.masterOutput * 100f,
                    valueRange = 50f..150f,
                    displayValue = "${(effects.masterOutput * 100).toInt()}%",
                    onValueChange = { v -> onUpdateEffects { it.copy(masterOutput = v / 100f) } }
                )

                // Gain Reduction Meter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(StudioBg)
                    ) {
                        val grPercent = (effects.gainReductionDb.coerceIn(0f, 20f) / 20f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = grPercent)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(masterGold, StudioAmber)
                                    )
                                )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gain Reduction", fontSize = 7.sp, color = StudioMuted)
                        Text(
                            text = String.format("%.1f dB", effects.gainReductionDb),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.sp,
                            color = masterGold
                        )
                    }
                }
            }
        }
    }
}

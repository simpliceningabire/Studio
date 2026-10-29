package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.EffectsState
import com.example.ui.theme.*

@Composable
fun EffectsRack(
    effects: EffectsState,
    onUpdateEffects: ((EffectsState) -> EffectsState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(StudioSurface.copy(alpha = 0.85f))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "EFFETS VOCAUX",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = StudioMuted,
            letterSpacing = 2.sp
        )

        // 1. Reverb
        EffectCard(
            title = "Reverb",
            isOn = effects.reverbOn,
            onToggle = { onUpdateEffects { it.copy(reverbOn = !it.reverbOn) } },
            modifier = Modifier.testTag("fx_reverb")
        ) {
            FxParamSlider(
                label = "Wet",
                value = effects.reverbWet * 100f,
                valueRange = 0f..100f,
                displayValue = "${(effects.reverbWet * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(reverbWet = v / 100f) } }
            )
            FxParamSlider(
                label = "Durée",
                value = effects.reverbDuration,
                valueRange = 0.5f..5.0f,
                displayValue = String.format("%.1fs", effects.reverbDuration),
                onValueChange = { v -> onUpdateEffects { it.copy(reverbDuration = v) } }
            )
        }

        // 2. Echo
        EffectCard(
            title = "Echo",
            isOn = effects.echoOn,
            onToggle = { onUpdateEffects { it.copy(echoOn = !it.echoOn) } },
            modifier = Modifier.testTag("fx_echo")
        ) {
            FxParamSlider(
                label = "Délai",
                value = effects.echoDelayMs,
                valueRange = 50f..800f,
                displayValue = "${effects.echoDelayMs.toInt()}ms",
                onValueChange = { v -> onUpdateEffects { it.copy(echoDelayMs = v) } }
            )
            FxParamSlider(
                label = "Répét.",
                value = effects.echoFeedback * 100f,
                valueRange = 0f..85f,
                displayValue = "${(effects.echoFeedback * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(echoFeedback = v / 100f) } }
            )
        }

        // 3. Delay
        EffectCard(
            title = "Delay",
            isOn = effects.delayOn,
            onToggle = { onUpdateEffects { it.copy(delayOn = !it.delayOn) } },
            modifier = Modifier.testTag("fx_delay")
        ) {
            FxParamSlider(
                label = "Temps",
                value = effects.delayTimeMs,
                valueRange = 10f..999f,
                displayValue = "${effects.delayTimeMs.toInt()}ms",
                onValueChange = { v -> onUpdateEffects { it.copy(delayTimeMs = v) } }
            )
            FxParamSlider(
                label = "Feedbk",
                value = effects.delayFeedback * 100f,
                valueRange = 0f..90f,
                displayValue = "${(effects.delayFeedback * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(delayFeedback = v / 100f) } }
            )
        }

        // 4. Compressor
        EffectCard(
            title = "Compressor",
            isOn = effects.compOn,
            onToggle = { onUpdateEffects { it.copy(compOn = !it.compOn) } },
            modifier = Modifier.testTag("fx_compressor")
        ) {
            FxParamSlider(
                label = "Seuil",
                value = effects.compThresholdDb,
                valueRange = -60f..-1f,
                displayValue = "${effects.compThresholdDb.toInt()}dB",
                onValueChange = { v -> onUpdateEffects { it.copy(compThresholdDb = v) } }
            )
            FxParamSlider(
                label = "Ratio",
                value = effects.compRatio,
                valueRange = 1f..20f,
                displayValue = "${effects.compRatio.toInt()}:1",
                onValueChange = { v -> onUpdateEffects { it.copy(compRatio = v) } }
            )
            FxParamSlider(
                label = "Attack",
                value = effects.compAttackMs,
                valueRange = 1f..100f,
                displayValue = "${effects.compAttackMs.toInt()}ms",
                onValueChange = { v -> onUpdateEffects { it.copy(compAttackMs = v) } }
            )
            FxParamSlider(
                label = "Release",
                value = effects.compReleaseMs,
                valueRange = 20f..500f,
                displayValue = "${effects.compReleaseMs.toInt()}ms",
                onValueChange = { v -> onUpdateEffects { it.copy(compReleaseMs = v) } }
            )
        }

        // 5. Limiter
        EffectCard(
            title = "Limiter",
            isOn = effects.limiterOn,
            onToggle = { onUpdateEffects { it.copy(limiterOn = !it.limiterOn) } },
            modifier = Modifier.testTag("fx_limiter")
        ) {
            FxParamSlider(
                label = "Ceiling",
                value = effects.limiterCeilingDb,
                valueRange = -20f..0f,
                displayValue = "${effects.limiterCeilingDb.toInt()}dB",
                onValueChange = { v -> onUpdateEffects { it.copy(limiterCeilingDb = v) } }
            )
        }

        // 6. EQ 4 bandes
        EffectCard(
            title = "EQ 4 bandes",
            isOn = effects.eqOn,
            onToggle = { onUpdateEffects { it.copy(eqOn = !it.eqOn) } },
            modifier = Modifier.testTag("fx_eq")
        ) {
            FxParamSlider(
                label = "200Hz",
                value = effects.eqBand1,
                valueRange = -18f..18f,
                displayValue = "${if (effects.eqBand1 > 0) "+" else ""}${effects.eqBand1.toInt()}dB",
                onValueChange = { v -> onUpdateEffects { it.copy(eqBand1 = v) } }
            )
            FxParamSlider(
                label = "600Hz",
                value = effects.eqBand2,
                valueRange = -18f..18f,
                displayValue = "${if (effects.eqBand2 > 0) "+" else ""}${effects.eqBand2.toInt()}dB",
                onValueChange = { v -> onUpdateEffects { it.copy(eqBand2 = v) } }
            )
            FxParamSlider(
                label = "2.5kHz",
                value = effects.eqBand3,
                valueRange = -18f..18f,
                displayValue = "${if (effects.eqBand3 > 0) "+" else ""}${effects.eqBand3.toInt()}dB",
                onValueChange = { v -> onUpdateEffects { it.copy(eqBand3 = v) } }
            )
            FxParamSlider(
                label = "8kHz",
                value = effects.eqBand4,
                valueRange = -18f..18f,
                displayValue = "${if (effects.eqBand4 > 0) "+" else ""}${effects.eqBand4.toInt()}dB",
                onValueChange = { v -> onUpdateEffects { it.copy(eqBand4 = v) } }
            )
        }

        // 7. Lo-Pass Filter
        EffectCard(
            title = "Lo-Pass Filter",
            isOn = effects.filterOn,
            onToggle = { onUpdateEffects { it.copy(filterOn = !it.filterOn) } },
            modifier = Modifier.testTag("fx_filter")
        ) {
            FxParamSlider(
                label = "Fréq.",
                value = effects.filterFreq,
                valueRange = 200f..8000f,
                displayValue = String.format("%.1fkHz", effects.filterFreq / 1000f),
                onValueChange = { v -> onUpdateEffects { it.copy(filterFreq = v) } }
            )
            FxParamSlider(
                label = "Réson.",
                value = effects.filterQ,
                valueRange = 0.5f..10f,
                displayValue = String.format("%.1f", effects.filterQ),
                onValueChange = { v -> onUpdateEffects { it.copy(filterQ = v) } }
            )
        }

        // 8. Distortion
        EffectCard(
            title = "Distortion",
            isOn = effects.distOn,
            onToggle = { onUpdateEffects { it.copy(distOn = !it.distOn) } },
            modifier = Modifier.testTag("fx_distortion")
        ) {
            FxParamSlider(
                label = "Drive",
                value = effects.distDrive * 100f,
                valueRange = 1f..100f,
                displayValue = "${(effects.distDrive * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(distDrive = v / 100f) } }
            )
        }

        // 9. Chorus
        EffectCard(
            title = "Chorus",
            isOn = effects.chorusOn,
            onToggle = { onUpdateEffects { it.copy(chorusOn = !it.chorusOn) } },
            modifier = Modifier.testTag("fx_chorus")
        ) {
            FxParamSlider(
                label = "Depth",
                value = effects.chorusDepthMs,
                valueRange = 1f..20f,
                displayValue = "${effects.chorusDepthMs.toInt()}ms",
                onValueChange = { v -> onUpdateEffects { it.copy(chorusDepthMs = v) } }
            )
            FxParamSlider(
                label = "Rate",
                value = effects.chorusRateHz,
                valueRange = 0.1f..5f,
                displayValue = String.format("%.1fHz", effects.chorusRateHz),
                onValueChange = { v -> onUpdateEffects { it.copy(chorusRateHz = v) } }
            )
            FxParamSlider(
                label = "Wet",
                value = effects.chorusWet * 100f,
                valueRange = 0f..100f,
                displayValue = "${(effects.chorusWet * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(chorusWet = v / 100f) } }
            )
        }

        // 10. Téléphone
        EffectCard(
            title = "📞 Téléphone",
            isOn = effects.telephoneOn,
            onToggle = { onUpdateEffects { it.copy(telephoneOn = !it.telephoneOn) } },
            modifier = Modifier.testTag("fx_telephone")
        ) {
            FxParamSlider(
                label = "Qualité",
                value = effects.telephoneQuality,
                valueRange = 1f..10f,
                displayValue = "${effects.telephoneQuality.toInt()}",
                onValueChange = { v -> onUpdateEffects { it.copy(telephoneQuality = v) } }
            )
            FxParamSlider(
                label = "Distors.",
                value = effects.telephoneDist * 100f,
                valueRange = 0f..100f,
                displayValue = "${(effects.telephoneDist * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(telephoneDist = v / 100f) } }
            )
        }

        // 11. Radio FM
        EffectCard(
            title = "📻 Radio FM",
            isOn = effects.radioOn,
            onToggle = { onUpdateEffects { it.copy(radioOn = !it.radioOn) } },
            modifier = Modifier.testTag("fx_radio")
        ) {
            FxParamSlider(
                label = "Grain",
                value = effects.radioGrain * 100f,
                valueRange = 0f..100f,
                displayValue = "${(effects.radioGrain * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(radioGrain = v / 100f) } }
            )
            FxParamSlider(
                label = "Bande",
                value = effects.radioBandwidthHz,
                valueRange = 500f..4000f,
                displayValue = String.format("%.1fkHz", effects.radioBandwidthHz / 1000f),
                onValueChange = { v -> onUpdateEffects { it.copy(radioBandwidthHz = v) } }
            )
        }

        // 12. Hall Reverb
        EffectCard(
            title = "🏟 Hall Reverb",
            isOn = effects.hallOn,
            onToggle = { onUpdateEffects { it.copy(hallOn = !it.hallOn) } },
            modifier = Modifier.testTag("fx_hall")
        ) {
            FxParamSlider(
                label = "Taille",
                value = effects.hallSize * 100f,
                valueRange = 10f..100f,
                displayValue = "${(effects.hallSize * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(hallSize = v / 100f) } }
            )
            FxParamSlider(
                label = "Wet",
                value = effects.hallWet * 100f,
                valueRange = 0f..100f,
                displayValue = "${(effects.hallWet * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(hallWet = v / 100f) } }
            )
        }

        // 13. Room Reverb
        EffectCard(
            title = "🏠 Room Reverb",
            isOn = effects.roomOn,
            onToggle = { onUpdateEffects { it.copy(roomOn = !it.roomOn) } },
            modifier = Modifier.testTag("fx_room")
        ) {
            FxParamSlider(
                label = "Taille",
                value = effects.roomSize * 100f,
                valueRange = 5f..50f,
                displayValue = "${(effects.roomSize * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(roomSize = v / 100f) } }
            )
            FxParamSlider(
                label = "Wet",
                value = effects.roomWet * 100f,
                valueRange = 0f..100f,
                displayValue = "${(effects.roomWet * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(roomWet = v / 100f) } }
            )
        }

        // 14. Cathedral
        EffectCard(
            title = "⛪ Cathedral",
            isOn = effects.cathedralOn,
            onToggle = { onUpdateEffects { it.copy(cathedralOn = !it.cathedralOn) } },
            modifier = Modifier.testTag("fx_cathedral")
        ) {
            FxParamSlider(
                label = "Durée",
                value = effects.cathedralSize * 10f,
                valueRange = 3f..10f,
                displayValue = String.format("%.1fs", effects.cathedralSize * 10f),
                onValueChange = { v -> onUpdateEffects { it.copy(cathedralSize = v / 10f) } }
            )
            FxParamSlider(
                label = "Wet",
                value = effects.cathedralWet * 100f,
                valueRange = 0f..100f,
                displayValue = "${(effects.cathedralWet * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(cathedralWet = v / 100f) } }
            )
        }

        // 15. Robotic Voice
        EffectCard(
            title = "🤖 Robotic Voice",
            isOn = effects.robotOn,
            onToggle = { onUpdateEffects { it.copy(robotOn = !it.robotOn) } },
            modifier = Modifier.testTag("fx_robot")
        ) {
            FxParamSlider(
                label = "Pitch",
                value = effects.robotPitchHz,
                valueRange = 50f..400f,
                displayValue = "${effects.robotPitchHz.toInt()}Hz",
                onValueChange = { v -> onUpdateEffects { it.copy(robotPitchHz = v) } }
            )
            FxParamSlider(
                label = "Metal",
                value = effects.robotMetal * 100f,
                valueRange = 0f..100f,
                displayValue = "${(effects.robotMetal * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(robotMetal = v / 100f) } }
            )
            FxParamSlider(
                label = "Wet",
                value = effects.robotWet * 100f,
                valueRange = 0f..100f,
                displayValue = "${(effects.robotWet * 100).toInt()}%",
                onValueChange = { v -> onUpdateEffects { it.copy(robotWet = v / 100f) } }
            )
        }
    }
}

@Composable
private fun EffectCard(
    title: String,
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (isOn) StudioCyan else StudioBorder
    val bgColor = if (isOn) StudioCyan.copy(alpha = 0.08f) else StudioSurface2.copy(alpha = 0.85f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(9.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Effect Header (Clickable to toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOn) StudioCyan else StudioText
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isOn) StudioCyan else StudioBorder)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isOn) "ON" else "OFF",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOn) Color.Black else StudioMuted
                )
            }
        }

        // Expanded Parameters when ON
        AnimatedVisibility(visible = isOn) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

@Composable
fun FxParamSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = StudioMuted,
            modifier = Modifier.width(44.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = StudioCyan,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = displayValue,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = StudioText,
            modifier = Modifier.width(44.dp)
        )
    }
}

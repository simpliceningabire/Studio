package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun VolumeSection(
    beatVolume: Float,
    voiceVolume: Float,
    onBeatVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
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
            text = "VOLUMES",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = StudioMuted,
            letterSpacing = 2.sp
        )

        // Beat Volume
        VolumeRow(
            icon = "🎵",
            label = "BEAT",
            value = beatVolume,
            color = StudioAmber,
            onValueChange = onBeatVolumeChange,
            modifier = Modifier.testTag("beat_volume_slider")
        )

        // Voice Volume
        VolumeRow(
            icon = "🎤",
            label = "VOIX",
            value = voiceVolume,
            color = StudioPink,
            onValueChange = onVoiceVolumeChange,
            modifier = Modifier.testTag("voice_volume_slider")
        )
    }
}

@Composable
private fun VolumeRow(
    icon: String,
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = icon, fontSize = 14.sp)
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = StudioMuted,
            modifier = Modifier.width(36.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1.5f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = color,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${(value * 100).toInt()}%",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = StudioMuted,
            modifier = Modifier.width(36.dp)
        )
    }
}

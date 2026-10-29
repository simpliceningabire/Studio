package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ImportSection(
    beatName: String,
    voiceName: String?,
    onImportBeat: (Uri) -> Unit,
    onImportVoice: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val beatPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onImportBeat(uri)
    }

    val voicePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onImportVoice(uri)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(StudioSurface.copy(alpha = 0.85f))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "IMPORTER AUDIO",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = StudioMuted,
            letterSpacing = 2.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Beat Import Box
            ImportCard(
                icon = "🎵",
                title = "Beat",
                subtitle = "MP3 · WAV · M4A",
                fileName = beatName,
                accentColor = StudioAmber,
                glowColor = StudioAmberGlow,
                onClick = { beatPickerLauncher.launch("audio/*") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_beat_button")
            )

            // Voice Import Box
            ImportCard(
                icon = "🎤",
                title = "Voix (fichier)",
                subtitle = "MP3 · WAV · M4A",
                fileName = voiceName ?: "Prise démo",
                accentColor = StudioPink,
                glowColor = StudioPinkGlow,
                onClick = { voicePickerLauncher.launch("audio/*") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_voice_button")
            )
        }
    }
}

@Composable
private fun ImportCard(
    icon: String,
    title: String,
    subtitle: String,
    fileName: String,
    accentColor: androidx.compose.ui.graphics.Color,
    glowColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(glowColor.copy(alpha = 0.08f))
            .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = StudioText
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = StudioMuted
            )
            Text(
                text = fileName,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

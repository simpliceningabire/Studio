package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioDspEngine
import com.example.audio.EffectsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ningabire Studio", appName)
  }

  @Test
  fun `dsp engine synthesizes beat and wav file`() {
    val beat = AudioDspEngine.generateDefaultBeat(bpm = 120, durationSec = 1.0f)
    assertTrue("Beat samples should not be empty", beat.isNotEmpty())

    val fx = EffectsState(reverbOn = true, echoOn = true)
    val processed = AudioDspEngine.processVoiceEffects(beat, fx)
    assertEquals(beat.size, processed.size)

    val stereo = FloatArray(beat.size * 2) { beat[it / 2] }
    val wavBytes = AudioDspEngine.createWavFile(stereo)
    assertTrue("WAV bytes should contain header and data", wavBytes.size > 44)
    assertEquals('R'.code.toByte(), wavBytes[0])
    assertEquals('I'.code.toByte(), wavBytes[1])
    assertEquals('F'.code.toByte(), wavBytes[2])
    assertEquals('F'.code.toByte(), wavBytes[3])
  }
}

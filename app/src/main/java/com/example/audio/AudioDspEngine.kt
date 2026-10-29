package com.example.audio

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

object AudioDspEngine {
    const val SAMPLE_RATE = 44100

    // Biquad filter state
    class BiquadFilter {
        private var a0 = 1.0f
        private var a1 = 0.0f
        private var a2 = 0.0f
        private var b0 = 1.0f
        private var b1 = 0.0f
        private var b2 = 0.0f

        private var x1 = 0.0f
        private var x2 = 0.0f
        private var y1 = 0.0f
        private var y2 = 0.0f

        fun setPeaking(freq: Float, q: Float, gainDb: Float) {
            val a = 10.0f.pow(gainDb / 40.0f)
            val w0 = 2.0f * Math.PI.toFloat() * freq / SAMPLE_RATE
            val alpha = sin(w0) / (2.0f * max(0.1f, q))
            val cosW0 = cos(w0)

            b0 = 1.0f + alpha * a
            b1 = -2.0f * cosW0
            b2 = 1.0f - alpha * a
            a0 = 1.0f + alpha / a
            a1 = -2.0f * cosW0
            a2 = 1.0f - alpha / a

            // Normalize
            b0 /= a0
            b1 /= a0
            b2 /= a0
            a1 /= a0
            a2 /= a0
        }

        fun setLowpass(freq: Float, q: Float) {
            val w0 = 2.0f * Math.PI.toFloat() * min(freq, SAMPLE_RATE * 0.49f) / SAMPLE_RATE
            val alpha = sin(w0) / (2.0f * max(0.1f, q))
            val cosW0 = cos(w0)

            b0 = (1.0f - cosW0) / 2.0f
            b1 = 1.0f - cosW0
            b2 = (1.0f - cosW0) / 2.0f
            a0 = 1.0f + alpha
            a1 = -2.0f * cosW0
            a2 = 1.0f - alpha

            b0 /= a0
            b1 /= a0
            b2 /= a0
            a1 /= a0
            a2 /= a0
        }

        fun setBandpass(freq: Float, q: Float) {
            val w0 = 2.0f * Math.PI.toFloat() * min(freq, SAMPLE_RATE * 0.49f) / SAMPLE_RATE
            val alpha = sin(w0) / (2.0f * max(0.1f, q))
            val cosW0 = cos(w0)

            b0 = alpha
            b1 = 0.0f
            b2 = -alpha
            a0 = 1.0f + alpha
            a1 = -2.0f * cosW0
            a2 = 1.0f - alpha

            b0 /= a0
            b1 /= a0
            b2 /= a0
            a1 /= a0
            a2 /= a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return if (y.isNaN() || y.isInfinite()) 0f else y
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }
    }

    /**
     * Process an entire voice audio buffer through the full effects chain.
     */
    fun processVoiceEffects(input: FloatArray, effects: EffectsState): FloatArray {
        if (input.isEmpty()) return FloatArray(0)
        var output = input.clone()

        // 1. 4-Band EQ
        if (effects.eqOn) {
            val eq1 = BiquadFilter().apply { setPeaking(200f, 1.0f, effects.eqBand1) }
            val eq2 = BiquadFilter().apply { setPeaking(600f, 1.0f, effects.eqBand2) }
            val eq3 = BiquadFilter().apply { setPeaking(2500f, 1.2f, effects.eqBand3) }
            val eq4 = BiquadFilter().apply { setPeaking(8000f, 0.8f, effects.eqBand4) }
            for (i in output.indices) {
                var s = output[i]
                s = eq1.process(s)
                s = eq2.process(s)
                s = eq3.process(s)
                s = eq4.process(s)
                output[i] = s
            }
        }

        // 2. Low-Pass Filter
        if (effects.filterOn) {
            val lpf = BiquadFilter().apply { setLowpass(effects.filterFreq, effects.filterQ) }
            for (i in output.indices) {
                output[i] = lpf.process(output[i])
            }
        }

        // 3. Distortion / Saturation
        if (effects.distOn && effects.distDrive > 0f) {
            val drive = 1.0f + effects.distDrive * 8.0f
            for (i in output.indices) {
                val x = output[i] * drive
                output[i] = tanh(x) / sqrt(drive)
            }
        }

        // 4. Telephone effect
        if (effects.telephoneOn) {
            val bpf = BiquadFilter().apply { setBandpass(1500f, 2.0f + effects.telephoneQuality * 0.3f) }
            for (i in output.indices) {
                val filtered = bpf.process(output[i])
                val sat = tanh(filtered * (1.0f + effects.telephoneDist * 3.0f))
                output[i] = output[i] * 0.15f + sat * 0.85f
            }
        }

        // 5. Radio FM effect
        if (effects.radioOn) {
            val bpf = BiquadFilter().apply { setBandpass(effects.radioBandwidthHz, 2.5f) }
            val grain = effects.radioGrain
            for (i in output.indices) {
                val filtered = bpf.process(output[i])
                // Add subtle harmonic saturation
                val loFi = tanh(filtered * (1.2f + grain * 2f))
                output[i] = output[i] * 0.2f + loFi * 0.8f
            }
        }

        // 6. Robotic Voice (Ring Modulation)
        if (effects.robotOn) {
            val carrierFreq = effects.robotPitchHz
            val metal = effects.robotMetal
            val wet = effects.robotWet
            for (i in output.indices) {
                val t = i.toFloat() / SAMPLE_RATE
                val carrier = sin(2.0f * Math.PI.toFloat() * carrierFreq * t) +
                        metal * 0.5f * sin(4.0f * Math.PI.toFloat() * carrierFreq * t)
                val robotSample = output[i] * carrier * 1.5f
                output[i] = output[i] * (1.0f - wet) + robotSample * wet
            }
        }

        // 7. Compressor
        if (effects.compOn) {
            output = applyCompressor(output, effects.compThresholdDb, effects.compRatio, effects.compAttackMs, effects.compReleaseMs)
        }

        // 8. Delay / Echo
        if (effects.delayOn || effects.echoOn) {
            val delayMs = if (effects.delayOn) effects.delayTimeMs else effects.echoDelayMs
            val feedback = if (effects.delayOn) effects.delayFeedback else effects.echoFeedback
            val delaySamples = (delayMs * SAMPLE_RATE / 1000f).toInt().coerceIn(100, SAMPLE_RATE * 3)
            val delayBuffer = FloatArray(delaySamples)
            var writeIdx = 0
            val wetGain = 0.55f

            for (i in output.indices) {
                val delayed = delayBuffer[writeIdx]
                val current = output[i]
                delayBuffer[writeIdx] = current + delayed * feedback * 0.88f
                writeIdx = (writeIdx + 1) % delaySamples
                output[i] = current + delayed * wetGain
            }
        }

        // 9. Chorus
        if (effects.chorusOn) {
            output = applyChorus(output, effects.chorusDepthMs, effects.chorusRateHz, effects.chorusWet)
        }

        // 10. Reverbs (Standard, Hall, Room, Cathedral)
        if (effects.reverbOn || effects.hallOn || effects.roomOn || effects.cathedralOn) {
            val dur = when {
                effects.cathedralOn -> 4.5f + effects.cathedralSize * 4.0f
                effects.hallOn -> 2.5f + effects.hallSize * 3.0f
                effects.roomOn -> 0.8f + effects.roomSize * 1.5f
                else -> effects.reverbDuration
            }
            val wet = when {
                effects.cathedralOn -> effects.cathedralWet
                effects.hallOn -> effects.hallWet
                effects.roomOn -> effects.roomWet
                else -> effects.reverbWet
            }
            output = applyReverb(output, dur, wet)
        }

        // 11. Limiter
        if (effects.limiterOn) {
            val ceiling = 10.0f.pow(effects.limiterCeilingDb / 20.0f)
            for (i in output.indices) {
                output[i] = output[i].coerceIn(-ceiling, ceiling)
            }
        }

        return output
    }

    private fun applyCompressor(
        input: FloatArray,
        thresholdDb: Float,
        ratio: Float,
        attackMs: Float,
        releaseMs: Float
    ): FloatArray {
        val output = FloatArray(input.size)
        val thresholdLin = 10.0f.pow(thresholdDb / 20.0f)
        val attackCoeff = exp(-1.0f / (attackMs * SAMPLE_RATE / 1000.0f))
        val releaseCoeff = exp(-1.0f / (releaseMs * SAMPLE_RATE / 1000.0f))
        var envelope = 0.0f

        for (i in input.indices) {
            val absSample = abs(input[i])
            envelope = if (absSample > envelope) {
                attackCoeff * envelope + (1.0f - attackCoeff) * absSample
            } else {
                releaseCoeff * envelope + (1.0f - releaseCoeff) * absSample
            }

            var gain = 1.0f
            if (envelope > thresholdLin && envelope > 0.0001f) {
                val envDb = 20.0f * log10(envelope)
                val targetDb = thresholdDb + (envDb - thresholdDb) / ratio
                gain = 10.0f.pow((targetDb - envDb) / 20.0f)
            }

            output[i] = input[i] * gain
        }
        return output
    }

    private fun applyChorus(
        input: FloatArray,
        depthMs: Float,
        rateHz: Float,
        wet: Float
    ): FloatArray {
        val output = FloatArray(input.size)
        val maxDelay = (30.0f * SAMPLE_RATE / 1000.0f).toInt()
        val delayBuf = FloatArray(maxDelay * 2)
        var writeIdx = 0
        val baseDelay = (15.0f * SAMPLE_RATE / 1000.0f)
        val modDepth = (depthMs * SAMPLE_RATE / 1000.0f).coerceAtMost(10.0f * SAMPLE_RATE / 1000.0f)

        for (i in input.indices) {
            delayBuf[writeIdx] = input[i]
            val lfo = sin(2.0f * Math.PI.toFloat() * rateHz * i / SAMPLE_RATE)
            val currentDelay = baseDelay + lfo * modDepth
            var readPos = writeIdx - currentDelay
            while (readPos < 0) readPos += delayBuf.size
            val r0 = readPos.toInt() % delayBuf.size
            val frac = readPos - r0
            val r1 = (r0 + 1) % delayBuf.size
            val delayed = delayBuf[r0] * (1.0f - frac) + delayBuf[r1] * frac

            output[i] = input[i] * (1.0f - wet * 0.5f) + delayed * wet
            writeIdx = (writeIdx + 1) % delayBuf.size
        }
        return output
    }

    private fun applyReverb(input: FloatArray, durationSec: Float, wet: Float): FloatArray {
        val output = FloatArray(input.size)
        // Comb filter delay times (prime-like values for natural diffusion)
        val combTimes = intArrayOf(1557, 1617, 1491, 1422, 1277, 1116)
        val combBuffers = combTimes.map { FloatArray(it) }.toTypedArray()
        val combIndices = IntArray(combTimes.size)
        val decay = (0.7f + durationSec * 0.04f).coerceIn(0.6f, 0.94f)

        // Allpass filters for diffusion
        val allpass1 = FloatArray(225)
        var ap1Idx = 0
        val allpass2 = FloatArray(556)
        var ap2Idx = 0

        for (i in input.indices) {
            val inSample = input[i]
            var combSum = 0.0f

            for (c in combTimes.indices) {
                val cIdx = combIndices[c]
                val cBuf = combBuffers[c]
                val outComb = cBuf[cIdx]
                cBuf[cIdx] = inSample + outComb * decay
                combIndices[c] = (cIdx + 1) % combTimes[c]
                combSum += outComb
            }
            combSum /= combTimes.size

            // Allpass 1
            val ap1Out = -0.5f * combSum + allpass1[ap1Idx]
            allpass1[ap1Idx] = combSum + 0.5f * ap1Out
            ap1Idx = (ap1Idx + 1) % allpass1.size

            // Allpass 2
            val ap2Out = -0.5f * ap1Out + allpass2[ap2Idx]
            allpass2[ap2Idx] = ap1Out + 0.5f * ap2Out
            ap2Idx = (ap2Idx + 1) % allpass2.size

            output[i] = inSample * (1.0f - wet * 0.5f) + ap2Out * wet * 1.5f
        }
        return output
    }

    /**
     * Mixes Beat and all Voice Clips into an interleaved Stereo FloatArray (L, R, L, R...).
     */
    fun mixProject(
        totalDurationSec: Float,
        beatTrack: BeatTrack,
        voiceClips: List<AudioClip>,
        effects: EffectsState,
        beatVolume: Float,
        voiceVolume: Float,
        beatPan: Float,
        voicePan: Float
    ): FloatArray {
        val totalSamples = (totalDurationSec * SAMPLE_RATE).toInt().coerceAtLeast(SAMPLE_RATE)
        val stereoOutput = FloatArray(totalSamples * 2)

        // 1. Render Beat
        if (beatTrack.samples.isNotEmpty()) {
            val beatLeftPan = (1.0f - beatPan).coerceIn(0.0f, 1.0f)
            val beatRightPan = (1.0f + beatPan).coerceIn(0.0f, 1.0f)
            val effectiveBeatVol = beatVolume * beatTrack.volume

            val beatLen = beatTrack.samples.size
            for (i in 0 until totalSamples) {
                val beatSample = beatTrack.samples[i % beatLen] * effectiveBeatVol
                stereoOutput[i * 2] += beatSample * beatLeftPan
                stereoOutput[i * 2 + 1] += beatSample * beatRightPan
            }
        }

        // 2. Render each Voice Clip with FX
        val voiceLeftPan = (1.0f - voicePan).coerceIn(0.0f, 1.0f)
        val voiceRightPan = (1.0f + voicePan).coerceIn(0.0f, 1.0f)

        for (clip in voiceClips) {
            if (clip.samples.isEmpty()) continue
            val processedClip = processVoiceEffects(clip.samples, effects)
            val startSample = (clip.startSec * SAMPLE_RATE).toInt()

            for (i in processedClip.indices) {
                val targetIndex = startSample + i
                if (targetIndex in 0 until totalSamples) {
                    val s = processedClip[i] * voiceVolume
                    stereoOutput[targetIndex * 2] += s * voiceLeftPan
                    stereoOutput[targetIndex * 2 + 1] += s * voiceRightPan
                }
            }
        }

        // 3. Mastering Limiter on Stereo output
        if (effects.masterOn) {
            val ceiling = 10.0f.pow(effects.masterCeilingDb / 20.0f)
            val outputGain = effects.masterOutput
            for (i in stereoOutput.indices) {
                var s = stereoOutput[i] * outputGain
                s = s.coerceIn(-ceiling, ceiling)
                stereoOutput[i] = s
            }
        } else {
            // General safety soft clip
            for (i in stereoOutput.indices) {
                stereoOutput[i] = tanh(stereoOutput[i])
            }
        }

        return stereoOutput
    }

    /**
     * Converts stereo FloatArray (-1.0 to 1.0) into a valid 16-bit 44.1kHz Stereo WAV file byte array.
     */
    fun createWavFile(stereoSamples: FloatArray): ByteArray {
        val totalAudioBytes = stereoSamples.size * 2
        val totalDataLen = totalAudioBytes + 36

        val baos = ByteArrayOutputStream()
        val out = DataOutputStream(baos)

        // RIFF header
        out.writeBytes("RIFF")
        writeIntLE(out, totalDataLen)
        out.writeBytes("WAVE")

        // fmt chunk
        out.writeBytes("fmt ")
        writeIntLE(out, 16) // 16 for PCM
        writeShortLE(out, 1.toShort()) // AudioFormat 1 = PCM
        writeShortLE(out, 2.toShort()) // Stereo (2 channels)
        writeIntLE(out, SAMPLE_RATE)
        writeIntLE(out, SAMPLE_RATE * 2 * 2) // ByteRate: SampleRate * Channels * Bits/8
        writeShortLE(out, 4.toShort()) // BlockAlign: Channels * Bits/8
        writeShortLE(out, 16.toShort()) // BitsPerSample 16

        // data chunk
        out.writeBytes("data")
        writeIntLE(out, totalAudioBytes)

        // Write PCM samples
        val byteBuffer = ByteBuffer.allocate(stereoSamples.size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (sample in stereoSamples) {
            val clamped = sample.coerceIn(-1.0f, 1.0f)
            val shortVal = (clamped * 32767.0f).toInt().toShort()
            byteBuffer.putShort(shortVal)
        }
        out.write(byteBuffer.array())
        out.flush()

        return baos.toByteArray()
    }

    private fun writeShortLE(out: DataOutputStream, value: Short) {
        out.writeByte(value.toInt() and 0xFF)
        out.writeByte((value.toInt() shr 8) and 0xFF)
    }

    private fun writeIntLE(out: DataOutputStream, value: Int) {
        out.writeByte(value and 0xFF)
        out.writeByte((value shr 8) and 0xFF)
        out.writeByte((value shr 16) and 0xFF)
        out.writeByte((value shr 24) and 0xFF)
    }

    /**
     * Generates a dynamic default hip-hop / trap Beat Loop (16 seconds at 120 BPM).
     */
    fun generateDefaultBeat(bpm: Int = 120, durationSec: Float = 16.0f): FloatArray {
        val totalSamples = (durationSec * SAMPLE_RATE).toInt()
        val samples = FloatArray(totalSamples)
        val beatInterval = (60.0f / bpm * SAMPLE_RATE).toInt()
        val barInterval = beatInterval * 4

        for (i in 0 until totalSamples) {
            val barPos = i % barInterval
            val beatPos = i % beatInterval
            var sample = 0.0f

            // 1. Kick on beat 1 and 3 (with syncopation)
            val kick1 = barPos
            val kick2 = (barPos - beatInterval * 2).let { if (it >= 0) it else Int.MAX_VALUE }
            val kick3 = (barPos - (beatInterval * 2.75f).toInt()).let { if (it >= 0) it else Int.MAX_VALUE }
            val kickDist = minOf(kick1, kick2, kick3)
            if (kickDist < SAMPLE_RATE * 0.25f) {
                val t = kickDist.toFloat() / SAMPLE_RATE
                val env = (1.0f - t / 0.25f).coerceAtLeast(0f).pow(2.5f)
                val freq = 55.0f * (1.0f + 3.0f * exp(-t * 25.0f))
                sample += sin(2.0f * Math.PI.toFloat() * freq * t) * env * 0.8f
            }

            // 2. Snare on beat 2 and 4
            val snare1 = (barPos - beatInterval).let { if (it >= 0) it else Int.MAX_VALUE }
            val snare2 = (barPos - beatInterval * 3).let { if (it >= 0) it else Int.MAX_VALUE }
            val snareDist = min(snare1, snare2)
            if (snareDist < SAMPLE_RATE * 0.2f) {
                val t = snareDist.toFloat() / SAMPLE_RATE
                val env = (1.0f - t / 0.2f).coerceAtLeast(0f).pow(2.0f)
                val noise = ((i * 1103515245 + 12345).ushr(16) and 0x7FFF).toFloat() / 32768.0f * 2.0f - 1.0f
                val tone = sin(2.0f * Math.PI.toFloat() * 180.0f * t) * 0.4f
                sample += (noise * 0.6f + tone) * env * 0.65f
            }

            // 3. Hi-Hat every eighth note
            val hatDist = beatPos % (beatInterval / 2)
            if (hatDist < SAMPLE_RATE * 0.05f) {
                val t = hatDist.toFloat() / SAMPLE_RATE
                val env = (1.0f - t / 0.05f).coerceAtLeast(0f).pow(3.0f)
                val noise = ((i * 3847291 + 777).ushr(16) and 0x7FFF).toFloat() / 32768.0f * 2.0f - 1.0f
                sample += noise * env * 0.25f
            }

            // 4. Bass 808 glide notes
            val barIndex = i / barInterval
            val baseFreq = when (barIndex % 4) {
                0 -> 43.65f // F
                1 -> 38.89f // Eb
                2 -> 34.65f // Db
                else -> 32.70f // C
            }
            val bassEnv = 0.5f + 0.3f * sin(2.0f * Math.PI.toFloat() * i / (beatInterval * 2))
            sample += sin(2.0f * Math.PI.toFloat() * baseFreq * i / SAMPLE_RATE) * 0.35f * bassEnv

            // 5. Synth pad chords (warm ambient Rhodes)
            val chordT = i.toFloat() / SAMPLE_RATE
            val chord = 0.12f * (
                    sin(2.0f * Math.PI.toFloat() * (baseFreq * 4) * chordT) +
                    0.5f * sin(2.0f * Math.PI.toFloat() * (baseFreq * 5) * chordT) +
                    0.5f * sin(2.0f * Math.PI.toFloat() * (baseFreq * 6) * chordT)
            )
            sample += chord

            samples[i] = tanh(sample * 0.9f)
        }
        return samples
    }

    /**
     * Generates a sample vocal melody snippet ("Yeah, Ningabire Studio Pro") for immediate testing!
     */
    fun generateDemoVocalPhrase(): FloatArray {
        val durationSec = 3.5f
        val totalSamples = (durationSec * SAMPLE_RATE).toInt()
        val samples = FloatArray(totalSamples)

        // Musical notes for a catchy vocal run: A3, C4, D4, E4, G4, A4
        val notes = floatArrayOf(220f, 261.63f, 293.66f, 329.63f, 392f, 440f)

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val noteIdx = ((t / durationSec) * notes.size).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]

            // Voice formant emulation (fundamental + formants)
            val f0 = sin(2.0f * Math.PI.toFloat() * freq * t)
            val f1 = 0.5f * sin(2.0f * Math.PI.toFloat() * freq * 2f * t)
            val f2 = 0.3f * sin(2.0f * Math.PI.toFloat() * 1200f * t) // Formant resonance
            val vibrato = 1.0f + 0.03f * sin(2.0f * Math.PI.toFloat() * 5.5f * t)

            // Syllable envelopes
            val phraseEnvelope = sin(Math.PI.toFloat() * t / durationSec) *
                    (0.5f + 0.5f * sin(2.0f * Math.PI.toFloat() * 2.5f * t))

            val voice = (f0 * vibrato + f1 + f2) * phraseEnvelope * 0.6f
            samples[i] = tanh(voice)
        }
        return samples
    }
}

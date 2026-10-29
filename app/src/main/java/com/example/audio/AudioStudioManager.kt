package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class AudioStudioManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null

    private var playbackJob: Job? = null
    private var recordJob: Job? = null
    private var liveJob: Job? = null

    init {
        // Initialize with default groovy beat and a demo vocal clip
        val defaultBeatSamples = AudioDspEngine.generateDefaultBeat(bpm = 120, durationSec = 16.0f)
        val defaultBeat = BeatTrack(
            name = "Studio Groove 120 BPM",
            durationSec = 16.0f,
            samples = defaultBeatSamples,
            volume = 1.0f,
            pan = 0.0f
        )

        val demoVocalSamples = AudioDspEngine.generateDemoVocalPhrase()
        val demoVocalClip = AudioClip(
            id = "demo_clip_1",
            name = "Demo Phrase",
            trackIndex = 0, // V1
            startSec = 1.0f,
            durationSec = demoVocalSamples.size.toFloat() / AudioDspEngine.SAMPLE_RATE,
            samples = demoVocalSamples
        )

        _uiState.update {
            it.copy(
                beatTrack = defaultBeat,
                totalDurationSec = defaultBeat.durationSec,
                voiceClips = listOf(demoVocalClip),
                statusMessage = "Studio prêt • Beat & Voix démo chargés"
            )
        }
    }

    fun setMicPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasMicPermission = granted, micErrorMessage = if (granted) null else "Permission micro requise") }
    }

    fun play(fromSec: Float? = null) {
        if (_uiState.value.isPlaying) {
            pause()
            return
        }

        val startPos = fromSec ?: _uiState.value.currentPositionSec
        val duration = _uiState.value.totalDurationSec
        if (startPos >= duration) {
            _uiState.update { it.copy(currentPositionSec = 0f) }
        }

        playbackJob?.cancel()
        playbackJob = scope.launch {
            runPlaybackLoop(fromSec ?: _uiState.value.currentPositionSec)
        }
    }

    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        stopAudioTrack()
        _uiState.update { it.copy(isPlaying = false, statusMessage = "En pause") }
    }

    fun stopAll() {
        playbackJob?.cancel()
        playbackJob = null
        if (_uiState.value.isRecording) {
            stopRecording()
        }
        if (_uiState.value.isLive) {
            stopLive()
        }
        stopAudioTrack()
        _uiState.update {
            it.copy(
                isPlaying = false,
                currentPositionSec = 0f,
                vuLevels = FloatArray(10),
                statusMessage = "Arrêté"
            )
        }
    }

    fun seekTo(sec: Float) {
        val clamped = sec.coerceIn(0f, _uiState.value.totalDurationSec)
        val wasPlaying = _uiState.value.isPlaying
        if (wasPlaying) {
            pause()
            _uiState.update { it.copy(currentPositionSec = clamped) }
            play(clamped)
        } else {
            _uiState.update { it.copy(currentPositionSec = clamped) }
        }
    }

    private suspend fun runPlaybackLoop(startSec: Float) {
        val state = _uiState.value
        val totalSec = state.totalDurationSec
        val totalSamples = (totalSec * AudioDspEngine.SAMPLE_RATE).toInt()

        // Mix the project into memory
        val stereoSamples = AudioDspEngine.mixProject(
            totalDurationSec = totalSec,
            beatTrack = state.beatTrack,
            voiceClips = state.voiceClips,
            effects = state.effects,
            beatVolume = state.beatVolume,
            voiceVolume = state.voiceVolume,
            beatPan = state.beatPan,
            voicePan = state.voicePan
        )

        val bufferSize = AudioTrack.getMinBufferSize(
            AudioDspEngine.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(AudioDspEngine.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        _uiState.update { it.copy(isPlaying = true, statusMessage = "Lecture en cours...") }

        val startSampleIdx = (startSec * AudioDspEngine.SAMPLE_RATE).toInt().coerceAtMost(totalSamples - 1)
        var currentSample = startSampleIdx

        val chunkSize = 1024 // stereo pairs
        val shortBuffer = ShortArray(chunkSize * 2)

        while (currentSample < totalSamples && _uiState.value.isPlaying) {
            val endSample = (currentSample + chunkSize).coerceAtMost(totalSamples)
            val count = endSample - currentSample

            var rms = 0f
            for (i in 0 until count) {
                val left = stereoSamples[(currentSample + i) * 2].coerceIn(-1.0f, 1.0f)
                val right = stereoSamples[(currentSample + i) * 2 + 1].coerceIn(-1.0f, 1.0f)
                shortBuffer[i * 2] = (left * 32767f).toInt().toShort()
                shortBuffer[i * 2 + 1] = (right * 32767f).toInt().toShort()
                rms += (left * left + right * right) / 2f
            }

            rms = sqrt(rms / max(1, count))
            val vuBars = computeVuLevels(rms)

            track.write(shortBuffer, 0, count * 2)

            currentSample = endSample
            val curSec = currentSample.toFloat() / AudioDspEngine.SAMPLE_RATE

            _uiState.update {
                it.copy(
                    currentPositionSec = curSec,
                    vuLevels = vuBars
                )
            }
        }

        // Playback finished
        track.stop()
        track.release()
        audioTrack = null
        _uiState.update { it.copy(isPlaying = false, currentPositionSec = 0f, vuLevels = FloatArray(10), statusMessage = "Fin de lecture") }
    }

    private fun stopAudioTrack() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun startRecording() {
        if (_uiState.value.isRecording) {
            stopRecording()
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            AudioDspEngine.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AudioDspEngine.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                _uiState.update { it.copy(micErrorMessage = "Impossible d'initialiser le micro") }
                return
            }

            audioRecord = record
            record.startRecording()

            _uiState.update {
                it.copy(
                    isRecording = true,
                    statusMessage = "Enregistrement en direct sur V${it.activeRecordTrack + 1}..."
                )
            }

            // Start playback in sync if beat exists
            if (!_uiState.value.isPlaying && _uiState.value.beatTrack.samples.isNotEmpty()) {
                play(_uiState.value.currentPositionSec)
            }

            recordJob?.cancel()
            recordJob = scope.launch {
                val recordedFloats = mutableListOf<Float>()
                val shortBuffer = ShortArray(1024)
                val startSec = _uiState.value.currentPositionSec

                while (_uiState.value.isRecording) {
                    val readCount = record.read(shortBuffer, 0, shortBuffer.size)
                    if (readCount > 0) {
                        var sumSq = 0f
                        for (i in 0 until readCount) {
                            val f = shortBuffer[i] / 32768.0f
                            recordedFloats.add(f)
                            sumSq += f * f
                        }
                        val rms = sqrt(sumSq / readCount)
                        val vu = computeVuLevels(rms * 1.5f)
                        _uiState.update { it.copy(vuLevels = vu) }
                    }
                }

                // Recording stopped, create new AudioClip
                if (recordedFloats.isNotEmpty()) {
                    val samples = recordedFloats.toFloatArray()
                    val durationSec = samples.size.toFloat() / AudioDspEngine.SAMPLE_RATE
                    val newClip = AudioClip(
                        id = UUID.randomUUID().toString(),
                        name = "Prise V${_uiState.value.activeRecordTrack + 1}",
                        trackIndex = _uiState.value.activeRecordTrack,
                        startSec = startSec,
                        durationSec = durationSec,
                        samples = samples
                    )

                    _uiState.update { s ->
                        val updatedClips = s.voiceClips + newClip
                        val newTotalDur = max(s.totalDurationSec, startSec + durationSec)
                        s.copy(
                            voiceClips = updatedClips,
                            totalDurationSec = newTotalDur,
                            statusMessage = "Prise enregistrée (${String.format("%.1fs", durationSec)}) sur V${s.activeRecordTrack + 1}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isRecording = false, micErrorMessage = "Erreur micro: ${e.message}") }
        }
    }

    fun stopRecording() {
        _uiState.update { it.copy(isRecording = false) }
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun toggleLive() {
        if (_uiState.value.isLive) {
            stopLive()
        } else {
            startLive()
        }
    }

    private fun startLive() {
        val bufferSize = AudioRecord.getMinBufferSize(
            AudioDspEngine.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                AudioDspEngine.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                _uiState.update { it.copy(micErrorMessage = "Erreur d'initialisation micro Live") }
                return
            }

            audioRecord = record
            record.startRecording()
            _uiState.update { it.copy(isLive = true, statusMessage = "Chant en direct actif 🎤") }

            liveJob = scope.launch {
                val shortBuffer = ShortArray(512)
                while (_uiState.value.isLive) {
                    val readCount = record.read(shortBuffer, 0, shortBuffer.size)
                    if (readCount > 0) {
                        var sumSq = 0f
                        for (i in 0 until readCount) {
                            val f = shortBuffer[i] / 32768.0f
                            sumSq += f * f
                        }
                        val rms = sqrt(sumSq / readCount)
                        val vu = computeVuLevels(rms * 2.0f)
                        _uiState.update { it.copy(vuLevels = vu) }
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLive = false, micErrorMessage = "Impossible de lancer le Live: ${e.message}") }
        }
    }

    private fun stopLive() {
        _uiState.update { it.copy(isLive = false, vuLevels = FloatArray(10), statusMessage = "Live arrêté") }
        liveJob?.cancel()
        liveJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    // ── Timeline Editing Operations ─────────────────────────────
    fun setActiveRecordTrack(trackIndex: Int) {
        _uiState.update { it.copy(activeRecordTrack = trackIndex.coerceIn(0, 5)) }
    }

    fun selectClip(clipId: String?) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun addEmptyClip() {
        val duration = 3.0f
        val samples = FloatArray((duration * AudioDspEngine.SAMPLE_RATE).toInt())
        val newClip = AudioClip(
            id = UUID.randomUUID().toString(),
            name = "Clip V${_uiState.value.activeRecordTrack + 1}",
            trackIndex = _uiState.value.activeRecordTrack,
            startSec = _uiState.value.currentPositionSec,
            durationSec = duration,
            samples = samples
        )
        _uiState.update {
            it.copy(
                voiceClips = it.voiceClips + newClip,
                selectedClipId = newClip.id,
                statusMessage = "Clip vide ajouté sur V${it.activeRecordTrack + 1}"
            )
        }
    }

    fun moveClipTrack(clipId: String, newTrackIndex: Int) {
        val validTrack = newTrackIndex.coerceIn(0, 5)
        _uiState.update { s ->
            val updated = s.voiceClips.map {
                if (it.id == clipId) it.copy(trackIndex = validTrack) else it
            }
            s.copy(voiceClips = updated, statusMessage = "Clip déplacé vers V${validTrack + 1}")
        }
    }

    fun moveClipTime(clipId: String, newStartSec: Float) {
        val validTime = newStartSec.coerceAtLeast(0f)
        _uiState.update { s ->
            val updated = s.voiceClips.map {
                if (it.id == clipId) it.copy(startSec = validTime) else it
            }
            s.copy(voiceClips = updated)
        }
    }

    fun splitSelectedClip() {
        val selectedId = _uiState.value.selectedClipId ?: return
        val clip = _uiState.value.voiceClips.find { it.id == selectedId } ?: return
        val curPos = _uiState.value.currentPositionSec
        val splitOffset = curPos - clip.startSec

        if (splitOffset <= 0.1f || splitOffset >= clip.durationSec - 0.1f) {
            _uiState.update { it.copy(statusMessage = "Place le curseur à l'intérieur du clip pour diviser") }
            return
        }

        val splitSample = (splitOffset * AudioDspEngine.SAMPLE_RATE).toInt()
        val part1Samples = clip.samples.copyOfRange(0, splitSample)
        val part2Samples = clip.samples.copyOfRange(splitSample, clip.samples.size)

        val clipA = clip.copy(
            id = UUID.randomUUID().toString(),
            name = "${clip.name} (Part 1)",
            durationSec = splitOffset,
            samples = part1Samples
        )

        val clipB = clip.copy(
            id = UUID.randomUUID().toString(),
            name = "${clip.name} (Part 2)",
            startSec = clip.startSec + splitOffset,
            durationSec = clip.durationSec - splitOffset,
            samples = part2Samples
        )

        _uiState.update { s ->
            val filtered = s.voiceClips.filter { it.id != selectedId }
            s.copy(
                voiceClips = filtered + clipA + clipB,
                selectedClipId = clipA.id,
                statusMessage = "Clip divisé en deux !"
            )
        }
    }

    fun deleteSelectedClip() {
        val selectedId = _uiState.value.selectedClipId ?: return
        _uiState.update { s ->
            val filtered = s.voiceClips.filter { it.id != selectedId }
            s.copy(
                voiceClips = filtered,
                selectedClipId = null,
                statusMessage = "Clip supprimé"
            )
        }
    }

    // ── Volumes & Pans ──────────────────────────────────────────
    fun setBeatVolume(vol: Float) {
        _uiState.update { it.copy(beatVolume = vol.coerceIn(0f, 1.5f)) }
    }

    fun setVoiceVolume(vol: Float) {
        _uiState.update { it.copy(voiceVolume = vol.coerceIn(0f, 1.5f)) }
    }

    fun setBeatPan(pan: Float) {
        _uiState.update { it.copy(beatPan = pan.coerceIn(-1.0f, 1.0f)) }
    }

    fun setVoicePan(pan: Float) {
        _uiState.update { it.copy(voicePan = pan.coerceIn(-1.0f, 1.0f)) }
    }

    fun setBpm(bpm: Int) {
        val validBpm = bpm.coerceIn(60, 200)
        _uiState.update { it.copy(bpm = validBpm) }
    }

    // ── FX Updates ──────────────────────────────────────────────
    fun updateEffects(reducer: (EffectsState) -> EffectsState) {
        _uiState.update { it.copy(effects = reducer(it.effects)) }
    }

    // ── Import Audio ────────────────────────────────────────────
    fun importBeatFromUri(uri: Uri) {
        scope.launch {
            _uiState.update { it.copy(statusMessage = "Décodage du beat...") }
            val decoded = decodeAudioUri(uri)
            if (decoded != null && decoded.isNotEmpty()) {
                val durSec = decoded.size.toFloat() / AudioDspEngine.SAMPLE_RATE
                val beat = BeatTrack(
                    name = "Beat Importé",
                    durationSec = durSec,
                    samples = decoded,
                    volume = 1.0f,
                    pan = 0.0f
                )
                _uiState.update {
                    it.copy(
                        beatTrack = beat,
                        totalDurationSec = max(it.totalDurationSec, durSec),
                        statusMessage = "Beat importé (${String.format("%.1fs", durSec)})"
                    )
                }
            } else {
                _uiState.update { it.copy(statusMessage = "Erreur de chargement du beat") }
            }
        }
    }

    fun importVoiceFromUri(uri: Uri) {
        scope.launch {
            _uiState.update { it.copy(statusMessage = "Décodage de la voix...") }
            val decoded = decodeAudioUri(uri)
            if (decoded != null && decoded.isNotEmpty()) {
                val durSec = decoded.size.toFloat() / AudioDspEngine.SAMPLE_RATE
                val clip = AudioClip(
                    id = UUID.randomUUID().toString(),
                    name = "Voix Importée",
                    trackIndex = _uiState.value.activeRecordTrack,
                    startSec = _uiState.value.currentPositionSec,
                    durationSec = durSec,
                    samples = decoded
                )
                _uiState.update {
                    it.copy(
                        voiceClips = it.voiceClips + clip,
                        selectedClipId = clip.id,
                        totalDurationSec = max(it.totalDurationSec, clip.startSec + durSec),
                        statusMessage = "Voix importée sur V${it.activeRecordTrack + 1}"
                    )
                }
            } else {
                _uiState.update { it.copy(statusMessage = "Erreur de chargement de la voix") }
            }
        }
    }

    private fun decodeAudioUri(uri: Uri): FloatArray? {
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) return null
            extractor.selectTrack(audioTrackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val floatList = mutableListOf<Float>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false

            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            while (!isEos) {
                val inIndex = codec.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                while (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuf = outBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val shorts = ShortArray(shortBuf.remaining())
                        shortBuf.get(shorts)

                        // Convert to mono float
                        for (i in 0 until shorts.size step channelCount) {
                            var sum = 0f
                            for (c in 0 until channelCount) {
                                if (i + c < shorts.size) {
                                    sum += shorts[i + c] / 32768.0f
                                }
                            }
                            floatList.add(sum / channelCount)
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            floatList.toFloatArray()
        } catch (_: Exception) {
            null
        }
    }

    // ── WAV Export & Share ──────────────────────────────────────
    fun exportAndShareMix() {
        scope.launch {
            _uiState.update { it.copy(statusMessage = "Rendu du mix en cours...") }
            val state = _uiState.value

            val stereoSamples = AudioDspEngine.mixProject(
                totalDurationSec = state.totalDurationSec,
                beatTrack = state.beatTrack,
                voiceClips = state.voiceClips,
                effects = state.effects,
                beatVolume = state.beatVolume,
                voiceVolume = state.voiceVolume,
                beatPan = state.beatPan,
                voicePan = state.voicePan
            )

            val wavBytes = AudioDspEngine.createWavFile(stereoSamples)
            val exportFile = File(context.cacheDir, "NingabireStudio_Mix.wav")
            FileOutputStream(exportFile).use { fos ->
                fos.write(wavBytes)
            }

            _uiState.update { it.copy(statusMessage = "Mix rendu (${exportFile.length() / 1024} KB)") }

            // Open Android Share Sheet
            withContext(Dispatchers.Main) {
                try {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        exportFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/wav"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Partager le mix audio"))
                } catch (e: Exception) {
                    _uiState.update { it.copy(statusMessage = "Fichier exporté: ${exportFile.name}") }
                }
            }
        }
    }

    private fun computeVuLevels(rms: Float): FloatArray {
        val levels = FloatArray(10)
        val normalized = (rms * 3.5f).coerceIn(0.0f, 1.0f)
        for (i in 0 until 10) {
            val threshold = (i + 1) / 10.0f
            levels[i] = if (normalized >= threshold) 1.0f else (normalized / threshold).coerceIn(0f, 0.9f)
        }
        return levels
    }
}

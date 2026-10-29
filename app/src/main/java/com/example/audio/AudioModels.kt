package com.example.audio

data class AudioClip(
    val id: String,
    val name: String,
    val trackIndex: Int, // 0 to 5 for V1 to V6
    val startSec: Float,
    val durationSec: Float,
    val samples: FloatArray // Normalized -1.0f to 1.0f mono PCM at 44100Hz
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioClip
        return id == other.id &&
                trackIndex == other.trackIndex &&
                startSec == other.startSec &&
                durationSec == other.durationSec
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + trackIndex
        result = 31 * result + startSec.hashCode()
        result = 31 * result + durationSec.hashCode()
        return result
    }
}

data class BeatTrack(
    val name: String = "Groovy Studio Beat",
    val durationSec: Float = 16.0f,
    val samples: FloatArray = FloatArray(0),
    val volume: Float = 1.0f,
    val pan: Float = 0.0f // -1.0 (Left) to +1.0 (Right)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BeatTrack
        return name == other.name &&
                durationSec == other.durationSec &&
                volume == other.volume &&
                pan == other.pan
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + durationSec.hashCode()
        result = 31 * result + volume.hashCode()
        result = 31 * result + pan.hashCode()
        return result
    }
}

data class EffectsState(
    // Reverb
    val reverbOn: Boolean = false,
    val reverbWet: Float = 0.40f,
    val reverbDuration: Float = 2.0f,

    // Echo
    val echoOn: Boolean = false,
    val echoDelayMs: Float = 300f,
    val echoFeedback: Float = 0.40f,

    // Delay
    val delayOn: Boolean = false,
    val delayTimeMs: Float = 250f,
    val delayFeedback: Float = 0.35f,

    // Compressor
    val compOn: Boolean = false,
    val compThresholdDb: Float = -24f,
    val compRatio: Float = 4f,
    val compAttackMs: Float = 5f,
    val compReleaseMs: Float = 150f,

    // Limiter
    val limiterOn: Boolean = false,
    val limiterCeilingDb: Float = -3f,

    // EQ 4 bands (-18dB to +18dB)
    val eqOn: Boolean = false,
    val eqBand1: Float = 0f, // 200 Hz
    val eqBand2: Float = 0f, // 600 Hz
    val eqBand3: Float = 0f, // 2.5 kHz
    val eqBand4: Float = 0f, // 8 kHz

    // Low-Pass Filter
    val filterOn: Boolean = false,
    val filterFreq: Float = 3000f,
    val filterQ: Float = 1.0f,

    // Distortion
    val distOn: Boolean = false,
    val distDrive: Float = 0.30f,

    // Chorus
    val chorusOn: Boolean = false,
    val chorusDepthMs: Float = 8f,
    val chorusRateHz: Float = 1.2f,
    val chorusWet: Float = 0.50f,

    // Telephone
    val telephoneOn: Boolean = false,
    val telephoneQuality: Float = 5f,
    val telephoneDist: Float = 0.30f,

    // Radio FM
    val radioOn: Boolean = false,
    val radioGrain: Float = 0.40f,
    val radioBandwidthHz: Float = 2000f,

    // Hall Reverb
    val hallOn: Boolean = false,
    val hallSize: Float = 0.60f,
    val hallWet: Float = 0.50f,

    // Room Reverb
    val roomOn: Boolean = false,
    val roomSize: Float = 0.20f,
    val roomWet: Float = 0.35f,

    // Cathedral
    val cathedralOn: Boolean = false,
    val cathedralSize: Float = 0.70f,
    val cathedralWet: Float = 0.55f,

    // Robotic Voice
    val robotOn: Boolean = false,
    val robotPitchHz: Float = 120f,
    val robotMetal: Float = 0.50f,
    val robotWet: Float = 0.80f,

    // Mastering Limiter
    val masterOn: Boolean = false,
    val masterCeilingDb: Float = -1f,
    val masterThresholdDb: Float = -6f,
    val masterReleaseMs: Float = 80f,
    val masterOutput: Float = 1.0f,
    val gainReductionDb: Float = 0.0f
)

data class StudioUiState(
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val isLive: Boolean = false,
    val currentPositionSec: Float = 0f,
    val totalDurationSec: Float = 16.0f,
    val activeRecordTrack: Int = 0, // 0 to 5
    val selectedClipId: String? = null,
    val beatVolume: Float = 1.0f,
    val voiceVolume: Float = 1.0f,
    val beatPan: Float = 0.0f,
    val voicePan: Float = 0.0f,
    val bpm: Int = 120,
    val beatTrack: BeatTrack = BeatTrack(),
    val voiceClips: List<AudioClip> = emptyList(),
    val effects: EffectsState = EffectsState(),
    val vuLevels: FloatArray = FloatArray(10), // 10 bars (0.0 to 1.0)
    val hasMicPermission: Boolean = false,
    val micErrorMessage: String? = null,
    val statusMessage: String = "Prêt à enregistrer"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StudioUiState
        return isPlaying == other.isPlaying &&
                isRecording == other.isRecording &&
                isLive == other.isLive &&
                currentPositionSec == other.currentPositionSec &&
                totalDurationSec == other.totalDurationSec &&
                activeRecordTrack == other.activeRecordTrack &&
                selectedClipId == other.selectedClipId &&
                beatVolume == other.beatVolume &&
                voiceVolume == other.voiceVolume &&
                beatPan == other.beatPan &&
                voicePan == other.voicePan &&
                bpm == other.bpm &&
                beatTrack == other.beatTrack &&
                voiceClips == other.voiceClips &&
                effects == other.effects &&
                hasMicPermission == other.hasMicPermission &&
                micErrorMessage == other.micErrorMessage &&
                statusMessage == other.statusMessage
    }

    override fun hashCode(): Int {
        var result = isPlaying.hashCode()
        result = 31 * result + isRecording.hashCode()
        result = 31 * result + isLive.hashCode()
        result = 31 * result + currentPositionSec.hashCode()
        result = 31 * result + totalDurationSec.hashCode()
        result = 31 * result + activeRecordTrack
        result = 31 * result + (selectedClipId?.hashCode() ?: 0)
        return result
    }
}

package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.example.audio.AudioStudioManager
import com.example.audio.EffectsState
import com.example.audio.StudioUiState
import kotlinx.coroutines.flow.StateFlow

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    val audioManager = AudioStudioManager(application.applicationContext)
    val uiState: StateFlow<StudioUiState> = audioManager.uiState

    fun togglePlay() {
        if (uiState.value.isPlaying) {
            audioManager.pause()
        } else {
            audioManager.play()
        }
    }

    fun toggleRecord() {
        if (uiState.value.isRecording) {
            audioManager.stopRecording()
        } else {
            audioManager.startRecording()
        }
    }

    fun stopAll() {
        audioManager.stopAll()
    }

    fun toggleLive() {
        audioManager.toggleLive()
    }

    fun seekTo(sec: Float) {
        audioManager.seekTo(sec)
    }

    fun setMicPermission(granted: Boolean) {
        audioManager.setMicPermissionGranted(granted)
    }

    fun setActiveRecordTrack(trackIndex: Int) {
        audioManager.setActiveRecordTrack(trackIndex)
    }

    fun selectClip(clipId: String?) {
        audioManager.selectClip(clipId)
    }

    fun addEmptyClip() {
        audioManager.addEmptyClip()
    }

    fun moveClipTrack(clipId: String, newTrack: Int) {
        audioManager.moveClipTrack(clipId, newTrack)
    }

    fun moveClipTime(clipId: String, newStartSec: Float) {
        audioManager.moveClipTime(clipId, newStartSec)
    }

    fun splitClip() {
        audioManager.splitSelectedClip()
    }

    fun deleteClip() {
        audioManager.deleteSelectedClip()
    }

    fun setBeatVolume(vol: Float) {
        audioManager.setBeatVolume(vol)
    }

    fun setVoiceVolume(vol: Float) {
        audioManager.setVoiceVolume(vol)
    }

    fun setBeatPan(pan: Float) {
        audioManager.setBeatPan(pan)
    }

    fun setVoicePan(pan: Float) {
        audioManager.setVoicePan(pan)
    }

    fun setBpm(bpm: Int) {
        audioManager.setBpm(bpm)
    }

    fun updateEffects(reducer: (EffectsState) -> EffectsState) {
        audioManager.updateEffects(reducer)
    }

    fun importBeat(uri: Uri) {
        audioManager.importBeatFromUri(uri)
    }

    fun importVoice(uri: Uri) {
        audioManager.importVoiceFromUri(uri)
    }

    fun exportMix() {
        audioManager.exportAndShareMix()
    }
}

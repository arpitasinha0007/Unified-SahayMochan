// SoundPlayerManager.kt
package com.example.unifiedapp.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.*

class SoundPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSoundId: String? = null
    private var currentSoundResId: Int? = null  // Store the resource ID for replaying
    private var volume: Float = 0.7f

    // For volume updates
    private val _volumeState = mutableStateOf(volume)
    val volumeState: State<Float> = _volumeState

    // For playback state
    private val _isPlayingState = mutableStateOf(false)
    val isPlayingState: State<Boolean> = _isPlayingState

    // For current sound
    private val _currentSoundIdState = mutableStateOf<String?>(null)
    val currentSoundIdState: State<String?> = _currentSoundIdState

    init {
        setupMediaPlayer()
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            setOnCompletionListener {
                // When sound finishes, replay it from the beginning
                _isPlayingState.value = false
                // Automatically restart the sound
                currentSoundId?.let { soundId ->
                    currentSoundResId?.let { resId ->
                        playSound(soundId, resId)
                    }
                }
            }

            setOnErrorListener { _, what, extra ->
                println("MediaPlayer error: $what, $extra")
                _isPlayingState.value = false
                true
            }
        }
    }

    fun playSound(soundId: String, soundResId: Int) {
        try {
            // Stop current playback if any
            stopSound()

            // Store the current sound info for replaying
            currentSoundId = soundId
            currentSoundResId = soundResId

            // Get raw resource URI
            val uri = Uri.parse("android.resource://${context.packageName}/$soundResId")

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(context, uri)
            mediaPlayer?.prepareAsync()

            mediaPlayer?.setOnPreparedListener {
                it.isLooping = false // Don't use MediaPlayer looping, we handle it manually in onCompletionListener
                it.setVolume(volume, volume)
                it.start()

                _isPlayingState.value = true
                _currentSoundIdState.value = soundId
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSoundFromUrl(soundId: String, url: String) {
        try {
            stopSound()

            currentSoundId = soundId
            // For URL-based sounds, we don't store a resource ID

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(url)
            mediaPlayer?.prepareAsync()

            mediaPlayer?.setOnPreparedListener {
                it.isLooping = false // Don't use MediaPlayer looping, we handle it manually
                it.setVolume(volume, volume)
                it.start()

                _isPlayingState.value = true
                _currentSoundIdState.value = soundId
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlayingState.value = false
            }
        }
    }

    fun resumeSound() {
        mediaPlayer?.let {
            if (!it.isPlaying && currentSoundId != null) {
                it.start()
                _isPlayingState.value = true
            }
        }
    }

    fun stopSound() {
        mediaPlayer?.let {
            if (it.isPlaying || currentSoundId != null) {
                it.stop()
                it.reset()
                _isPlayingState.value = false
                _currentSoundIdState.value = null
                currentSoundId = null
                currentSoundResId = null
            }
        }
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        _volumeState.value = volume
        mediaPlayer?.setVolume(volume, volume)
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun togglePlayPause(soundId: String, soundResId: Int) {
        if (currentSoundId == soundId && _isPlayingState.value) {
            pauseSound()
        } else if (currentSoundId == soundId && !_isPlayingState.value) {
            resumeSound()
        } else {
            playSound(soundId, soundResId)
        }
    }

    // Optional: Add a method to toggle play/pause for URL-based sounds
    fun togglePlayPauseFromUrl(soundId: String, url: String) {
        if (currentSoundId == soundId && _isPlayingState.value) {
            pauseSound()
        } else if (currentSoundId == soundId && !_isPlayingState.value) {
            resumeSound()
        } else {
            playSoundFromUrl(soundId, url)
        }
    }
}
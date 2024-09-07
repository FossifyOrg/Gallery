package org.fossify.gallery.helpers

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import org.fossify.gallery.extensions.audioManager

class VolumeController(
    private val context: Context,
    private val streamType: Int = AudioManager.STREAM_MUSIC,
    private val onVolumeChanged: (isMuted: Boolean) -> Unit
) {
    private var audioManager = context.audioManager
    private var savedVolume = audioManager.getStreamMaxVolume(streamType)

    private val currentVolume: Int
        get() = audioManager.getStreamVolume(streamType)

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            onVolumeChanged(isMuted())
        }
    }

    init {
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)
        onVolumeChanged(isMuted())
    }

    private fun isMuted() = currentVolume == 0

    private fun mute() {
        savedVolume = audioManager.getStreamVolume(streamType)
        audioManager.setStreamVolume(streamType, 0, 0)
    }

    private fun unmute() {
        audioManager.setStreamVolume(streamType, savedVolume, 0)
    }

    fun toggleMute() {
        if (isMuted()) {
            unmute()
            onVolumeChanged(false)
        } else {
            mute()
            onVolumeChanged(true)
        }
    }

    fun destroy() {
        context.contentResolver.unregisterContentObserver(volumeObserver)
    }
}

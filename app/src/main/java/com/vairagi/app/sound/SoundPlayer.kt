package com.vairagi.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.vairagi.app.R

class SoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Plays the 3-second chime sound if soundEnabled is true.
     * Respects system ringer mode unless forced, using USAGE_NOTIFICATION_EVENT audio attributes.
     */
    fun playChime(soundEnabled: Boolean) {
        if (!soundEnabled) {
            Log.d("SoundPlayer", "Popup sound disabled by user setting.")
            return
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            // Check if ringer mode is SILENT or VIBRATE
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                Log.d("SoundPlayer", "Device is in Silent mode; suppressing sound.")
                return
            }
        }

        try {
            stop()
            mediaPlayer = MediaPlayer.create(context, R.raw.chime_sound)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                )
                setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundPlayer", "Failed to play chime sound", e)
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }
}

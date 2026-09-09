package com.example.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AudioPlayer(private val context: Context) {

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    var isPlaying: Boolean = false
        private set

    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                try {
                    // Try Hindi or Indian English locale by default
                    val hindiLocale = Locale("hi", "IN")
                    if (textToSpeech?.isLanguageAvailable(hindiLocale) == TextToSpeech.LANG_AVAILABLE) {
                        textToSpeech?.language = hindiLocale
                    } else {
                        textToSpeech?.language = Locale("en", "IN")
                    }
                } catch (e: Exception) {
                    textToSpeech?.language = Locale.getDefault()
                }

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        setPlayingState(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        setPlayingState(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        setPlayingState(false)
                    }
                })
            }
        }
    }

    /**
     * Immediately stops any current playback (handles interruptions).
     */
    fun stop() {
        try {
            if (audioTrack != null) {
                audioTrack?.stop()
                audioTrack?.flush()
                audioTrack?.release()
                audioTrack = null
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping AudioTrack: ${e.message}")
        }

        try {
            if (mediaPlayer != null) {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.reset()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping MediaPlayer: ${e.message}")
        }

        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping TTS: ${e.message}")
        }

        setPlayingState(false)
    }

    /**
     * Plays audio data from Gemini response (PCM, WAV, MP3).
     */
    fun playAudioData(mimeType: String, base64Data: String, onDone: (() -> Unit)? = null): Boolean {
        stop()

        return try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (audioBytes.isEmpty()) return false

            if (mimeType.contains("pcm", ignoreCase = true) || mimeType.contains("raw", ignoreCase = true)) {
                playPcmBytes(audioBytes, onDone)
            } else {
                playMediaBytes(audioBytes, onDone)
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to play audio data: ${e.message}")
            setPlayingState(false)
            false
        }
    }

    private fun playPcmBytes(pcmBytes: ByteArray, onDone: (() -> Unit)? = null): Boolean {
        return try {
            val sampleRate = 24000 // Standard Gemini audio output sample rate
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
                maxOf(minBufferSize, pcmBytes.size),
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            track.write(pcmBytes, 0, pcmBytes.size)
            track.setNotificationMarkerPosition(pcmBytes.size / 2) // 16-bit PCM = 2 bytes per sample
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack?) {}
                override fun onMarkerReached(track: AudioTrack?) {
                    setPlayingState(false)
                    onDone?.invoke()
                    track?.release()
                }
            })

            audioTrack = track
            setPlayingState(true)
            track.play()
            true
        } catch (e: Exception) {
            Log.e("AudioPlayer", "PCM playback failed: ${e.message}")
            setPlayingState(false)
            false
        }
    }

    private fun playMediaBytes(audioBytes: ByteArray, onDone: (() -> Unit)? = null): Boolean {
        return try {
            val tempFile = File.createTempFile("gemini_audio_", ".tmp", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    setPlayingState(false)
                    onDone?.invoke()
                    try {
                        tempFile.delete()
                    } catch (_: Exception) {}
                }
                setOnErrorListener { _, _, _ ->
                    setPlayingState(false)
                    try {
                        tempFile.delete()
                    } catch (_: Exception) {}
                    false
                }
            }

            mediaPlayer = mp
            setPlayingState(true)
            mp.start()
            true
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Media playback failed: ${e.message}")
            setPlayingState(false)
            false
        }
    }

    /**
     * Fallback speech synthesis if Gemini audio is unavailable (e.g. offline/no API key).
     */
    fun speakFallbackText(text: String, languageCode: String? = null, onDone: (() -> Unit)? = null) {
        stop()
        if (!isTtsReady || textToSpeech == null) {
            onDone?.invoke()
            return
        }

        try {
            if (languageCode != null) {
                val locale = when (languageCode.lowercase()) {
                    "hi" -> Locale("hi", "IN")
                    "mr" -> Locale("mr", "IN")
                    "gu" -> Locale("gu", "IN")
                    "ta" -> Locale("ta", "IN")
                    "te" -> Locale("te", "IN")
                    "bn" -> Locale("bn", "IN")
                    else -> Locale("en", "IN")
                }
                if (textToSpeech?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                    textToSpeech?.language = locale
                }
            }
            setPlayingState(true)
            val utteranceId = "tts_${System.currentTimeMillis()}"
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            setPlayingState(false)
            onDone?.invoke()
        }
    }

    private fun setPlayingState(playing: Boolean) {
        isPlaying = playing
        onPlaybackStateChanged?.invoke(playing)
    }

    fun release() {
        stop()
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}

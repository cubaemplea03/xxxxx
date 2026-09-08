package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sin
import kotlin.random.Random

class GameAudioEngine(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    var musicEnabled: Boolean = true
    var sfxEnabled: Boolean = true
    var musicVolume: Float = 0.8f
    var sfxVolume: Float = 0.9f
    var vibrationEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var ambientMusicJob: Job? = null
    private var trainSoundJob: Job? = null
    private val sfxQueue = ConcurrentLinkedQueue<ShortArray>()

    private val sampleRate = 22050
    private var sfxAudioTrack: AudioTrack? = null
    private var isRunning = true

    init {
        initSfxTrack()
        startSfxWorker()
    }

    private fun initSfxTrack() {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 4)

            sfxAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            sfxAudioTrack?.play()
        } catch (_: Exception) {
        }
    }

    private fun startSfxWorker() {
        scope.launch {
            while (isRunning) {
                val samples = sfxQueue.poll()
                if (samples != null && sfxAudioTrack != null && sfxEnabled) {
                    try {
                        sfxAudioTrack?.write(samples, 0, samples.size)
                    } catch (_: Exception) {
                    }
                } else {
                    delay(16)
                }
            }
        }
    }

    fun startAmbientMusic() {
        ambientMusicJob?.cancel()
        ambientMusicJob = scope.launch {
            // Generates dark atmospheric drone / mysterious cinematic tones
            val notes = floatArrayOf(110.0f, 130.81f, 146.83f, 164.81f, 196.0f, 220.0f) // A minor / post-apocalyptic scale
            var step = 0
            while (isActive && isRunning) {
                if (musicEnabled && musicVolume > 0.05f) {
                    val rootFreq = notes[step % notes.size]
                    val durationMs = 1800
                    val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                    val buffer = ShortArray(numSamples)

                    val vol = (musicVolume * 0.25f).coerceIn(0f, 1f)
                    for (i in 0 until numSamples) {
                        val t = i.toFloat() / sampleRate
                        // Soft slow envelope
                        val envelope = when {
                            i < sampleRate * 0.4f -> i / (sampleRate * 0.4f)
                            i > numSamples - sampleRate * 0.4f -> (numSamples - i) / (sampleRate * 0.4f)
                            else -> 1.0f
                        }
                        // Fundamental + harmonic + sub octave
                        val sampleVal = (sin(2.0 * Math.PI * rootFreq * t) * 0.5 +
                                sin(2.0 * Math.PI * (rootFreq * 1.5) * t) * 0.25 +
                                sin(2.0 * Math.PI * (rootFreq * 0.5) * t) * 0.3) * envelope * Short.MAX_VALUE * vol

                        buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    if (musicEnabled) {
                        sfxQueue.offer(buffer)
                    }
                    step++
                    delay(1600)
                } else {
                    delay(500)
                }
            }
        }
    }

    fun startTrainRhythm() {
        trainSoundJob?.cancel()
        trainSoundJob = scope.launch {
            while (isActive && isRunning) {
                if (sfxEnabled && sfxVolume > 0.05f) {
                    // Click-clack train wheels rhythm
                    playWheelClack()
                    delay(240)
                    playWheelClack(lower = true)
                    delay(380)
                    playWheelClack()
                    delay(220)
                    playWheelClack(lower = true)
                    delay(500)
                } else {
                    delay(500)
                }
            }
        }
    }

    fun stopTrainRhythm() {
        trainSoundJob?.cancel()
    }

    private fun playWheelClack(lower: Boolean = false) {
        val durationMs = 45
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.25f).coerceIn(0f, 1f)
        val baseFreq = if (lower) 140f else 180f

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = 1.0f - (i.toFloat() / numSamples)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.4f
            val tone = sin(2.0 * Math.PI * baseFreq * t).toFloat() * 0.6f
            val sampleVal = (tone + noise) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playButtonClick() {
        if (!sfxEnabled) return
        vibrate(20)
        val durationMs = 35
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.4f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 450f + 250f * (1.0f - i.toFloat() / numSamples)
            val decay = 1.0f - (i.toFloat() / numSamples)
            val sampleVal = sin(2.0 * Math.PI * freq * t) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playJump() {
        if (!sfxEnabled) return
        val durationMs = 90
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.5f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = 220f + progress * 400f // pitch sweep up
            val decay = 1.0f - progress * 0.7f
            val sampleVal = sin(2.0 * Math.PI * freq * (i.toFloat() / sampleRate)) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playAttackSwing() {
        if (!sfxEnabled) return
        val durationMs = 80
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.6f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val noise = (Random.nextFloat() * 2f - 1f)
            val freq = 380f - progress * 200f
            val tone = sin(2.0 * Math.PI * freq * (i.toFloat() / sampleRate)).toFloat() * 0.5f
            val decay = (1.0f - progress)
            val sampleVal = (noise * 0.7f + tone) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playGunshot() {
        if (!sfxEnabled) return
        vibrate(50)
        val durationMs = 120
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.7f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val noise = (Random.nextFloat() * 2f - 1f)
            val boom = sin(2.0 * Math.PI * 90f * (i.toFloat() / sampleRate)).toFloat()
            val decay = (1.0f - progress) * (1.0f - progress)
            val sampleVal = (noise * 0.8f + boom * 0.5f) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playEnemyHit() {
        if (!sfxEnabled) return
        vibrate(35)
        val durationMs = 60
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.65f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val noise = (Random.nextFloat() * 2f - 1f)
            val thump = sin(2.0 * Math.PI * 130f * (i.toFloat() / sampleRate)).toFloat()
            val decay = 1.0f - progress
            val sampleVal = (thump * 0.7f + noise * 0.3f) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playEnemyDeath() {
        if (!sfxEnabled) return
        val durationMs = 140
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.7f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = 200f - progress * 120f
            val growl = sin(2.0 * Math.PI * freq * (i.toFloat() / sampleRate)).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f) * 0.4f
            val decay = 1.0f - progress
            val sampleVal = (growl + noise) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playPlayerHurt() {
        if (!sfxEnabled) return
        vibrate(100)
        val durationMs = 150
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.8f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = 120f - progress * 40f
            val tone = sin(2.0 * Math.PI * freq * (i.toFloat() / sampleRate)).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f) * 0.5f
            val decay = 1.0f - progress
            val sampleVal = (tone + noise) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playItemPickup() {
        if (!sfxEnabled) return
        vibrate(25)
        val durationMs = 110
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.6f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = if (progress < 0.5f) 587.33f else 880.0f // D5 to A5
            val decay = 1.0f - progress * 0.5f
            val sampleVal = sin(2.0 * Math.PI * freq * (i.toFloat() / sampleRate)) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playInteractFurnace() {
        if (!sfxEnabled) return
        vibrate(40)
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.65f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val hiss = (Random.nextFloat() * 2f - 1f) * 0.6f
            val rumble = sin(2.0 * Math.PI * 75f * (i.toFloat() / sampleRate)).toFloat() * 0.4f
            val decay = 1.0f - progress * 0.4f
            val sampleVal = (hiss + rumble) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    private fun vibrate(durationMs: Long) {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        isRunning = false
        ambientMusicJob?.cancel()
        trainSoundJob?.cancel()
        try {
            sfxAudioTrack?.stop()
            sfxAudioTrack?.release()
        } catch (_: Exception) {
        }
    }
}

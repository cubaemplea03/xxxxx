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
    var musicVolume: Float = 0.7f
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
    private val sfxQueue = ConcurrentLinkedQueue<ShortArray>()

    private val sampleRate = 22050
    private var sfxAudioTrack: AudioTrack? = null
    private var ambientAudioTrack: AudioTrack? = null
    private var isRunning = true

    init {
        initAudioTracks()
        startSfxWorker()
    }

    private fun initAudioTracks() {
        try {
            val sfxBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 8)

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
                .setBufferSizeInBytes(sfxBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            sfxAudioTrack?.play()

            // Dedicated Ambient track so music NEVER interferes with or delays SFX
            val ambientBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 4)

            ambientAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(ambientBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            ambientAudioTrack?.play()
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
                    delay(8)
                }
            }
        }
    }

    /**
     * Subtle, eerie post-apocalyptic ambient wind and low resonant drone
     * Streams smoothly in 250ms chunks directly to ambientAudioTrack without blocking SFX
     */
    fun startAmbientMusic() {
        ambientMusicJob?.cancel()
        ambientMusicJob = scope.launch {
            var phase1 = 0.0
            var phase2 = 0.0
            val chunkSize = (sampleRate * 0.25f).toInt()
            val chunkBuffer = ShortArray(chunkSize)

            while (isActive && isRunning) {
                if (musicEnabled && musicVolume > 0.05f && ambientAudioTrack != null) {
                    val vol = (musicVolume * 0.18f).coerceIn(0f, 1f)
                    val freq1 = 65.4f // C2 low sub-drone
                    val freq2 = 98.0f // G2 perfect fifth harmonic

                    for (i in 0 until chunkSize) {
                        phase1 += 2.0 * Math.PI * freq1 / sampleRate
                        phase2 += 2.0 * Math.PI * freq2 / sampleRate
                        if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                        if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI

                        // Wind noise + resonant deep tone
                        val windNoise = (Random.nextFloat() * 2f - 1f) * 0.25f
                        val tone = (sin(phase1) * 0.55 + sin(phase2) * 0.25).toFloat()
                        val sampleVal = (tone + windNoise) * Short.MAX_VALUE * vol
                        chunkBuffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    try {
                        ambientAudioTrack?.write(chunkBuffer, 0, chunkSize)
                    } catch (_: Exception) {
                    }
                } else {
                    delay(250)
                }
            }
        }
    }

    fun stopAmbientMusic() {
        ambientMusicJob?.cancel()
    }

    // No longer spams endless clacking! Kept as no-op or clean stub for compatibility
    fun startTrainRhythm() {
        // We do not play endless clicking loops anymore
    }

    fun stopTrainRhythm() {
        // Cleaned up
    }

    /**
     * Train arrival & braking sound: pneumatic air release + metallic disc hiss
     */
    fun playTrainBrake() {
        if (!sfxEnabled) return
        vibrate(80)
        scope.launch {
            val durationMs = 600
            val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            val vol = (sfxVolume * 0.55f).coerceIn(0f, 1f)

            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val t = i.toFloat() / sampleRate
                val airHiss = (Random.nextFloat() * 2f - 1f) * (1.0f - progress * 0.6f)
                val metallicSqueal = (sin(2.0 * Math.PI * (1200f - progress * 400f) * t) * 0.25).toFloat() * (1.0f - progress)
                val rumble = (sin(2.0 * Math.PI * 60f * t) * 0.4).toFloat() * (1.0f - progress)
                val sampleVal = (airHiss * 0.5f + metallicSqueal + rumble) * Short.MAX_VALUE * vol
                buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            sfxQueue.offer(buffer)
        }
    }

    /**
     * Train departure whistle
     */
    fun playTrainWhistle() {
        if (!sfxEnabled) return
        scope.launch {
            val durationMs = 700
            val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            val vol = (sfxVolume * 0.6f).coerceIn(0f, 1f)

            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val t = i.toFloat() / sampleRate
                val env = when {
                    progress < 0.15f -> progress / 0.15f
                    progress > 0.75f -> (1.0f - progress) / 0.25f
                    else -> 1.0f
                }
                val tone1 = sin(2.0 * Math.PI * 440f * t).toFloat() * 0.5f
                val tone2 = sin(2.0 * Math.PI * 554.37f * t).toFloat() * 0.5f // Major third
                val steam = (Random.nextFloat() * 2f - 1f) * 0.15f
                val sampleVal = (tone1 + tone2 + steam) * env * Short.MAX_VALUE * vol
                buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            sfxQueue.offer(buffer)
        }
    }

    /**
     * Crisp, punchy gunshot sound when player shoots
     */
    fun playGunshot() {
        if (!sfxEnabled) return
        vibrate(45)
        val durationMs = 110
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.85f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val t = i.toFloat() / sampleRate
            val noise = (Random.nextFloat() * 2f - 1f)
            val subThud = sin(2.0 * Math.PI * (180f - progress * 130f) * t).toFloat()
            val decay = (1.0f - progress) * (1.0f - progress)
            val sampleVal = (noise * 0.75f + subThud * 0.55f) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    /**
     * Train Gatling/Turret shot
     */
    fun playTrainTurretShoot() {
        if (!sfxEnabled) return
        val durationMs = 70
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.6f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val t = i.toFloat() / sampleRate
            val noise = (Random.nextFloat() * 2f - 1f)
            val click = sin(2.0 * Math.PI * 280f * t).toFloat()
            val decay = 1.0f - progress
            val sampleVal = (noise * 0.8f + click * 0.6f) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    /**
     * Train Cannon shot (heavy boom)
     */
    fun playTrainCannonShoot() {
        if (!sfxEnabled) return
        vibrate(85)
        val durationMs = 260
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.9f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val t = i.toFloat() / sampleRate
            val noise = (Random.nextFloat() * 2f - 1f) * 0.7f
            val lowBoom = sin(2.0 * Math.PI * (95f - progress * 65f) * t).toFloat() * 0.8f
            val decay = (1.0f - progress) * (1.0f - progress)
            val sampleVal = (noise + lowBoom) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    /**
     * Melee attack swing
     */
    fun playAttackSwing() {
        if (!sfxEnabled) return
        val durationMs = 70
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.5f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val noise = (Random.nextFloat() * 2f - 1f)
            val decay = (1.0f - progress)
            val sampleVal = noise * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    /**
     * Enemy hit impact
     */
    fun playEnemyHit() {
        if (!sfxEnabled) return
        vibrate(25)
        val durationMs = 50
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.65f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val t = i.toFloat() / sampleRate
            val thump = sin(2.0 * Math.PI * 160f * t).toFloat()
            val decay = 1.0f - progress
            val sampleVal = thump * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    /**
     * Enemy death sound: deep collapse and defeat
     */
    fun playEnemyDeath() {
        if (!sfxEnabled) return
        vibrate(40)
        val durationMs = 140
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.75f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val t = i.toFloat() / sampleRate
            val freq = 180f - progress * 110f
            val tone = sin(2.0 * Math.PI * freq * t).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f) * 0.35f
            val decay = 1.0f - progress
            val sampleVal = (tone + noise) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    /**
     * Gold coin pickup: bright, high-pitched double metallic chime
     */
    fun playGoldPickup() {
        if (!sfxEnabled) return
        vibrate(20)
        val durationMs = 130
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.7f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val t = i.toFloat() / sampleRate
            val freq = if (progress < 0.45f) 1318.5f else 1760.0f // E6 -> A6
            val decay = (1.0f - progress)
            val sampleVal = sin(2.0 * Math.PI * freq * t).toFloat() * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playItemPickup() {
        if (!sfxEnabled) return
        vibrate(20)
        val durationMs = 90
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.5f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val t = i.toFloat() / sampleRate
            val freq = 659.25f + progress * 220f
            val decay = 1.0f - progress
            val sampleVal = sin(2.0 * Math.PI * freq * t).toFloat() * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playPlayerHurt() {
        if (!sfxEnabled) return
        vibrate(90)
        val durationMs = 120
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.8f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val t = i.toFloat() / sampleRate
            val freq = 110f - progress * 40f
            val tone = sin(2.0 * Math.PI * freq * t).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f) * 0.4f
            val decay = 1.0f - progress
            val sampleVal = (tone + noise) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playJump() {
        if (!sfxEnabled) return
        val durationMs = 70
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.45f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = 200f + progress * 350f
            val decay = 1.0f - progress * 0.6f
            val sampleVal = sin(2.0 * Math.PI * freq * (i.toFloat() / sampleRate)) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playButtonClick() {
        if (!sfxEnabled) return
        vibrate(15)
        val durationMs = 30
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.4f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 520f
            val decay = 1.0f - (i.toFloat() / numSamples)
            val sampleVal = sin(2.0 * Math.PI * freq * t) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    fun playInteractFurnace() {
        if (!sfxEnabled) return
        vibrate(35)
        val durationMs = 150
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        val vol = (sfxVolume * 0.6f).coerceIn(0f, 1f)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val hiss = (Random.nextFloat() * 2f - 1f) * 0.5f
            val rumble = sin(2.0 * Math.PI * 70f * (i.toFloat() / sampleRate)).toFloat() * 0.5f
            val decay = 1.0f - progress * 0.5f
            val sampleVal = (hiss + rumble) * decay * Short.MAX_VALUE * vol
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        sfxQueue.offer(buffer)
    }

    /**
     * Triumphant level victory fanfare (4 ascending notes: C, E, G, C)
     */
    fun playLevelVictory() {
        if (!sfxEnabled) return
        scope.launch {
            val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f) // C5, E5, G5, C6
            for (freq in notes) {
                val durationMs = 100
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val buffer = ShortArray(numSamples)
                val vol = (sfxVolume * 0.7f).coerceIn(0f, 1f)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val progress = i.toFloat() / numSamples
                    val decay = 1.0f - progress * 0.4f
                    val sampleVal = sin(2.0 * Math.PI * freq * t) * decay * Short.MAX_VALUE * vol
                    buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                sfxQueue.offer(buffer)
                delay(90)
            }
        }
    }

    /**
     * Heavy, monstrous roar announcing the arrival of the Titan Boss
     */
    fun playBossRoar() {
        if (!sfxEnabled) return
        vibrate(250)
        scope.launch {
            val durationMs = 800
            val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            val vol = (sfxVolume * 0.95f).coerceIn(0f, 1f)

            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val t = i.toFloat() / sampleRate
                val freq = 85.0 - progress * 45.0 // Sub-rumble pitch drop
                val growlMod = sin(2.0 * Math.PI * 18.0 * t.toDouble()) // low flutter
                val dist = (sin(2.0 * Math.PI * freq * t.toDouble() + growlMod).toFloat() * 0.7f).coerceIn(-0.6f, 0.6f)
                val noise = (Random.nextFloat() * 2f - 1f) * 0.35f
                val env = sin(progress.toDouble() * Math.PI).toFloat()
                val sampleVal = (dist + noise) * env * Short.MAX_VALUE * vol
                buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            sfxQueue.offer(buffer)
        }
    }

    /**
     * Deep plasma blast sound when the Boss shoots
     */
    fun playBossShoot() {
        if (!sfxEnabled) return
        vibrate(90)
        scope.launch {
            val durationMs = 320
            val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            val vol = (sfxVolume * 0.9f).coerceIn(0f, 1f)

            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val t = i.toFloat() / sampleRate
                val freq = 260f - progress * 190f
                val plasmaHum = sin(2.0 * Math.PI * 45f * t)
                val tone = sin(2.0 * Math.PI * (freq + plasmaHum * 30f) * t).toFloat()
                val env = (1.0f - progress * progress)
                val sampleVal = tone * env * Short.MAX_VALUE * vol
                buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            sfxQueue.offer(buffer)
        }
    }

    /**
     * Dramatic orchestral/chime chord when the Titan Boss is destroyed
     */
    fun playBossDefeated() {
        if (!sfxEnabled) return
        vibrate(300)
        scope.launch {
            val notes = listOf(220f, 277f, 330f, 440f, 554f) // Epic A major chord fanfare
            for (freq in notes) {
                val durationMs = 280
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val buffer = ShortArray(numSamples)
                val vol = (sfxVolume * 0.75f).coerceIn(0f, 1f)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val t = i.toFloat() / sampleRate
                    val sampleVal = sin(2.0 * Math.PI * freq * t) * (1.0f - progress) * Short.MAX_VALUE * vol
                    buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                sfxQueue.offer(buffer)
                delay(80)
            }
        }
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
        try {
            sfxAudioTrack?.stop()
            sfxAudioTrack?.release()
            ambientAudioTrack?.stop()
            ambientAudioTrack?.release()
        } catch (_: Exception) {
        }
    }
}

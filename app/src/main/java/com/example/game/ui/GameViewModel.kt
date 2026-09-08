package com.example.game.ui

import android.app.Application
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.GameAudioEngine
import com.example.game.data.GameRepository
import com.example.game.data.GameSettingsEntity
import com.example.game.data.GameStatsEntity
import com.example.game.engine.GameEngine
import com.example.game.model.GameScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val repository = GameRepository(application)
    val audioEngine = GameAudioEngine(application)

    private val _screen = MutableStateFlow(GameScreen.SPLASH)
    val screen: StateFlow<GameScreen> = _screen.asStateFlow()

    private val _settings = MutableStateFlow(GameSettingsEntity())
    val settings: StateFlow<GameSettingsEntity> = _settings.asStateFlow()

    private val _stats = MutableStateFlow(GameStatsEntity())
    val stats: StateFlow<GameStatsEntity> = _stats.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _lastRunDistance = MutableStateFlow(0)
    val lastRunDistance: StateFlow<Int> = _lastRunDistance.asStateFlow()

    private val _lastRunScrap = MutableStateFlow(0)
    val lastRunScrap: StateFlow<Int> = _lastRunScrap.asStateFlow()

    private val _lastRunKills = MutableStateFlow(0)
    val lastRunKills: StateFlow<Int> = _lastRunKills.asStateFlow()

    val renderTick = mutableLongStateOf(0L)
    val hudTick = mutableLongStateOf(0L)

    val engine = GameEngine(
        audioEngine = audioEngine,
        onGameOver = { dist, scrap, kills ->
            triggerGameOver(dist, scrap, kills)
        }
    )

    init {
        // Load initial settings and stats
        viewModelScope.launch {
            repository.settingsFlow.collect { s ->
                val current = s ?: GameSettingsEntity()
                _settings.value = current
                audioEngine.musicEnabled = current.musicEnabled
                audioEngine.sfxEnabled = current.sfxEnabled
                audioEngine.musicVolume = current.musicVolume
                audioEngine.sfxVolume = current.sfxVolume
                audioEngine.vibrationEnabled = current.vibrationEnabled
                engine.graphicQuality = current.graphicQuality
            }
        }

        viewModelScope.launch {
            repository.statsFlow.collect { st ->
                _stats.value = st ?: GameStatsEntity()
            }
        }

        audioEngine.startAmbientMusic()
    }

    fun navigateTo(newScreen: GameScreen) {
        audioEngine.playButtonClick()
        _screen.value = newScreen
        if (newScreen == GameScreen.GAMEPLAY) {
            startGame()
        } else {
            resetInputs()
            audioEngine.stopTrainRhythm()
        }
    }

    fun startGame() {
        _isGameOver.value = false
        _isPaused.value = false
        resetInputs()
        engine.resetGame()
        renderTick.longValue = 0L
        hudTick.longValue = 0L
        audioEngine.startTrainRhythm()
    }

    fun updateGame(dt: Float) {
        if (_isPaused.value || _isGameOver.value || _screen.value != GameScreen.GAMEPLAY) return
        engine.update(dt)
        val next = renderTick.longValue + 1L
        renderTick.longValue = next
        if (next % 6L == 0L) {
            hudTick.longValue = next / 6L
        }
    }

    fun resetInputs() {
        engine.inputLeft = false
        engine.inputRight = false
        engine.inputCrouch = false
    }

    fun pauseGame() {
        _isPaused.value = true
        engine.isPaused = true
        resetInputs()
        audioEngine.stopTrainRhythm()
        audioEngine.playButtonClick()
    }

    fun resumeGame() {
        _isPaused.value = false
        engine.isPaused = false
        audioEngine.startTrainRhythm()
        audioEngine.playButtonClick()
    }

    fun retryGame() {
        audioEngine.playButtonClick()
        startGame()
    }

    private fun triggerGameOver(dist: Int, scrap: Int, kills: Int) {
        _isGameOver.value = true
        _lastRunDistance.value = dist
        _lastRunScrap.value = scrap
        _lastRunKills.value = kills
        resetInputs()
        audioEngine.stopTrainRhythm()

        viewModelScope.launch {
            repository.saveGameRun(dist, scrap, kills)
        }
    }

    // Input actions
    fun setMoveLeft(active: Boolean) {
        engine.inputLeft = active
    }

    fun setMoveRight(active: Boolean) {
        engine.inputRight = active
    }

    fun setCrouch(active: Boolean) {
        engine.inputCrouch = active
    }

    fun jump() {
        if (engine.player.isGrounded && !engine.player.isDead) {
            engine.player.vy = -380f
            engine.player.isGrounded = false
            audioEngine.playJump()
        }
    }

    fun attackMelee() {
        engine.performMeleeAttack()
    }

    fun attackShoot() {
        engine.performRangedAttack()
    }

    fun interact() {
        engine.performInteract()
    }

    // Settings
    fun updateMusicEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(musicEnabled = enabled)
        saveSettings(updated)
    }

    fun updateSfxEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(sfxEnabled = enabled)
        saveSettings(updated)
    }

    fun updateMusicVolume(vol: Float) {
        val updated = _settings.value.copy(musicVolume = vol)
        saveSettings(updated)
    }

    fun updateSfxVolume(vol: Float) {
        val updated = _settings.value.copy(sfxVolume = vol)
        saveSettings(updated)
    }

    fun updateGraphicQuality(quality: String) {
        val updated = _settings.value.copy(graphicQuality = quality)
        saveSettings(updated)
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(vibrationEnabled = enabled)
        saveSettings(updated)
    }

    fun updateControlSize(size: String) {
        val updated = _settings.value.copy(controlSize = size)
        saveSettings(updated)
    }

    fun updateControlOpacity(opacity: Float) {
        val updated = _settings.value.copy(controlOpacity = opacity)
        saveSettings(updated)
    }

    private fun saveSettings(s: GameSettingsEntity) {
        _settings.value = s
        audioEngine.musicEnabled = s.musicEnabled
        audioEngine.sfxEnabled = s.sfxEnabled
        audioEngine.musicVolume = s.musicVolume
        audioEngine.sfxVolume = s.sfxVolume
        audioEngine.vibrationEnabled = s.vibrationEnabled
        engine.graphicQuality = s.graphicQuality

        viewModelScope.launch {
            repository.saveSettings(s)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}

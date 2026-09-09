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

    private val _isLevelCompleted = MutableStateFlow(false)
    val isLevelCompleted: StateFlow<Boolean> = _isLevelCompleted.asStateFlow()

    private val _levelCompletedNumber = MutableStateFlow(1)
    val levelCompletedNumber: StateFlow<Int> = _levelCompletedNumber.asStateFlow()

    private val _levelEarnedGold = MutableStateFlow(0)
    val levelEarnedGold: StateFlow<Int> = _levelEarnedGold.asStateFlow()

    private val _levelEarnedScrap = MutableStateFlow(0)
    val levelEarnedScrap: StateFlow<Int> = _levelEarnedScrap.asStateFlow()

    private val _lastRunDistance = MutableStateFlow(0)
    val lastRunDistance: StateFlow<Int> = _lastRunDistance.asStateFlow()

    private val _lastRunScrap = MutableStateFlow(0)
    val lastRunScrap: StateFlow<Int> = _lastRunScrap.asStateFlow()

    private val _lastRunGold = MutableStateFlow(0)
    val lastRunGold: StateFlow<Int> = _lastRunGold.asStateFlow()

    private val _lastRunKills = MutableStateFlow(0)
    val lastRunKills: StateFlow<Int> = _lastRunKills.asStateFlow()

    private val _lastRunLevel = MutableStateFlow(0)
    val lastRunLevel: StateFlow<Int> = _lastRunLevel.asStateFlow()

    val renderTick = mutableLongStateOf(0L)
    val hudTick = mutableLongStateOf(0L)

    val engine = GameEngine(
        audioEngine = audioEngine,
        onGameOver = { dist, scrap, gold, kills, level ->
            triggerGameOver(dist, scrap, gold, kills, level)
        },
        onLevelComplete = { lvl, kills, gold, scrap ->
            triggerLevelComplete(lvl, kills, gold, scrap)
        },
        onPlayerLevelUp = { newLevel ->
            viewModelScope.launch {
                repository.savePlayerLevel(newLevel)
            }
        },
        onGoldEarned = { amount ->
            viewModelScope.launch {
                repository.addGold(amount)
            }
        },
        onMonsterSeen = { enemyType ->
            viewModelScope.launch {
                repository.markMonsterSeen(enemyType.name)
            }
        }
    )

    var previousScreenBeforeUpgrades: GameScreen = GameScreen.MAIN_MENU
        private set
    var previousScreenForSettings: GameScreen = GameScreen.MAIN_MENU
        private set

    init {
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
                val current = st ?: GameStatsEntity()
                _stats.value = current
                engine.applyUpgrades(current)
            }
        }

        audioEngine.startAmbientMusic()
    }

    fun openUpgradesFrom(origin: GameScreen) {
        previousScreenBeforeUpgrades = origin
        viewModelScope.launch {
            repository.savePlayerLevel(engine.playerSurvivalLevel)
        }
        audioEngine.playButtonClick()
        _screen.value = GameScreen.UPGRADES
        resetInputs()
    }

    fun closeUpgrades() {
        audioEngine.playButtonClick()
        if (previousScreenBeforeUpgrades == GameScreen.GAMEPLAY) {
            _screen.value = GameScreen.GAMEPLAY
            _isPaused.value = true
            engine.isPaused = true
            resetInputs()
        } else {
            _screen.value = GameScreen.MAIN_MENU
            resetInputs()
        }
    }

    fun openSettingsFrom(origin: GameScreen) {
        previousScreenForSettings = origin
        audioEngine.playButtonClick()
        _screen.value = GameScreen.SETTINGS
        resetInputs()
    }

    fun closeSettings() {
        audioEngine.playButtonClick()
        if (previousScreenForSettings == GameScreen.GAMEPLAY) {
            _screen.value = GameScreen.GAMEPLAY
            _isPaused.value = true
            engine.isPaused = true
            resetInputs()
        } else {
            _screen.value = GameScreen.MAIN_MENU
            resetInputs()
        }
    }

    fun navigateTo(newScreen: GameScreen) {
        audioEngine.playButtonClick()
        if (newScreen == GameScreen.UPGRADES) {
            previousScreenBeforeUpgrades = _screen.value
            viewModelScope.launch {
                repository.savePlayerLevel(engine.playerSurvivalLevel)
            }
        } else if (newScreen == GameScreen.SETTINGS) {
            previousScreenForSettings = _screen.value
        }
        _screen.value = newScreen
        if (newScreen == GameScreen.GAMEPLAY) {
            if (_isGameOver.value) {
                startGame()
            }
        } else {
            resetInputs()
        }
    }

    fun startGame() {
        startSurvivalGame(engine.selectedMapId)
    }

    fun startSurvivalGame(mapId: Int = 1) {
        _screen.value = GameScreen.GAMEPLAY
        _isGameOver.value = false
        _isPaused.value = false
        _isLevelCompleted.value = false
        resetInputs()
        engine.applyUpgrades(_stats.value)
        val finalMap = if (mapId <= 0) (1..3).random() else mapId
        engine.resetGame(finalMap, initialGold = _stats.value.totalGold)
        renderTick.longValue = 0L
        hudTick.longValue = 0L
    }

    fun nextLevel() {
        _isLevelCompleted.value = false
        resetInputs()
        engine.applyUpgrades(_stats.value)
        engine.startNextLevel()
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
        audioEngine.playButtonClick()
    }

    fun resumeGame() {
        _isPaused.value = false
        engine.isPaused = false
        audioEngine.playButtonClick()
    }

    fun retryGame() {
        audioEngine.playButtonClick()
        startSurvivalGame(engine.selectedMapId)
    }

    private fun triggerGameOver(dist: Int, scrap: Int, gold: Int, kills: Int, level: Int) {
        _isGameOver.value = true
        _lastRunDistance.value = dist
        _lastRunScrap.value = scrap
        _lastRunGold.value = gold
        _lastRunKills.value = kills
        _lastRunLevel.value = level
        resetInputs()

        viewModelScope.launch {
            repository.saveGameRun(dist, scrap, gold, kills, level)
        }
    }

    private fun triggerLevelComplete(lvl: Int, kills: Int, gold: Int, scrap: Int) {
        _isLevelCompleted.value = true
        _levelCompletedNumber.value = lvl
        _levelEarnedGold.value = gold
        _levelEarnedScrap.value = scrap
        resetInputs()

        viewModelScope.launch {
            repository.saveGameRun(engine.train.distance.toInt(), scrap, gold, kills, lvl)
        }
    }

    fun buyTrainUpgrade(upgradeId: String, cost: Int) {
        viewModelScope.launch {
            val success = repository.purchaseTrainUpgrade(upgradeId, cost)
            if (success) {
                audioEngine.playGoldPickup()
            }
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

    fun purchaseSkin(skin: com.example.game.model.WardrobeSkinItem, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.purchaseSkin(
                skinId = skin.id,
                cost = skin.costGold,
                slotName = skin.slot.name
            )
            if (success) {
                audioEngine.playGoldPickup()
                when (skin.slot) {
                    com.example.game.model.WardrobeSlot.CABEZA -> engine.player.headSkin = skin.id
                    com.example.game.model.WardrobeSlot.PECHO -> engine.player.chestSkin = skin.id
                    com.example.game.model.WardrobeSlot.PIERNAS -> engine.player.legsSkin = skin.id
                }
            } else {
                audioEngine.playButtonClick()
            }
            onResult(success)
        }
    }

    fun equipSkin(skin: com.example.game.model.WardrobeSkinItem) {
        viewModelScope.launch {
            repository.equipSkin(
                skinId = skin.id,
                slotName = skin.slot.name
            )
            audioEngine.playButtonClick()
            when (skin.slot) {
                com.example.game.model.WardrobeSlot.CABEZA -> engine.player.headSkin = skin.id
                com.example.game.model.WardrobeSlot.PECHO -> engine.player.chestSkin = skin.id
                com.example.game.model.WardrobeSlot.PIERNAS -> engine.player.legsSkin = skin.id
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}

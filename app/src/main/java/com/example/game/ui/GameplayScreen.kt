package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.example.game.model.GameScreen
import kotlinx.coroutines.isActive

@Composable
fun GameplayScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val isPaused by viewModel.isPaused.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()
    val isLevelCompleted by viewModel.isLevelCompleted.collectAsState()
    val levelNumber by viewModel.levelCompletedNumber.collectAsState()
    val levelEarnedGold by viewModel.levelEarnedGold.collectAsState()
    val levelEarnedScrap by viewModel.levelEarnedScrap.collectAsState()

    val settings by viewModel.settings.collectAsState()
    val lastDistance by viewModel.lastRunDistance.collectAsState()
    val lastScrap by viewModel.lastRunScrap.collectAsState()
    val lastGold by viewModel.lastRunGold.collectAsState()
    val lastKills by viewModel.lastRunKills.collectAsState()
    val lastLevel by viewModel.lastRunLevel.collectAsState()

    // 60FPS Game loop synced with hardware display VSYNC
    LaunchedEffect(isPaused, isGameOver, isLevelCompleted) {
        if (!isPaused && !isGameOver && !isLevelCompleted) {
            var lastFrameNanos = 0L
            while (isActive) {
                withFrameNanos { frameNanos ->
                    if (lastFrameNanos != 0L) {
                        val dt = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                        viewModel.updateGame(dt)
                    }
                    lastFrameNanos = frameNanos
                }
            }
        }
    }

    // Back press pauses game
    BackHandler {
        if (!isPaused && !isGameOver && !isLevelCompleted) {
            viewModel.pauseGame()
        } else if (isPaused) {
            viewModel.resumeGame()
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (isPaused || isGameOver || isLevelCompleted) return@onKeyEvent false
                val isDown = keyEvent.type == KeyEventType.KeyDown
                val isUp = keyEvent.type == KeyEventType.KeyUp
                when (keyEvent.key) {
                    Key.DirectionLeft, Key.A -> {
                        if (isDown) viewModel.setMoveLeft(true)
                        else if (isUp) viewModel.setMoveLeft(false)
                        true
                    }
                    Key.DirectionRight, Key.D -> {
                        if (isDown) viewModel.setMoveRight(true)
                        else if (isUp) viewModel.setMoveRight(false)
                        true
                    }
                    Key.DirectionDown, Key.S -> {
                        if (isDown) viewModel.setCrouch(true)
                        else if (isUp) viewModel.setCrouch(false)
                        true
                    }
                    Key.DirectionUp, Key.W, Key.Spacebar -> {
                        if (isDown) viewModel.jump()
                        true
                    }
                    Key.J, Key.Z -> {
                        if (isDown) viewModel.attackMelee()
                        true
                    }
                    Key.K, Key.X -> {
                        if (isDown) viewModel.attackShoot()
                        true
                    }
                    Key.E, Key.C -> {
                        if (isDown) viewModel.interact()
                        true
                    }
                    Key.Escape, Key.P -> {
                        if (isDown) viewModel.pauseGame()
                        true
                    }
                    else -> false
                }
            }
    ) {
        // 1. Core 2D Game Canvas
        GameCanvas(
            engine = viewModel.engine,
            renderTickProvider = { viewModel.renderTick.longValue },
            modifier = Modifier.fillMaxSize()
        )

        // 2. HUD
        GameHud(
            engine = viewModel.engine,
            hudTick = viewModel.hudTick.longValue,
            onPauseClick = { viewModel.pauseGame() }
        )

        // 3. Virtual Mobile Controls (Landscape)
        if (!isPaused && !isGameOver && !isLevelCompleted) {
            GameControls(
                settings = settings,
                onLeftPress = { active -> viewModel.setMoveLeft(active) },
                onRightPress = { active -> viewModel.setMoveRight(active) },
                onCrouchPress = { active -> viewModel.setCrouch(active) },
                onJump = { viewModel.jump() },
                onMeleeAttack = { viewModel.attackMelee() },
                onShootAttack = { viewModel.attackShoot() },
                onInteract = { viewModel.interact() },
                ammoCount = viewModel.engine.player.ammo
            )
        }

        // 4. Pause Overlay Dialog
        if (isPaused) {
            PauseDialog(
                distance = viewModel.engine.train.distance.toInt(),
                onResume = { viewModel.resumeGame() },
                onSettings = { viewModel.openSettingsFrom(GameScreen.GAMEPLAY) },
                onUpgrades = { viewModel.openUpgradesFrom(GameScreen.GAMEPLAY) },
                onMainMenu = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
            )
        }

        // 5. Game Over Overlay Dialog
        if (isGameOver) {
            GameOverDialog(
                distance = lastDistance,
                scrap = lastScrap,
                gold = lastGold,
                kills = lastKills,
                level = lastLevel,
                onRetry = { viewModel.retryGame() },
                onUpgrades = { viewModel.openUpgradesFrom(GameScreen.MAIN_MENU) },
                onMainMenu = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
            )
        }

        // 6. Level Complete Victory Dialog
        if (isLevelCompleted) {
            LevelCompleteDialog(
                level = levelNumber,
                goldEarned = levelEarnedGold,
                scrapEarned = levelEarnedScrap,
                targetEnemies = viewModel.engine.totalEnemiesTarget,
                onNextLevel = { viewModel.nextLevel() },
                onUpgrades = { viewModel.openUpgradesFrom(GameScreen.GAMEPLAY) },
                onMainMenu = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
            )
        }
    }
}

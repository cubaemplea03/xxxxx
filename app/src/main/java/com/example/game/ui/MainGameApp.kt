package com.example.game.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.game.model.GameScreen

@Composable
fun MainGameApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.screen.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val stats by viewModel.stats.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(350),
            label = "screen_crossfade"
        ) { screen ->
            when (screen) {
                GameScreen.SPLASH -> {
                    SplashScreen(
                        onContinue = {
                            viewModel.navigateTo(GameScreen.MAIN_MENU)
                        }
                    )
                }
                GameScreen.MAIN_MENU -> {
                    MainMenuScreen(
                        stats = stats,
                        onPlayClick = { mapId ->
                            viewModel.startSurvivalGame(mapId)
                        },
                        onUpgradesClick = {
                            viewModel.openUpgradesFrom(GameScreen.MAIN_MENU)
                        },
                        onSettingsClick = {
                            viewModel.openSettingsFrom(GameScreen.MAIN_MENU)
                        },
                        onCreditsClick = {
                            viewModel.navigateTo(GameScreen.CREDITS)
                        },
                        onGuideClick = {
                            viewModel.navigateTo(GameScreen.MONSTER_GUIDE)
                        },
                        onWardrobeClick = {
                            viewModel.navigateTo(GameScreen.WARDROBE)
                        }
                    )
                }
                GameScreen.GAMEPLAY -> {
                    GameplayScreen(viewModel = viewModel)
                }
                GameScreen.UPGRADES -> {
                    TrainUpgradesScreen(
                        viewModel = viewModel,
                        onBack = {
                            viewModel.closeUpgrades()
                        }
                    )
                }
                GameScreen.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onMusicEnabledChange = { viewModel.updateMusicEnabled(it) },
                        onSfxEnabledChange = { viewModel.updateSfxEnabled(it) },
                        onMusicVolumeChange = { viewModel.updateMusicVolume(it) },
                        onSfxVolumeChange = { viewModel.updateSfxVolume(it) },
                        onGraphicQualityChange = { viewModel.updateGraphicQuality(it) },
                        onVibrationEnabledChange = { viewModel.updateVibrationEnabled(it) },
                        onControlSizeChange = { viewModel.updateControlSize(it) },
                        onControlOpacityChange = { viewModel.updateControlOpacity(it) },
                        onBackClick = {
                            viewModel.closeSettings()
                        }
                    )
                }
                GameScreen.CREDITS -> {
                    CreditsScreen(
                        onBackClick = {
                            viewModel.navigateTo(GameScreen.MAIN_MENU)
                        }
                    )
                }
                GameScreen.MONSTER_GUIDE -> {
                    MonsterGuideScreen(
                        stats = stats,
                        onBackClick = {
                            viewModel.navigateTo(GameScreen.MAIN_MENU)
                        }
                    )
                }
                GameScreen.WARDROBE -> {
                    WardrobeScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            viewModel.navigateTo(GameScreen.MAIN_MENU)
                        }
                    )
                }
            }
        }
    }
}

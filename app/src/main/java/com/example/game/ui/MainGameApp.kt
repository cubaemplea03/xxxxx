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
            animationSpec = tween(400),
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
                        onPlayClick = {
                            viewModel.navigateTo(GameScreen.GAMEPLAY)
                        },
                        onSettingsClick = {
                            viewModel.navigateTo(GameScreen.SETTINGS)
                        },
                        onCreditsClick = {
                            viewModel.navigateTo(GameScreen.CREDITS)
                        }
                    )
                }
                GameScreen.GAMEPLAY -> {
                    GameplayScreen(viewModel = viewModel)
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
                            viewModel.navigateTo(GameScreen.MAIN_MENU)
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
            }
        }
    }
}

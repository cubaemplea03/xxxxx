package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.GameSettingsEntity

@Composable
fun SettingsScreen(
    settings: GameSettingsEntity,
    onMusicEnabledChange: (Boolean) -> Unit,
    onSfxEnabledChange: (Boolean) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onGraphicQualityChange: (String) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onControlSizeChange: (String) -> Unit,
    onControlOpacityChange: (Float) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF090E13), Color(0xFF141D26), Color(0xFF1C2733))
                )
            )
            .padding(horizontal = 32.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Row: Title & Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AJUSTES",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "• Guardado automático",
                        fontSize = 11.sp,
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF263238),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                        .testTag("btn_settings_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VOLVER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Two Columns Layout in Landscape
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Column 1: Audio Settings
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x88151D24))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    SettingSectionTitle(icon = Icons.Default.MusicNote, title = "AUDIO Y MÚSICA")

                    // Music Switch
                    SettingToggleRow(
                        label = "Música de fondo",
                        checked = settings.musicEnabled,
                        onCheckedChange = onMusicEnabledChange
                    )

                    // Music Volume Slider
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Volumen Música", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                            Text(text = "${(settings.musicVolume * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.musicVolume,
                            onValueChange = onMusicVolumeChange,
                            enabled = settings.musicEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFB300),
                                activeTrackColor = Color(0xFFFFB300),
                                inactiveTrackColor = Color(0xFF37474F)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SFX Switch
                    SettingToggleRow(
                        label = "Efectos de sonido (SFX)",
                        checked = settings.sfxEnabled,
                        onCheckedChange = onSfxEnabledChange
                    )

                    // SFX Volume Slider
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Volumen Efectos", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                            Text(text = "${(settings.sfxVolume * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.sfxVolume,
                            onValueChange = onSfxVolumeChange,
                            enabled = settings.sfxEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFB300),
                                activeTrackColor = Color(0xFFFFB300),
                                inactiveTrackColor = Color(0xFF37474F)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Column 2: Graphics, Haptics & Mobile Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x88151D24))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    SettingSectionTitle(icon = Icons.Default.GraphicEq, title = "GRÁFICOS Y CONTROLES")

                    // Graphic Quality
                    Text(text = "Calidad Gráfica", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BAJA", "MEDIA", "ALTA").forEach { q ->
                            FilterChip(
                                selected = settings.graphicQuality == q,
                                onClick = { onGraphicQualityChange(q) },
                                label = { Text(q, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFB300),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vibration Toggle
                    SettingToggleRow(
                        label = "Vibración al golpear / recibir daño",
                        checked = settings.vibrationEnabled,
                        onCheckedChange = onVibrationEnabledChange
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Touch Controls Size
                    Text(text = "Tamaño de Controles Táctiles", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("NORMAL", "GRANDE").forEach { size ->
                            FilterChip(
                                selected = settings.controlSize == size,
                                onClick = { onControlSizeChange(size) },
                                label = { Text(size, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF26A69A),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opacity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Opacidad de Controles", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                        Text(text = "${(settings.controlOpacity * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFF26A69A), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.controlOpacity,
                        onValueChange = onControlOpacityChange,
                        valueRange = 0.4f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF26A69A),
                            activeTrackColor = Color(0xFF26A69A),
                            inactiveTrackColor = Color(0xFF37474F)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSectionTitle(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFFECEFF1), fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFFFFB300),
                uncheckedThumbColor = Color(0xFF90A4AE),
                uncheckedTrackColor = Color(0xFF263238)
            )
        )
    }
}

package com.example.game.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.GameStatsEntity

@Composable
fun MainMenuScreen(
    stats: GameStatsEntity,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreditsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_anim")
    val ambientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_shift"
    )
    val lightPulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light_pulse"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Atmospheric Canvas background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Night Sky
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF070B0E), Color(0xFF131A21), Color(0xFF1E2833))
                ),
                size = Size(w, h)
            )

            // Twinkling stars
            for (i in 0..25) {
                val sx = (i * 73f + ambientShift * 2f) % w
                val sy = (i * 37f) % (h * 0.45f)
                val alpha = ((i % 5 + 1) * 0.15f * lightPulse).coerceIn(0.1f, 1f)
                drawCircle(color = Color.White.copy(alpha = alpha), radius = 1.8f, center = Offset(sx, sy))
            }

            // Distant Mountains
            val mPath = Path().apply {
                moveTo(0f, h * 0.65f)
                lineTo(w * 0.2f, h * 0.48f)
                lineTo(w * 0.45f, h * 0.62f)
                lineTo(w * 0.7f, h * 0.42f)
                lineTo(w * 0.9f, h * 0.58f)
                lineTo(w, h * 0.5f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(mPath, color = Color(0xFF11171E))

            // Ground & Railway
            val railY = h * 0.78f
            drawRect(color = Color(0xFF191F24), topLeft = Offset(0f, railY), size = Size(w, h - railY))
            drawLine(
                color = Color(0xFF546E7A),
                start = Offset(0f, railY + 2f),
                end = Offset(w, railY + 2f),
                strokeWidth = 3f
            )

            // Locomotive waiting on tracks on the right side
            val locoX = w * 0.55f
            val locoY = railY - 95f
            drawRect(
                color = Color(0xFF263238),
                topLeft = Offset(locoX, locoY),
                size = Size(280f, 95f)
            )
            // Headlight
            val hlX = locoX + 280f
            val hlY = locoY + 35f
            drawCircle(color = Color(0xFFFFD54F), radius = 8f * lightPulse, center = Offset(hlX, hlY))
            // Conical headlight beam
            val beam = Path().apply {
                moveTo(hlX, hlY)
                lineTo(w, hlY - 60f)
                lineTo(w, hlY + 80f)
                close()
            }
            drawPath(
                path = beam,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x66FFE082), Color.Transparent),
                    startX = hlX,
                    endX = w
                )
            )

            // Chimney smoke
            for (p in 0..4) {
                val px = locoX + 220f - p * 35f
                val py = locoY - 20f - p * 12f
                drawCircle(color = Color(0x33455A64), radius = 18f + p * 8f, center = Offset(px, py))
            }

            // Foreground fog
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x660B0F13)),
                    startY = h * 0.6f,
                    endY = h
                ),
                size = Size(w, h)
            )
        }

        // Left-Center: Brand Title & Menu Buttons
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // Game Logo Title
                Text(
                    text = "ÚLTIMO TREN",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFFFFB300).copy(alpha = 0.8f * lightPulse),
                            offset = Offset(0f, 0f),
                            blurRadius = 26f * lightPulse
                        )
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ACCIÓN Y SUPERVIVENCIA EN LAS VÍAS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color(0xFFFFCA28)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Chip if player has played
                if (stats.highScoreDistance > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x881E2833))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Récord",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Récord: ${stats.highScoreDistance} m  |  Chatarra: ${stats.totalScrap}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFECEFF1)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Primary JUGAR Button
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .width(260.dp)
                        .height(54.dp)
                        .border(1.5.dp, Color(0xFFFFF9C4), RoundedCornerShape(14.dp))
                        .testTag("btn_play")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Jugar",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "JUGAR",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // AJUSTES Button
                    OutlinedButton(
                        onClick = onSettingsClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xAA1C262F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(124.dp)
                            .height(44.dp)
                            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                            .testTag("btn_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = Color(0xFFECEFF1),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AJUSTES",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // CRÉDITOS Button
                    OutlinedButton(
                        onClick = onCreditsClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xAA1C262F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(124.dp)
                            .height(44.dp)
                            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                            .testTag("btn_credits")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Créditos",
                            tint = Color(0xFFECEFF1),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CRÉDITOS",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Right side spacer for locomotive visual composition
            Spacer(modifier = Modifier.weight(0.9f))
        }
    }
}

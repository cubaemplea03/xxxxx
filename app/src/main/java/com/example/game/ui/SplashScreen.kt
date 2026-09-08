package com.example.game.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Cinematic animation sequence states
    var lightIntensity by remember { mutableFloatStateOf(0f) }
    var trainOffsetProgress by remember { mutableFloatStateOf(0f) }
    var titleAlpha by remember { mutableFloatStateOf(0f) }
    var canContinue by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Step 1: Pitch black initial delay
        delay(400)

        // Step 2 & 3: Locomotive light slowly appears & train enters
        val startTime = System.currentTimeMillis()
        val enterDuration = 2200L
        while (System.currentTimeMillis() - startTime < enterDuration) {
            val elapsed = System.currentTimeMillis() - startTime
            val fraction = (elapsed.toFloat() / enterDuration).coerceIn(0f, 1f)
            lightIntensity = fraction
            trainOffsetProgress = fraction
            delay(16)
        }
        lightIntensity = 1f
        trainOffsetProgress = 1f

        // Step 4: Title fades in with cinematic weight
        val titleStart = System.currentTimeMillis()
        val titleDuration = 1000L
        while (System.currentTimeMillis() - titleStart < titleDuration) {
            val elapsed = System.currentTimeMillis() - titleStart
            titleAlpha = (elapsed.toFloat() / titleDuration).coerceIn(0f, 1f)
            delay(16)
        }
        titleAlpha = 1f

        // Step 6: "TOCA PARA CONTINUAR" appears
        delay(600)
        canContinue = true
    }

    // Continuous breathing / glow animation for title
    val infiniteTransition = rememberInfiniteTransition(label = "splash_loop")
    val titleGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_glow"
    )

    val promptAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "prompt_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (canContinue) {
                    onContinue()
                }
            }
            .testTag("splash_screen")
    ) {
        // Dynamic Canvas for Dark Night, Train entering, Headlight cone, Smoke & Fog
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Dark night background with subtle stormy gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF070B0E), Color(0xFF10171D), Color(0xFF18222A)),
                    startY = 0f,
                    endY = h
                ),
                size = Size(w, h)
            )

            // Abandoned distant hills silhouette
            val hillPath = Path().apply {
                moveTo(0f, h * 0.72f)
                lineTo(w * 0.25f, h * 0.58f)
                lineTo(w * 0.55f, h * 0.68f)
                lineTo(w * 0.82f, h * 0.52f)
                lineTo(w, h * 0.65f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hillPath, color = Color(0xFF0D1217))

            // Ground & Railway tracks
            val railY = h * 0.75f
            drawRect(color = Color(0xFF161A1E), topLeft = Offset(0f, railY), size = Size(w, h - railY))
            drawLine(
                color = Color(0xFF546E7A),
                start = Offset(0f, railY + 4f),
                end = Offset(w, railY + 4f),
                strokeWidth = 3f
            )

            // 2. Train entering from left
            // Final train head is around w * 0.42f
            val trainX = -w * 0.4f + (w * 0.75f) * trainOffsetProgress
            val trainY = railY - 110f
            val trainW = 380f
            val trainH = 110f

            // Train locomotive silhouette
            drawRect(
                color = Color(0xFF1E282E),
                topLeft = Offset(trainX, trainY),
                size = Size(trainW, trainH)
            )
            // Locomotive cabin
            drawRect(
                color = Color(0xFF151C20),
                topLeft = Offset(trainX + 20f, trainY - 25f),
                size = Size(100f, 135f)
            )
            // Chimney with billowing smoke
            drawRect(
                color = Color(0xFF151C20),
                topLeft = Offset(trainX + trainW - 80f, trainY - 35f),
                size = Size(25f, 35f)
            )

            // Billowing smoke puffs behind chimney
            val time = System.currentTimeMillis() * 0.003f
            for (s in 0..5) {
                val puffX = trainX + trainW - 80f - (s * 45f) - (time * 10f % 40f)
                val puffY = trainY - 45f - s * 12f
                val puffR = (20f + s * 9f) * lightIntensity
                drawCircle(
                    color = Color(0x33455A64),
                    radius = puffR,
                    center = Offset(puffX, puffY)
                )
            }

            // Wheels
            for (wh in 0..3) {
                drawCircle(
                    color = Color(0xFF101417),
                    radius = 18f,
                    center = Offset(trainX + 60f + wh * 85f, railY - 2f)
                )
            }

            // 3. Locomotive Headlight and Beam
            val headLightPos = Offset(trainX + trainW + 5f, trainY + 45f)
            if (lightIntensity > 0.05f) {
                // Core light bulb
                drawCircle(
                    color = Color(0xFFFFF9C4).copy(alpha = lightIntensity),
                    radius = 10f,
                    center = headLightPos
                )

                // Dramatic cinematic light beam cutting through mist
                val beam = Path().apply {
                    moveTo(headLightPos.x, headLightPos.y)
                    lineTo(w, headLightPos.y - 140f)
                    lineTo(w, headLightPos.y + 160f)
                    close()
                }
                drawPath(
                    path = beam,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x88FFF176).copy(alpha = 0.85f * lightIntensity),
                            Color(0x22FFF59D).copy(alpha = 0.4f * lightIntensity),
                            Color.Transparent
                        ),
                        startX = headLightPos.x,
                        endX = w
                    )
                )
            }

            // Ambient drifting mist particles
            for (m in 0..3) {
                val mx = (System.currentTimeMillis() * 0.03f * (m + 1)) % w
                drawOval(
                    color = Color(0x18B0BEC5),
                    topLeft = Offset(mx - 150f, h * 0.55f + m * 20f),
                    size = Size(300f, 60f)
                )
            }
        }

        // Title and Subtitle Overlay
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ÚLTIMO TREN",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFECEFF1).copy(alpha = titleAlpha),
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFFFB300).copy(alpha = 0.7f * titleGlow * titleAlpha),
                        offset = Offset(0f, 0f),
                        blurRadius = 24f * titleGlow
                    )
                ),
                modifier = Modifier.alpha(titleAlpha)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "SOBREVIVE EN LAS VÍAS HACIA LO DESCONOCIDO",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                color = Color(0xFFFFCA28).copy(alpha = 0.85f * titleAlpha),
                modifier = Modifier.alpha(titleAlpha)
            )

            Spacer(modifier = Modifier.height(34.dp))

            if (canContinue) {
                Text(
                    text = "— TOCA PARA CONTINUAR —",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = Color.White.copy(alpha = promptAlpha)
                )
            }
        }
    }
}

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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.GameStatsEntity

@Composable
fun MainMenuScreen(
    stats: GameStatsEntity,
    onPlayClick: (mapId: Int) -> Unit,
    onUpgradesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreditsClick: () -> Unit,
    onGuideClick: () -> Unit,
    onWardrobeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMultiplayerDialog by remember { mutableStateOf(false) }
    var showMapSelectionDialog by remember { mutableStateOf(false) }

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
            // Ballast bed
            drawRect(color = Color(0xFF15191D), topLeft = Offset(0f, railY), size = Size(w, h - railY))
            // Railroad ties
            var mtx = 0f
            while (mtx < w) {
                drawRect(color = Color(0xFF2A211B), topLeft = Offset(mtx, railY + 3f), size = Size(10f, 10f))
                drawRect(color = Color(0xFF37474F), topLeft = Offset(mtx + 1f, railY + 1.5f), size = Size(8f, 2f))
                mtx += 24f
            }
            // Steel rail
            drawRect(color = Color(0xFF263238), topLeft = Offset(0f, railY + 2f), size = Size(w, 2f))
            drawRect(color = Color(0xFF90A4AE), topLeft = Offset(0f, railY), size = Size(w, 1.5f))
            drawRect(color = Color(0xFFFFFFFF), topLeft = Offset(0f, railY), size = Size(w, 0.6f))

            // Train on tracks on the right side: Tender + Heavy Steam Locomotive
            val tenderX = w * 0.44f
            val tenderW = 100f
            val tenderH = 65f
            val tenderY = railY - tenderH

            // Coal Tender
            drawRect(color = Color(0xFF202930), topLeft = Offset(tenderX, tenderY), size = Size(tenderW, tenderH))
            val mCoal = Path().apply {
                moveTo(tenderX + 4f, tenderY)
                lineTo(tenderX + 30f, tenderY - 12f)
                lineTo(tenderX + 60f, tenderY - 14f)
                lineTo(tenderX + tenderW - 4f, tenderY)
                close()
            }
            drawPath(mCoal, color = Color(0xFF101416))
            // Tender wheels
            drawCircle(color = Color(0xFF151B1F), radius = 8f, center = Offset(tenderX + 22f, railY - 4f))
            drawCircle(color = Color(0xFF151B1F), radius = 8f, center = Offset(tenderX + 38f, railY - 4f))
            drawCircle(color = Color(0xFF151B1F), radius = 8f, center = Offset(tenderX + 68f, railY - 4f))
            drawCircle(color = Color(0xFF151B1F), radius = 8f, center = Offset(tenderX + 84f, railY - 4f))

            // Coupler connecting tender to locomotive
            drawRect(color = Color(0xFF12171A), topLeft = Offset(tenderX + tenderW, railY - 12f), size = Size(14f, 6f))

            // Locomotive
            val locoX = tenderX + tenderW + 10f
            val locoW = 280f
            val locoCabW = 75f
            val locoCabH = 95f
            val locoCabY = railY - locoCabH
            val boilerY = railY - 78f
            val boilerH = 74f
            val boilerW = locoW - locoCabW - 15f
            val boilerX = locoX + locoCabW

            // 1. Cab
            drawRect(color = Color(0xFF1C242A), topLeft = Offset(locoX, locoCabY), size = Size(locoCabW, locoCabH))
            // Cab roof overhang
            val mCabRoof = Path().apply {
                moveTo(locoX - 4f, locoCabY + 4f)
                quadraticTo(locoX + locoCabW * 0.5f, locoCabY - 4f, locoX + locoCabW + 4f, locoCabY + 4f)
                lineTo(locoX + locoCabW + 3f, locoCabY + 6f)
                lineTo(locoX - 3f, locoCabY + 6f)
                close()
            }
            drawPath(mCabRoof, color = Color(0xFF12171B))
            // Cab Window with warm golden light
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFFF8F00)),
                    center = Offset(locoX + 35f, locoCabY + 24f),
                    radius = 24f
                ),
                topLeft = Offset(locoX + 22f, locoCabY + 12f),
                size = Size(26f, 24f)
            )
            // Window mullion
            drawLine(color = Color(0xFF212121), start = Offset(locoX + 35f, locoCabY + 12f), end = Offset(locoX + 35f, locoCabY + 36f), strokeWidth = 2f)

            // 2. Boiler with cylindrical metallic gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF192126), Color(0xFF37474F), Color(0xFF455A64), Color(0xFF263238), Color(0xFF13181C)),
                    startY = boilerY,
                    endY = railY
                ),
                topLeft = Offset(boilerX, boilerY),
                size = Size(boilerW, boilerH)
            )
            // Brass boiler bands
            for (bb in listOf(0.2f, 0.45f, 0.7f, 0.92f)) {
                val bbx = boilerX + boilerW * bb
                drawRect(color = Color(0xFFB58E34), topLeft = Offset(bbx, boilerY), size = Size(3f, boilerH))
            }
            // Running board & handrail
            drawLine(color = Color(0xFFFFCA28), start = Offset(boilerX, boilerY + 38f), end = Offset(boilerX + boilerW, boilerY + 38f), strokeWidth = 2f)

            // 3. Smokebox & Cap
            val sboxW = 40f
            val sboxX = boilerX + boilerW - sboxW
            drawRect(color = Color(0xFF14191D), topLeft = Offset(sboxX, boilerY), size = Size(sboxW, boilerH))
            val mCap = Path().apply {
                moveTo(boilerX + boilerW, boilerY)
                quadraticTo(boilerX + boilerW + 14f, boilerY + boilerH * 0.5f, boilerX + boilerW, railY)
                close()
            }
            drawPath(mCap, color = Color(0xFF111518))

            // 4. Steam Dome & Sand Dome & Chimney
            val dome1X = boilerX + boilerW * 0.28f
            drawCircle(color = Color(0xFF37474F), radius = 10f, center = Offset(dome1X, boilerY))
            val dome2X = boilerX + boilerW * 0.55f
            drawCircle(color = Color(0xFF37474F), radius = 8f, center = Offset(dome2X, boilerY))

            // Smokestack with flared rim
            val mStackX = sboxX + 18f
            val mStackPath = Path().apply {
                moveTo(mStackX - 7f, boilerY)
                lineTo(mStackX - 5f, boilerY - 22f)
                lineTo(mStackX - 9f, boilerY - 26f)
                lineTo(mStackX + 9f, boilerY - 26f)
                lineTo(mStackX + 5f, boilerY - 22f)
                lineTo(mStackX + 7f, boilerY)
                close()
            }
            drawPath(mStackPath, color = Color(0xFF12161A))
            drawRect(color = Color(0xFFFFB300), topLeft = Offset(mStackX - 9f, boilerY - 26f), size = Size(18f, 3f))

            // 5. Cowcatcher Wedge with hazard stripes
            val plowX = boilerX + boilerW
            val mPlow = Path().apply {
                moveTo(plowX, railY)
                lineTo(plowX + 28f, railY)
                lineTo(plowX, railY - 26f)
                close()
            }
            drawPath(mPlow, color = Color(0xFF1C252B))
            for (hi in 0..3) {
                val hx = plowX + hi * 7f
                drawLine(color = Color(0xFFFFB300), start = Offset(hx, railY), end = Offset(hx + 6f, railY - 14f), strokeWidth = 2f)
            }

            // 6. Large Driver Wheels & Connecting Rods
            val dRadius = 14f
            val dwY = railY - 4f
            val d1 = locoX + 45f
            val d2 = locoX + 85f
            val d3 = locoX + 125f
            val d4 = locoX + 165f
            listOf(d1, d2, d3, d4).forEach { dw ->
                drawCircle(color = Color(0xFF101417), radius = dRadius + 1f, center = Offset(dw, dwY))
                drawCircle(color = Color(0xFF263238), radius = dRadius, center = Offset(dw, dwY))
                drawCircle(color = Color(0xFF78909C), radius = dRadius * 0.82f, center = Offset(dw, dwY))
                drawCircle(color = Color(0xFF1A2228), radius = dRadius * 0.62f, center = Offset(dw, dwY))
                drawCircle(color = Color(0xFFB0BEC5), radius = 3f, center = Offset(dw, dwY))
            }
            // Horizontal connecting siderod linking driver wheels
            drawLine(color = Color(0xFF101418), start = Offset(d1 - 2f, dwY + 4f), end = Offset(d4 + 2f, dwY + 4f), strokeWidth = 4f)
            drawLine(color = Color(0xFFB0BEC5), start = Offset(d1, dwY + 3.5f), end = Offset(d4, dwY + 3.5f), strokeWidth = 2.5f)

            // Front pilot wheels
            drawCircle(color = Color(0xFF1E282D), radius = 8f, center = Offset(locoX + 215f, dwY))
            drawCircle(color = Color(0xFF1E282D), radius = 8f, center = Offset(locoX + 242f, dwY))

            // 7. Headlight & Volumetric Beam
            val hlX = plowX + 4f
            val hlY = boilerY + 16f
            drawCircle(color = Color(0xFF1A2127), radius = 10f, center = Offset(hlX, hlY))
            drawCircle(color = Color(0xFFFFB300), radius = 8.5f, center = Offset(hlX, hlY))
            drawCircle(color = Color(0xFFFFD54F), radius = 6.5f * lightPulse, center = Offset(hlX, hlY))
            drawCircle(color = Color(0xFFFFFFFF), radius = 3f, center = Offset(hlX, hlY))

            // Conical headlight beam
            val beam = Path().apply {
                moveTo(hlX, hlY)
                lineTo(w, hlY - 70f)
                lineTo(w, hlY + 110f)
                close()
            }
            drawPath(
                path = beam,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x88FFE082), Color(0x33FFD54F), Color.Transparent),
                    startX = hlX,
                    endX = w
                )
            )

            // 8. Chimney smoke billowing backward
            for (p in 0..5) {
                val px = mStackX - p * 32f
                val py = boilerY - 26f - p * 14f
                val pAlpha = (0.55f - p * 0.08f).coerceAtLeast(0.1f)
                drawCircle(color = Color(0xFF455A64).copy(alpha = pAlpha), radius = 18f + p * 8f, center = Offset(px, py))
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
                .padding(horizontal = 40.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // Game Logo Title
                Text(
                    text = "ÚLTIMO TREN",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp,
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

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "ACCIÓN Y SUPERVIVENCIA EN LAS VÍAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color(0xFFFFCA28)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stats & Gold Pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gold badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xEE2A1D05))
                            .border(1.2.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Oro",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${stats.totalGold} ORO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFE082)
                            )
                        }
                    }

                    if (stats.highestLevel > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x881E2833))
                                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NIVEL MÁX: ${stats.highestLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFECEFF1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary JUGAR Button
                Button(
                    onClick = { showMapSelectionDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .width(260.dp)
                        .height(48.dp)
                        .border(1.5.dp, Color(0xFFFFF9C4), RoundedCornerShape(12.dp))
                        .testTag("btn_play")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Jugar",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "JUGAR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // MEJORAS DEL TREN Button
                Button(
                    onClick = onUpgradesClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF263238),
                        contentColor = Color(0xFFFFD700)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(260.dp)
                        .height(44.dp)
                        .border(1.2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                        .testTag("btn_menu_upgrades")
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Mejoras del Tren",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MEJORAS DEL TREN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFFFFD700)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // MULTIJUGADOR (EN DESARROLLO)
                Button(
                    onClick = { showMultiplayerDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E2933),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(260.dp)
                        .height(44.dp)
                        .border(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .testTag("btn_menu_multiplayer")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Multijugador",
                        tint = Color(0xFF80D8FF),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MULTIJUGADOR",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE65100))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EN DESARROLLO",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFE0B2),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // AJUSTES Button
                    OutlinedButton(
                        onClick = onSettingsClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xAA1C262F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(125.dp)
                            .height(40.dp)
                            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                            .testTag("btn_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = Color(0xFFECEFF1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AJUSTES",
                            fontSize = 11.sp,
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(125.dp)
                            .height(40.dp)
                            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                            .testTag("btn_credits")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Créditos",
                            tint = Color(0xFFECEFF1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CRÉDITOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Right side spacer for locomotive visual composition
            Spacer(modifier = Modifier.weight(0.8f))
        }

        // Top-Right Corner: Book of Monster Guide ("GUÍA") & Player Wardrobe ("VESTUARIO")
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GuideBookCornerButton(
                stats = stats,
                lightPulse = lightPulse,
                onClick = onGuideClick
            )

            WardrobeCornerButton(
                lightPulse = lightPulse,
                onClick = onWardrobeClick
            )
        }

        // Multiplayer In-Development Dialog
        if (showMultiplayerDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC080C10)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF162534), Color(0xFF0F1822))
                            )
                        )
                        .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp))
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE65100))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "EN DESARROLLO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "MODO MULTIJUGADOR",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "El modo cooperativo en línea para defender el tren junto a otros supervivientes y maquinistas está actualmente en desarrollo por Star App.",
                            fontSize = 12.sp,
                            color = Color(0xFFB0BEC5),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "¡Estará disponible en una próxima actualización!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80D8FF),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { showMultiplayerDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("btn_close_multiplayer_dialog")
                        ) {
                            Text(
                                text = "ENTENDIDO",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Map Selection Dialog for Survival Mode
        if (showMapSelectionDialog) {
            MapSelectionDialog(
                onSelectMap = { mapId ->
                    showMapSelectionDialog = false
                    onPlayClick(mapId)
                },
                onDismiss = { showMapSelectionDialog = false }
            )
        }
    }
}

@Composable
fun MapSelectionDialog(
    onSelectMap: (mapId: Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9080C10)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(620.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF15222E), Color(0xFF0C1319))
                    )
                )
                .border(1.5.dp, Color(0xFFFFD54F), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SELECCIÓN DE DESTINO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = Color(0xFFFFD54F)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Modo Supervivencia: Sobrevive hordas interminables y sube de nivel eliminando infectados",
                    fontSize = 11.sp,
                    color = Color(0xFFB0BEC5)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MapOptionCard(
                        mapId = 1,
                        name = "BOSQUE SOMBRÍO",
                        icon = "🌲",
                        themeColor = Color(0xFF4CAF50),
                        description = "Niebla densa, pinos retorcidos y terreno fangoso.",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectMap(1) }
                    )
                    MapOptionCard(
                        mapId = 2,
                        name = "CAÑÓN CARMESÍ",
                        icon = "🏜️",
                        themeColor = Color(0xFFFF7043),
                        description = "Desfiladeros áridos, polvo rojo y visibilidad reducida.",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectMap(2) }
                    )
                    MapOptionCard(
                        mapId = 3,
                        name = "TUNDRA GLACIAL",
                        icon = "❄️",
                        themeColor = Color(0xFF40C4FF),
                        description = "Viento gélido, montañas nevadas y escarcha helada.",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectMap(3) }
                    )
                    MapOptionCard(
                        mapId = 0,
                        name = "ALEATORIO",
                        icon = "🎲",
                        themeColor = Color(0xFFFFD700),
                        description = "¡Destino al azar! Pon a prueba tu preparación.",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectMap(0) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("CANCELAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MapOptionCard(
    mapId: Int,
    name: String,
    icon: String,
    themeColor: Color,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x991E2833))
            .border(1.dp, themeColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = icon, fontSize = 26.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = themeColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 9.sp,
                color = Color(0xFF90A4AE),
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                maxLines = 3,
                modifier = Modifier.height(34.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .testTag(if (mapId == 0) "btn_map_random" else "btn_map_$mapId")
            ) {
                Text(
                    text = "ELEGIR",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun GuideBookCornerButton(
    stats: GameStatsEntity,
    lightPulse: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMonsters = com.example.game.model.BESTIARY_ENTRIES.size
    val discovered = com.example.game.model.BESTIARY_ENTRIES.count { stats.hasSeenMonster(it.type.name) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3E1F18), Color(0xFF1E0E0B))
                )
            )
            .border(
                width = 1.8.dp,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFF8D6E63))
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag("btn_guide_book")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Book Icon & Gilded Frame
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2B1510))
                    .border(1.2.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Libro Guía",
                    tint = Color(0xFFFFE082),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GUÍA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = Color(0xFFFFF9C4),
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFFFFB300).copy(alpha = 0.8f * lightPulse),
                                offset = Offset(0f, 0f),
                                blurRadius = 12f * lightPulse
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Badge with discovered count
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFB71C1C))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "$discovered/$totalMonsters",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFE0B2)
                        )
                    }
                }
                Text(
                    text = "BESTIARIO DE MONSTRUOS",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCA28),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun WardrobeCornerButton(
    lightPulse: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF132238), Color(0xFF0D1522))
                )
            )
            .border(
                width = 1.8.dp,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF334155))
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag("btn_wardrobe")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Checkroom / Armor Suit Icon & Neon Frame
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B192C))
                    .border(1.2.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Checkroom,
                    contentDescription = "Vestuario",
                    tint = Color(0xFF7DD3FC),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VESTUARIO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = Color(0xFFE0F2FE),
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF0284C7).copy(alpha = 0.8f * lightPulse),
                                offset = Offset(0f, 0f),
                                blurRadius = 12f * lightPulse
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Badge 3 FASES
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0369A1))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "3 FASES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE0F2FE),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Text(
                    text = "ASPECTOS DEL JUGADOR",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}



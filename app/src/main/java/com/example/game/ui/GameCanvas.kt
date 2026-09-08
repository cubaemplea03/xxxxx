package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.example.game.engine.GameEngine
import com.example.game.model.Enemy
import com.example.game.model.EnemyState
import com.example.game.model.EnemyType
import com.example.game.model.LevelPhase
import com.example.game.model.Player
import com.example.game.model.ResourceType
import com.example.game.model.Wagon
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
    engine: GameEngine,
    modifier: Modifier = Modifier,
    renderTickProvider: () -> Long = { 0L }
) {
    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val bannerPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 36f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val subBannerPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val _tick = renderTickProvider()
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Logical height is 320 units
        val scale = canvasHeight / 320f
        val camX = engine.cameraX
        val viewLeft = camX - (canvasWidth / scale) * 0.5f

        // 1. Draw parallax sky and environment (specific to map level)
        drawBackground(engine, canvasWidth, canvasHeight, viewLeft, scale)

        // 2. Draw Train Tracks, Station Platform and Ground
        drawTracksAndGround(engine, canvasWidth, canvasHeight, viewLeft, scale)

        // 3. Draw Wagons and Mounted Train Weapons
        for (w in engine.wagons) {
            drawWagon(w, viewLeft, scale, engine)
        }

        // 4. Draw Resources (Gold coins, Scrap, Ammo, etc.)
        for (res in engine.resources) {
            drawResourceItem(res, viewLeft, scale)
        }

        // 5. Draw Enemies
        for (enemy in engine.enemies) {
            drawEnemy(enemy, viewLeft, scale)
        }

        // 6. Draw Player
        drawPlayer(engine.player, viewLeft, scale)

        // 7. Draw Player Projectiles
        for (p in engine.projectiles) {
            val sx = (p.x - viewLeft) * scale
            val sy = p.y * scale
            drawCircle(
                color = Color(0xFFFFEB3B),
                radius = 3.5f * scale,
                center = Offset(sx, sy)
            )
            drawLine(
                color = Color(0xFFFF9800),
                start = Offset(sx - p.vx * 0.04f * scale, sy),
                end = Offset(sx, sy),
                strokeWidth = 2.5f * scale
            )
        }

        // 8. Draw Train Turret & Cannon Projectiles
        for (tp in engine.turretProjectiles) {
            val sx = (tp.x - viewLeft) * scale
            val sy = tp.y * scale
            if (tp.isCannon) {
                // Heavy explosive shell with flame trail
                drawCircle(
                    color = Color(0xFFFF5722),
                    radius = 6f * scale,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color(0xFFFFEB3B),
                    radius = 3.5f * scale,
                    center = Offset(sx, sy)
                )
                drawLine(
                    color = Color(0xAAFF7043),
                    start = Offset(sx - tp.vx * 0.05f * scale, sy - tp.vy * 0.05f * scale),
                    end = Offset(sx, sy),
                    strokeWidth = 4f * scale
                )
            } else {
                // Fast bright yellow tracer bullet
                drawCircle(
                    color = Color(0xFFFFF176),
                    radius = 3.5f * scale,
                    center = Offset(sx, sy)
                )
                drawLine(
                    color = Color(0xFFFFB300),
                    start = Offset(sx - tp.vx * 0.035f * scale, sy - tp.vy * 0.035f * scale),
                    end = Offset(sx, sy),
                    strokeWidth = 2.8f * scale
                )
            }
        }

        // 9. Draw Slash Effects
        for (s in engine.slashEffects) {
            val sx = (s.x - viewLeft) * scale
            val sy = s.y * scale
            val arcDir = if (s.facingRight) 1f else -1f
            val slashPath = Path().apply {
                moveTo(sx - 15f * scale * arcDir, sy - 18f * scale)
                quadraticTo(sx + 24f * scale * arcDir, sy, sx - 15f * scale * arcDir, sy + 18f * scale)
            }
            drawPath(
                path = slashPath,
                color = Color(0xFFE0F7FA),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f * scale)
            )
        }

        // 10. Draw Particles
        for (part in engine.particles) {
            val sx = (part.x - viewLeft) * scale
            val sy = part.y * scale
            val c = Color(part.color).copy(alpha = part.alpha)
            drawCircle(
                color = c,
                radius = (part.size * 0.5f) * scale,
                center = Offset(sx, sy)
            )
        }

        // 11. Draw Floating texts
        drawContext.canvas.nativeCanvas.let { nativeCanvas ->
            for (ft in engine.floatingTexts) {
                val sx = (ft.x - viewLeft) * scale
                val sy = ft.y * scale
                textPaint.color = ft.color.toInt()
                textPaint.alpha = (ft.life * 255).coerceIn(0f, 255f).toInt()
                nativeCanvas.drawText(ft.text, sx, sy, textPaint)
            }
        }

        // 12. Atmospheric Overlay & Cinematic Arrival Banners
        drawAtmosphericOverlay(engine, canvasWidth, canvasHeight)

        // Draw Arrival or Victory Cinematic Banners
        if (engine.levelPhase == LevelPhase.ARRIVING) {
            drawArrivalBanner(engine, canvasWidth, canvasHeight, scale, bannerPaint, subBannerPaint)
        }
    }
}

private fun DrawScope.drawArrivalBanner(
    engine: GameEngine,
    width: Float,
    height: Float,
    scale: Float,
    titlePaint: android.graphics.Paint,
    subPaint: android.graphics.Paint
) {
    // Top and bottom cinematic letterbox bands
    val barHeight = 36f * scale
    drawRect(color = Color(0xEE05080C), topLeft = Offset.Zero, size = Size(width, barHeight))
    drawRect(color = Color(0xEE05080C), topLeft = Offset(0f, height - barHeight), size = Size(width, barHeight))

    // Central translucent arrival plaque
    val plaqueW = (width * 0.75f).coerceAtMost(620f * scale)
    val plaqueH = 75f * scale
    val plaqueX = (width - plaqueW) * 0.5f
    val plaqueY = 28f * scale

    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xE61A222B), Color(0xF20F161E)),
            startY = plaqueY,
            endY = plaqueY + plaqueH
        ),
        topLeft = Offset(plaqueX, plaqueY),
        size = Size(plaqueW, plaqueH)
    )
    drawRect(
        color = Color(0xFFFFB300),
        topLeft = Offset(plaqueX, plaqueY),
        size = Size(plaqueW, 3f * scale)
    )

    drawContext.canvas.nativeCanvas.let { nativeCanvas ->
        titlePaint.color = 0xFFFFD54F.toInt()
        val levelName = engine.getMapNameForLevel(engine.currentLevel).uppercase()
        nativeCanvas.drawText("LLEGADA - NIVEL ${engine.currentLevel}: $levelName", width * 0.5f, plaqueY + 34f * scale, titlePaint)

        subPaint.color = 0xFFFF7043.toInt()
        val secLeft = (engine.trainArrivalTimer).coerceAtLeast(0f)
        nativeCanvas.drawText("Frenando convoy en estación... (${"%.1f".format(secLeft)}s) ¡Prepárate!", width * 0.5f, plaqueY + 60f * scale, subPaint)
    }
}

private fun DrawScope.drawBackground(
    engine: GameEngine,
    width: Float,
    height: Float,
    viewLeft: Float,
    scale: Float
) {
    when (engine.currentLevel) {
        2 -> {
            // MAP 2: CAÑÓN CARMESÍ (Dusty red sandstone desert, burning sunset)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF280905), Color(0xFF5A1C0E), Color(0xFF9E3E18), Color(0xFFD66B24)),
                    startY = 0f,
                    endY = height * 0.85f
                ),
                size = Size(width, height)
            )

            // Massive red sunset sun
            val sunX = width * 0.7f - (viewLeft * 0.02f) % width
            drawCircle(
                color = Color(0x44FF5722),
                radius = 55f * scale,
                center = Offset(sunX, height * 0.30f)
            )
            drawCircle(
                color = Color(0xFFFF7043),
                radius = 32f * scale,
                center = Offset(sunX, height * 0.30f)
            )

            // Distant sandstone mesas
            val p1Offset = -(viewLeft * 0.12f * scale) % width
            for (copy in -1..2) {
                val ox = p1Offset + copy * width
                val p1 = Path().apply {
                    moveTo(ox, height * 0.70f)
                    lineTo(ox + width * 0.10f, height * 0.52f)
                    lineTo(ox + width * 0.28f, height * 0.52f)
                    lineTo(ox + width * 0.35f, height * 0.70f)
                    lineTo(ox + width * 0.50f, height * 0.48f)
                    lineTo(ox + width * 0.72f, height * 0.48f)
                    lineTo(ox + width * 0.80f, height * 0.70f)
                    lineTo(ox + width, height * 0.70f)
                    lineTo(ox + width, height)
                    lineTo(ox, height)
                    close()
                }
                drawPath(p1, color = Color(0xFF3E1A16))
            }

            // Jagged rock pillars & desert crags
            val p2Offset = -(viewLeft * 0.32f * scale) % (width * 0.5f)
            for (copy in -1..3) {
                val ox = p2Offset + copy * (width * 0.5f)
                for (i in 0..5) {
                    val rx = ox + i * 65f * scale
                    val ry = height * 0.72f
                    val rockHeight = (35f + (i % 3) * 18f) * scale
                    val rockPath = Path().apply {
                        moveTo(rx, ry - rockHeight)
                        lineTo(rx - 16f * scale, ry)
                        lineTo(rx + 16f * scale, ry)
                        close()
                    }
                    drawPath(rockPath, color = Color(0xFF4D221D))
                }
            }
        }
        3 -> {
            // MAP 3: COMPLEJO HELADO (Glacial arctic steel grey and frozen industrial)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF091118), Color(0xFF142431), Color(0xFF1F3546), Color(0xFF2E485C)),
                    startY = 0f,
                    endY = height * 0.85f
                ),
                size = Size(width, height)
            )

            // Frosty moon halo
            val moonX = width * 0.8f - (viewLeft * 0.02f) % width
            drawCircle(
                color = Color(0x3380DEEA),
                radius = 48f * scale,
                center = Offset(moonX, height * 0.22f)
            )
            drawCircle(
                color = Color(0xFFE0F7FA),
                radius = 22f * scale,
                center = Offset(moonX, height * 0.22f)
            )

            // Distant industrial silos and gantry cranes
            val p1Offset = -(viewLeft * 0.12f * scale) % width
            for (copy in -1..2) {
                val ox = p1Offset + copy * width
                val p1 = Path().apply {
                    moveTo(ox, height * 0.70f)
                    lineTo(ox + width * 0.15f, height * 0.46f)
                    lineTo(ox + width * 0.30f, height * 0.46f)
                    lineTo(ox + width * 0.45f, height * 0.65f)
                    lineTo(ox + width * 0.60f, height * 0.42f)
                    lineTo(ox + width * 0.80f, height * 0.42f)
                    lineTo(ox + width, height * 0.70f)
                    lineTo(ox + width, height)
                    lineTo(ox, height)
                    close()
                }
                drawPath(p1, color = Color(0xFF132029))
            }

            // Snowy steel towers
            val p2Offset = -(viewLeft * 0.35f * scale) % (width * 0.5f)
            for (copy in -1..3) {
                val ox = p2Offset + copy * (width * 0.5f)
                for (i in 0..5) {
                    val tx = ox + i * 60f * scale
                    val ty = height * 0.72f
                    drawRect(
                        color = Color(0xFF1B2D38),
                        topLeft = Offset(tx, ty - 60f * scale),
                        size = Size(14f * scale, 60f * scale)
                    )
                    // White snow cap
                    drawRect(
                        color = Color(0xFFECEFF1),
                        topLeft = Offset(tx - 2f * scale, ty - 62f * scale),
                        size = Size(18f * scale, 4f * scale)
                    )
                }
            }
        }
        else -> {
            // MAP 1: BOSQUE NOCTURNO (Default: midnight blues, glowing moon, dense pines)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0E14), Color(0xFF141C24), Color(0xFF1E2833)),
                    startY = 0f,
                    endY = height * 0.85f
                ),
                size = Size(width, height)
            )

            val moonX = width * 0.75f - (viewLeft * 0.02f) % width
            drawCircle(
                color = Color(0x33FFF59D),
                radius = 45f * scale,
                center = Offset(moonX, height * 0.22f)
            )
            drawCircle(
                color = Color(0xDDFFF9C4),
                radius = 24f * scale,
                center = Offset(moonX, height * 0.22f)
            )

            val p1Offset = -(viewLeft * 0.12f * scale) % width
            for (copy in -1..2) {
                val ox = p1Offset + copy * width
                val p1 = Path().apply {
                    moveTo(ox, height * 0.65f)
                    lineTo(ox + width * 0.15f, height * 0.42f)
                    lineTo(ox + width * 0.35f, height * 0.58f)
                    lineTo(ox + width * 0.55f, height * 0.38f)
                    lineTo(ox + width * 0.75f, height * 0.62f)
                    lineTo(ox + width * 0.90f, height * 0.45f)
                    lineTo(ox + width, height * 0.65f)
                    lineTo(ox + width, height)
                    lineTo(ox, height)
                    close()
                }
                drawPath(p1, color = Color(0xFF131921))
            }

            val p2Offset = -(viewLeft * 0.35f * scale) % (width * 0.5f)
            for (copy in -1..3) {
                val ox = p2Offset + copy * (width * 0.5f)
                for (i in 0..6) {
                    val tx = ox + i * 55f * scale
                    val ty = height * 0.68f
                    val treeHeight = (45f + (i % 3) * 15f) * scale
                    val treePath = Path().apply {
                        moveTo(tx, ty - treeHeight)
                        lineTo(tx - 14f * scale, ty)
                        lineTo(tx + 14f * scale, ty)
                        close()
                    }
                    drawPath(treePath, color = Color(0xFF1C242D))
                }
            }
        }
    }
}

private fun DrawScope.drawTracksAndGround(
    engine: GameEngine,
    width: Float,
    height: Float,
    viewLeft: Float,
    scale: Float
) {
    val groundY = 224f * scale
    val groundColor = when (engine.currentLevel) {
        2 -> Color(0xFF2C1610) // Reddish ballast
        3 -> Color(0xFF263238) // Frozen gravel
        else -> Color(0xFF1A1D20) // Dark ballast
    }

    drawRect(
        color = groundColor,
        topLeft = Offset(0f, groundY),
        size = Size(width, height - groundY)
    )

    // Moving railroad ties
    val tieSpacing = 28f * scale
    val offsetMovement = if (engine.levelPhase == LevelPhase.ARRIVING || engine.levelPhase == LevelPhase.DEPARTING) {
        engine.trainWheelOffset * scale
    } else {
        viewLeft * scale
    }
    val tieOffset = -offsetMovement % tieSpacing
    var tx = tieOffset - tieSpacing
    while (tx < width + tieSpacing) {
        drawRect(
            color = Color(0xFF2C2520),
            topLeft = Offset(tx, groundY + 4f * scale),
            size = Size(10f * scale, 12f * scale)
        )
        tx += tieSpacing
    }

    // Steel Rails
    drawRect(
        color = Color(0xFF546E7A),
        topLeft = Offset(0f, groundY + 2f * scale),
        size = Size(width, 3f * scale)
    )
    drawRect(
        color = Color(0xFFB0BEC5),
        topLeft = Offset(0f, groundY + 1f * scale),
        size = Size(width, 1.2f * scale)
    )

    // Station Marker Platform (at x = 800)
    val stationX = (800f - viewLeft) * scale
    if (stationX > -300f * scale && stationX < width + 300f * scale) {
        // Platform structure
        drawRect(
            color = Color(0xFF37474F),
            topLeft = Offset(stationX - 120f * scale, groundY - 8f * scale),
            size = Size(240f * scale, 8f * scale)
        )
        // Station sign post
        drawRect(
            color = Color(0xFF263238),
            topLeft = Offset(stationX - 4f * scale, groundY - 60f * scale),
            size = Size(8f * scale, 52f * scale)
        )
        // Station sign board
        drawRect(
            color = Color(0xFFD32F2F),
            topLeft = Offset(stationX - 70f * scale, groundY - 80f * scale),
            size = Size(140f * scale, 24f * scale)
        )
        drawRect(
            color = Color(0xFFFFD54F),
            topLeft = Offset(stationX - 66f * scale, groundY - 76f * scale),
            size = Size(132f * scale, 16f * scale)
        )
        // Small lantern
        drawCircle(
            color = Color(0xFFFFEB3B),
            radius = 4f * scale,
            center = Offset(stationX, groundY - 86f * scale)
        )
    }
}

private fun DrawScope.drawWagon(
    w: Wagon,
    viewLeft: Float,
    scale: Float,
    engine: GameEngine
) {
    val wx = (w.x - viewLeft) * scale
    val wy = w.roofY * scale
    val wWidth = w.width * scale
    val wHeight = (w.floorY - w.roofY) * scale
    val floorScreenY = w.floorY * scale

    when (w.index) {
        4 -> {
            // LOCOMOTIVE
            drawRect(
                color = Color(0xFF263238),
                topLeft = Offset(wx, wy + 15f * scale),
                size = Size(wWidth, wHeight - 15f * scale)
            )
            drawRect(
                color = Color(0xFF1E272C),
                topLeft = Offset(wx + 10f * scale, wy),
                size = Size(95f * scale, wHeight)
            )
            drawRect(
                color = Color(0xFFFFD54F),
                topLeft = Offset(wx + 25f * scale, wy + 10f * scale),
                size = Size(25f * scale, 22f * scale)
            )
            // Furnace door
            drawRect(
                color = Color(0xFFFF5722),
                topLeft = Offset(wx + 220f * scale, wy + 35f * scale),
                size = Size(30f * scale, 35f * scale)
            )
            drawCircle(
                color = Color(0x66FF9800),
                radius = 24f * scale,
                center = Offset(wx + 235f * scale, wy + 52f * scale)
            )
            // Smokestack
            drawRect(
                color = Color(0xFF1A2226),
                topLeft = Offset(wx + 280f * scale, wy - 10f * scale),
                size = Size(20f * scale, 30f * scale)
            )

            // Front plow / Cowcatcher (Enhanced if Armor upgraded)
            val hasArmor = engine.trainWeapons.armorLevel > 0
            val cowcatcherColor = if (hasArmor) Color(0xFFFF5722) else Color(0xFF37474F)
            val cowcatcher = Path().apply {
                moveTo(wx + wWidth, floorScreenY)
                lineTo(wx + wWidth + (if (hasArmor) 35f else 24f) * scale, floorScreenY)
                lineTo(wx + wWidth, floorScreenY - (if (hasArmor) 32f else 24f) * scale)
                close()
            }
            drawPath(cowcatcher, color = cowcatcherColor)

            // If Armor Upgrade: draw front spikes!
            if (hasArmor) {
                for (s in 0..2) {
                    val spikeY = floorScreenY - (8f + s * 9f) * scale
                    drawLine(
                        color = Color(0xFFFFD54F),
                        start = Offset(wx + wWidth + 12f * scale, spikeY),
                        end = Offset(wx + wWidth + 38f * scale, spikeY),
                        strokeWidth = 3f * scale
                    )
                }
            }

            // Headlight / Searchlight (Enhanced if Spotlight upgraded)
            val lightX = wx + wWidth + 10f * scale
            val lightY = wy + 25f * scale
            val hasSpotlight = engine.trainWeapons.spotlightLevel > 0
            val beamLength = if (hasSpotlight) 520f * scale else 380f * scale

            drawCircle(
                color = Color(0xFFFFF9C4),
                radius = (if (hasSpotlight) 10f else 7f) * scale,
                center = Offset(lightX, lightY)
            )
            val beam = Path().apply {
                moveTo(lightX, lightY)
                lineTo(lightX + beamLength, lightY - 70f * scale)
                lineTo(lightX + beamLength, lightY + 120f * scale)
                close()
            }
            drawPath(
                path = beam,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (hasSpotlight) Color(0x99FFF9C4) else Color(0x77FFF59D),
                        Color(0x11FFF59D),
                        Color.Transparent
                    ),
                    startX = lightX,
                    endX = lightX + beamLength
                )
            )
        }
        3 -> {
            // TENDER
            drawRect(
                color = Color(0xFF37474F),
                topLeft = Offset(wx, wy),
                size = Size(wWidth, wHeight)
            )
            val coal = Path().apply {
                moveTo(wx + 10f * scale, wy)
                lineTo(wx + wWidth * 0.45f, wy - 18f * scale)
                lineTo(wx + wWidth - 10f * scale, wy)
                close()
            }
            drawPath(coal, color = Color(0xFF121416))
        }
        2 -> {
            // ARMORED BOXCAR
            drawRect(
                color = Color(0xFF2E3B43),
                topLeft = Offset(wx, wy),
                size = Size(wWidth, wHeight)
            )
            // Catwalk railing
            drawRect(
                color = Color(0xFF546E7A),
                topLeft = Offset(wx + 5f * scale, wy - 4f * scale),
                size = Size(wWidth - 10f * scale, 3f * scale)
            )
            for (i in 1..3) {
                drawLine(
                    color = Color(0xFF1E282D),
                    start = Offset(wx + (wWidth / 4) * i, wy),
                    end = Offset(wx + (wWidth / 4) * i, floorScreenY),
                    strokeWidth = 2f * scale
                )
            }
            val ladderX = wx + 18f * scale
            for (r in 0..5) {
                drawLine(
                    color = Color(0xFF78909C),
                    start = Offset(ladderX, wy + r * 11f * scale),
                    end = Offset(ladderX + 14f * scale, wy + r * 11f * scale),
                    strokeWidth = 2.5f * scale
                )
            }

            // HEAVY CANNON UPGRADE (Mounted on roof at center of Armored Wagon)
            if (engine.trainWeapons.cannonLevel > 0) {
                val cannonCenterX = wx + wWidth * 0.5f
                val cannonCenterY = wy - 12f * scale

                // Cupola turret base
                drawCircle(
                    color = Color(0xFF1B242A),
                    radius = 16f * scale,
                    center = Offset(cannonCenterX, wy - 4f * scale)
                )
                drawCircle(
                    color = Color(0xFF455A64),
                    radius = 11f * scale,
                    center = Offset(cannonCenterX, cannonCenterY)
                )

                // Cannon Barrel pointing at cannonAngle
                val angle = engine.trainWeapons.cannonAngle
                val barrelLen = (24f + engine.trainWeapons.cannonLevel * 5f) * scale
                val barrelEndX = cannonCenterX + cos(angle) * barrelLen
                val barrelEndY = cannonCenterY + sin(angle) * barrelLen

                drawLine(
                    color = Color(0xFF263238),
                    start = Offset(cannonCenterX, cannonCenterY),
                    end = Offset(barrelEndX, barrelEndY),
                    strokeWidth = 6f * scale
                )
                // Cannon muzzle tip
                drawCircle(
                    color = Color(0xFF90A4AE),
                    radius = 4f * scale,
                    center = Offset(barrelEndX, barrelEndY)
                )

                // Cannon Muzzle Flash Flare
                if (engine.trainWeapons.cannonMuzzleTimer > 0f) {
                    drawCircle(
                        color = Color(0xFFFF5722),
                        radius = 14f * scale,
                        center = Offset(barrelEndX, barrelEndY)
                    )
                    drawCircle(
                        color = Color(0xFFFFEB3B),
                        radius = 8f * scale,
                        center = Offset(barrelEndX, barrelEndY)
                    )
                }
            }
        }
        1 -> {
            // PLATAFORMA ARTILLADA (Flatcar)
            drawRect(
                color = Color(0xFF263238),
                topLeft = Offset(wx, wy),
                size = Size(wWidth, 14f * scale)
            )

            // Crates on flatcar
            drawRect(
                color = Color(0xFF5D4037),
                topLeft = Offset(wx + 25f * scale, wy - 32f * scale),
                size = Size(35f * scale, 32f * scale)
            )

            // GATLING / TURRET UPGRADE (Mounted on Flatcar)
            if (engine.trainWeapons.turretLevel > 0) {
                val turretCenterX = wx + wWidth * 0.5f
                val turretCenterY = wy - 14f * scale

                // Tripod / Steel Turret Stand
                drawLine(
                    color = Color(0xFF37474F),
                    start = Offset(turretCenterX - 14f * scale, wy),
                    end = Offset(turretCenterX, turretCenterY),
                    strokeWidth = 3f * scale
                )
                drawLine(
                    color = Color(0xFF37474F),
                    start = Offset(turretCenterX + 14f * scale, wy),
                    end = Offset(turretCenterX, turretCenterY),
                    strokeWidth = 3f * scale
                )
                // Turret swivel dome
                drawCircle(
                    color = Color(0xFF212121),
                    radius = 9f * scale,
                    center = Offset(turretCenterX, turretCenterY)
                )
                drawCircle(
                    color = Color(0xFF00E676), // Green status LED
                    radius = 2.5f * scale,
                    center = Offset(turretCenterX, turretCenterY - 4f * scale)
                )

                // Twin Gatling barrels pointing at turretAngle
                val angle = engine.trainWeapons.turretAngle
                val bLen = (18f + engine.trainWeapons.turretLevel * 4f) * scale
                val bEndX = turretCenterX + cos(angle) * bLen
                val bEndY = turretCenterY + sin(angle) * bLen

                drawLine(
                    color = Color(0xFF424242),
                    start = Offset(turretCenterX, turretCenterY),
                    end = Offset(bEndX, bEndY),
                    strokeWidth = 4f * scale
                )

                // Turret Muzzle Flash
                if (engine.trainWeapons.turretMuzzleTimer > 0f) {
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 8f * scale,
                        center = Offset(bEndX, bEndY)
                    )
                }
            }
        }
        0 -> {
            // CABOOSE
            drawRect(
                color = Color(0xFF455A64),
                topLeft = Offset(wx + 20f * scale, wy),
                size = Size(wWidth - 20f * scale, wHeight)
            )
            drawRect(
                color = Color(0xFF263238),
                topLeft = Offset(wx, floorScreenY - 8f * scale),
                size = Size(20f * scale, 8f * scale)
            )
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 5f * scale,
                center = Offset(wx + 4f * scale, wy + 20f * scale)
            )
            drawCircle(
                color = Color(0x44FF1744),
                radius = 16f * scale,
                center = Offset(wx + 4f * scale, wy + 20f * scale)
            )
        }
    }

    // Scavenge Crate
    if (w.hasCrate) {
        val cx = wx + wWidth * 0.7f - 14f * scale
        val cy = (if (w.hasRoof) w.roofY else w.floorY) * scale - 24f * scale
        if (!w.crateOpened) {
            drawRect(
                color = Color(0xFF8D6E63),
                topLeft = Offset(cx, cy),
                size = Size(28f * scale, 24f * scale)
            )
            drawRect(
                color = Color(0xFFFFB300),
                topLeft = Offset(cx + 10f * scale, cy + 9f * scale),
                size = Size(8f * scale, 6f * scale)
            )
            drawCircle(
                color = Color(0x33FFCA28),
                radius = 20f * scale,
                center = Offset(cx + 14f * scale, cy + 12f * scale)
            )
        } else {
            drawRect(
                color = Color(0xFF4E342E),
                topLeft = Offset(cx, cy + 6f * scale),
                size = Size(28f * scale, 18f * scale)
            )
        }
    }

    // Wheels
    val wheelY = floorScreenY + 6f * scale
    val wheelRadius = 8f * scale
    val w1 = wx + 30f * scale
    val w2 = wx + 55f * scale
    val w3 = wx + wWidth - 55f * scale
    val w4 = wx + wWidth - 30f * scale

    listOf(w1, w2, w3, w4).forEach { wheelX ->
        drawCircle(
            color = Color(0xFF1E282D),
            radius = wheelRadius,
            center = Offset(wheelX, wheelY)
        )
        drawCircle(
            color = Color(0xFF78909C),
            radius = wheelRadius * 0.4f,
            center = Offset(wheelX, wheelY)
        )
    }

    // Coupler
    drawRect(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(wx + wWidth, floorScreenY - 6f * scale),
        size = Size(20f * scale, 5f * scale)
    )
}

private fun DrawScope.drawPlayer(
    player: Player,
    viewLeft: Float,
    scale: Float
) {
    val px = (player.x - viewLeft) * scale
    val py = player.y * scale

    if (player.invulnerableTimer > 0f && (player.invulnerableTimer * 20).toInt() % 2 == 0) {
        return
    }

    val dir = if (player.facingRight) 1f else -1f
    val isCrouch = player.isCrouching
    val animOffset = (sin(player.animFrame.toDouble()) * 3f * scale).toFloat()

    // 1. Shadow beneath player
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(px - 14f * scale, py - 4f * scale),
        size = Size(28f * scale, 8f * scale)
    )

    // 2. Legs / Boots
    val legHeight = if (isCrouch) 10f * scale else 16f * scale
    val legY = py - legHeight
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(px - 6f * scale, legY),
        size = Size(5f * scale, legHeight)
    )
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(px + 1f * scale, legY + animOffset),
        size = Size(5f * scale, legHeight)
    )

    // 3. Torso (Leather survivor trenchcoat)
    val bodyHeight = if (isCrouch) 16f * scale else 22f * scale
    val bodyY = legY - bodyHeight
    drawRect(
        color = Color(0xFF4E342E),
        topLeft = Offset(px - 7f * scale, bodyY),
        size = Size(14f * scale, bodyHeight)
    )
    // Red bandana / scarf
    drawRect(
        color = Color(0xFFD32F2F),
        topLeft = Offset(px - 6f * scale, bodyY - 1f * scale),
        size = Size(12f * scale, 5f * scale)
    )

    // 4. Head & Gas mask
    val headRadius = 6.5f * scale
    val headY = bodyY - headRadius
    drawCircle(
        color = Color(0xFF37474F),
        radius = headRadius,
        center = Offset(px, headY)
    )
    // Glowing goggles
    val eyeX = px + 4f * scale * dir
    drawCircle(
        color = Color(0xFF00E5FF),
        radius = 2.2f * scale,
        center = Offset(eyeX, headY - 1f * scale)
    )

    // 5. Weapon held
    val armY = bodyY + 8f * scale
    val gunEndX = px + 16f * scale * dir
    drawLine(
        color = Color(0xFFB0BEC5),
        start = Offset(px, armY),
        end = Offset(gunEndX, armY),
        strokeWidth = 3f * scale
    )
}

private fun DrawScope.drawEnemy(
    enemy: Enemy,
    viewLeft: Float,
    scale: Float
) {
    val ex = (enemy.x - viewLeft) * scale
    val ey = enemy.y * scale
    val dir = if (enemy.facingRight) 1f else -1f

    // Flashing when hurt
    val baseColor = if (enemy.state == EnemyState.HURT) Color(0xFFFFFFFF) else Color(0xFF37474F)

    when (enemy.type) {
        EnemyType.ERRANTE -> {
            // Zombie-like biped walker
            drawRect(
                color = baseColor,
                topLeft = Offset(ex - 7f * scale, ey - 32f * scale),
                size = Size(14f * scale, 24f * scale)
            )
            drawCircle(
                color = Color(0xFF5D4037),
                radius = 6f * scale,
                center = Offset(ex, ey - 36f * scale)
            )
            // Glowing feral eye
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 1.8f * scale,
                center = Offset(ex + 3f * scale * dir, ey - 36f * scale)
            )
            // Limbs
            drawLine(
                color = baseColor,
                start = Offset(ex - 3f * scale, ey - 8f * scale),
                end = Offset(ex - 3f * scale, ey),
                strokeWidth = 3f * scale
            )
            drawLine(
                color = baseColor,
                start = Offset(ex + 3f * scale, ey - 8f * scale),
                end = Offset(ex + 3f * scale, ey),
                strokeWidth = 3f * scale
            )
        }
        EnemyType.CORREDOR -> {
            // Low quadruped beast
            drawOval(
                color = baseColor,
                topLeft = Offset(ex - 14f * scale, ey - 18f * scale),
                size = Size(28f * scale, 12f * scale)
            )
            val headX = ex + 12f * scale * dir
            drawCircle(color = baseColor, radius = 5f * scale, center = Offset(headX, ey - 14f * scale))
            drawCircle(color = Color(0xFFFF1744), radius = 1.8f * scale, center = Offset(headX, ey - 14f * scale))
            drawLine(
                color = Color(0xFFD84315),
                start = Offset(ex - 4f * scale, ey - 18f * scale),
                end = Offset(ex - 8f * scale, ey - 25f * scale),
                strokeWidth = 2.5f * scale
            )
        }
        EnemyType.TREPADOR -> {
            // Spidery climber
            drawCircle(color = baseColor, radius = 8f * scale, center = Offset(ex, ey - 16f * scale))
            drawLine(color = baseColor, start = Offset(ex, ey - 16f * scale), end = Offset(ex - 12f * scale, ey - 26f * scale), strokeWidth = 2f * scale)
            drawLine(color = baseColor, start = Offset(ex, ey - 16f * scale), end = Offset(ex + 12f * scale, ey - 26f * scale), strokeWidth = 2f * scale)
            drawLine(color = baseColor, start = Offset(ex, ey - 16f * scale), end = Offset(ex - 14f * scale, ey), strokeWidth = 2f * scale)
            drawLine(color = baseColor, start = Offset(ex, ey - 16f * scale), end = Offset(ex + 14f * scale, ey), strokeWidth = 2f * scale)
            drawCircle(color = Color(0xFFE040FB), radius = 2.5f * scale, center = Offset(ex + 3f * scale * dir, ey - 16f * scale))
        }
    }

    // Health bar above enemy
    if (enemy.health < enemy.maxHealth && enemy.state != EnemyState.DYING) {
        val barWidth = 24f * scale
        val barHeight = 3.5f * scale
        val barX = ex - barWidth * 0.5f
        val barY = ey - 40f * scale
        drawRect(color = Color(0x88000000), topLeft = Offset(barX, barY), size = Size(barWidth, barHeight))
        val hpRatio = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
        drawRect(color = Color(0xFFE53935), topLeft = Offset(barX, barY), size = Size(barWidth * hpRatio, barHeight))
    }
}

private fun DrawScope.drawResourceItem(
    res: com.example.game.model.ResourceItem,
    viewLeft: Float,
    scale: Float
) {
    val rx = (res.x - viewLeft) * scale
    val ry = (res.y + res.bobOffset) * scale

    when (res.type) {
        ResourceType.ORO -> {
            // Shiny 3D gold coin
            drawCircle(
                color = Color(0x55FFD700),
                radius = 14f * scale,
                center = Offset(rx, ry)
            )
            drawCircle(
                color = Color(0xFFFFB300),
                radius = 7f * scale,
                center = Offset(rx, ry)
            )
            drawCircle(
                color = Color(0xFFFFE082),
                radius = 5.2f * scale,
                center = Offset(rx, ry)
            )
        }
        ResourceType.CHATARRA -> {
            drawCircle(color = Color(0x44B0BEC5), radius = 12f * scale, center = Offset(rx, ry))
            drawCircle(color = Color(0xFFB0BEC5), radius = 5.5f * scale, center = Offset(rx, ry))
        }
        ResourceType.MUNICION -> {
            drawCircle(color = Color(0x4442A5F5), radius = 12f * scale, center = Offset(rx, ry))
            drawCircle(color = Color(0xFF42A5F5), radius = 5.5f * scale, center = Offset(rx, ry))
        }
        ResourceType.COMIDA -> {
            drawCircle(color = Color(0x4466BB6A), radius = 12f * scale, center = Offset(rx, ry))
            drawCircle(color = Color(0xFF66BB6A), radius = 5.5f * scale, center = Offset(rx, ry))
        }
        ResourceType.MEDICINA -> {
            drawCircle(color = Color(0x44EF5350), radius = 12f * scale, center = Offset(rx, ry))
            drawCircle(color = Color(0xFFEF5350), radius = 5.5f * scale, center = Offset(rx, ry))
        }
        ResourceType.COMBUSTIBLE -> {
            drawCircle(color = Color(0x44FF7043), radius = 12f * scale, center = Offset(rx, ry))
            drawCircle(color = Color(0xFFFF7043), radius = 5.5f * scale, center = Offset(rx, ry))
        }
    }
}

private fun DrawScope.drawAtmosphericOverlay(
    engine: GameEngine,
    width: Float,
    height: Float
) {
    // Cinematic Vignette
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x22000000), Color(0x9905080C)),
            center = Offset(width * 0.5f, height * 0.5f),
            radius = width * 0.65f
        ),
        size = Size(width, height)
    )

    // Moving Mist
    val mistOffset = (System.currentTimeMillis() * 0.04f) % width
    for (i in 0..1) {
        val mx = (mistOffset + i * width) % (width * 2) - width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0x14B0BEC5), Color.Transparent)
            ),
            topLeft = Offset(mx, height * 0.45f),
            size = Size(width, height * 0.35f)
        )
    }
}

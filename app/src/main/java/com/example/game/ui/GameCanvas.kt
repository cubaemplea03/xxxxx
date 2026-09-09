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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

        // 8b. Draw Enemy Projectiles (Boss plasma blasts / energy projectiles)
        for (ep in engine.enemyProjectiles) {
            val sx = (ep.x - viewLeft) * scale
            val sy = ep.y * scale
            val pColor = Color(ep.color)

            // Outer plasma aura
            drawCircle(
                color = pColor.copy(alpha = 0.35f),
                radius = (ep.radius * 2.2f) * scale,
                center = Offset(sx, sy)
            )
            // Mid energy ball
            drawCircle(
                color = pColor.copy(alpha = 0.85f),
                radius = (ep.radius * 1.3f) * scale,
                center = Offset(sx, sy)
            )
            // Hot white core
            drawCircle(
                color = Color.White,
                radius = (ep.radius * 0.65f) * scale,
                center = Offset(sx, sy)
            )
            // Fiery tail
            drawLine(
                color = pColor.copy(alpha = 0.7f),
                start = Offset(sx - ep.vx * 0.045f * scale, sy - ep.vy * 0.045f * scale),
                end = Offset(sx, sy),
                strokeWidth = (ep.radius * 1.1f) * scale
            )
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
        2 -> Color(0xFF2C1610) // Reddish desert ballast
        3 -> Color(0xFF242E35) // Frozen gravel ballast
        else -> Color(0xFF181B1F) // Dark industrial crushed ballast
    }

    // Deep sub-ballast bed
    drawRect(
        color = groundColor,
        topLeft = Offset(0f, groundY),
        size = Size(width, height - groundY)
    )

    // Ballast gravel texture granules
    val ballastGranuleSpacing = 16f * scale
    var gx = -(viewLeft * 0.8f * scale) % ballastGranuleSpacing
    while (gx < width) {
        drawCircle(
            color = Color(0x33455A64),
            radius = 1.6f * scale,
            center = Offset(gx, groundY + 14f * scale)
        )
        drawCircle(
            color = Color(0x22FFFFFF),
            radius = 1.2f * scale,
            center = Offset(gx + 8f * scale, groundY + 8f * scale)
        )
        gx += ballastGranuleSpacing
    }

    // Moving railroad ties (Durmientes de madera tratada con creosota)
    val tieSpacing = 28f * scale
    val offsetMovement = if (engine.levelPhase == LevelPhase.ARRIVING || engine.levelPhase == LevelPhase.DEPARTING) {
        engine.trainWheelOffset * scale
    } else {
        viewLeft * scale
    }
    val tieOffset = -offsetMovement % tieSpacing
    var tx = tieOffset - tieSpacing
    while (tx < width + tieSpacing) {
        // Tie shadow and body
        drawRect(
            color = Color(0xFF1B1714),
            topLeft = Offset(tx - 0.8f * scale, groundY + 3.5f * scale),
            size = Size(12.5f * scale, 13.5f * scale)
        )
        drawRect(
            color = Color(0xFF342A22),
            topLeft = Offset(tx, groundY + 4f * scale),
            size = Size(11f * scale, 12f * scale)
        )
        // Wood grain chamfer highlight
        drawRect(
            color = Color(0xFF4A3D33),
            topLeft = Offset(tx + 1f * scale, groundY + 4f * scale),
            size = Size(9f * scale, 1.8f * scale)
        )
        // Cast-steel tie plates (Placas de asiento metálicas)
        drawRect(
            color = Color(0xFF263238),
            topLeft = Offset(tx + 1.5f * scale, groundY + 2f * scale),
            size = Size(8f * scale, 3f * scale)
        )
        // Rail fastening spikes (Pernos de fijación)
        drawCircle(
            color = Color(0xFF90A4AE),
            radius = 0.8f * scale,
            center = Offset(tx + 2.5f * scale, groundY + 3.5f * scale)
        )
        drawCircle(
            color = Color(0xFF90A4AE),
            radius = 0.8f * scale,
            center = Offset(tx + 8.5f * scale, groundY + 3.5f * scale)
        )
        tx += tieSpacing
    }

    // Heavy Steel Rail Base Flange
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(0f, groundY + 3f * scale),
        size = Size(width, 2.5f * scale)
    )
    // Rail Web (Alma del riel en sombra)
    drawRect(
        color = Color(0xFF37474F),
        topLeft = Offset(0f, groundY + 1.8f * scale),
        size = Size(width, 2f * scale)
    )
    // Polished Specular Steel Railhead (Cabeza del riel pulida y brillante)
    drawRect(
        color = Color(0xFFB0BEC5),
        topLeft = Offset(0f, groundY + 0.8f * scale),
        size = Size(width, 1.4f * scale)
    )
    // Mirror light glint on top edge
    drawRect(
        color = Color(0xFFFFFFFF),
        topLeft = Offset(0f, groundY + 0.5f * scale),
        size = Size(width, 0.6f * scale)
    )

    // Station Marker Platform (at x = 800)
    val stationX = (800f - viewLeft) * scale
    if (stationX > -350f * scale && stationX < width + 350f * scale) {
        // Platform masonry foundation
        drawRect(
            color = Color(0xFF263238),
            topLeft = Offset(stationX - 140f * scale, groundY - 10f * scale),
            size = Size(280f * scale, 10f * scale)
        )
        drawRect(
            color = Color(0xFF455A64),
            topLeft = Offset(stationX - 138f * scale, groundY - 9f * scale),
            size = Size(276f * scale, 7f * scale)
        )
        // Yellow safety curb with hazard diagonal warning stripes
        val curbW = 276f * scale
        val curbH = 3f * scale
        val curbX = stationX - 138f * scale
        val curbY = groundY - 9f * scale
        drawRect(
            color = Color(0xFFFFB300),
            topLeft = Offset(curbX, curbY),
            size = Size(curbW, curbH)
        )
        var hx = curbX
        while (hx < curbX + curbW) {
            drawLine(
                color = Color(0xFF212121),
                start = Offset(hx, curbY + curbH),
                end = Offset(hx + 4f * scale, curbY),
                strokeWidth = 1.8f * scale
            )
            hx += 8f * scale
        }

        // Station sign post (Cast iron column)
        drawRect(
            color = Color(0xFF1E282D),
            topLeft = Offset(stationX - 4f * scale, groundY - 65f * scale),
            size = Size(8f * scale, 56f * scale)
        )
        drawRect(
            color = Color(0xFF546E7A),
            topLeft = Offset(stationX - 2f * scale, groundY - 65f * scale),
            size = Size(4f * scale, 56f * scale)
        )
        // Station sign board with wooden frame
        drawRect(
            color = Color(0xFF3E2723),
            topLeft = Offset(stationX - 75f * scale, groundY - 86f * scale),
            size = Size(150f * scale, 24f * scale)
        )
        drawRect(
            color = Color(0xFFD32F2F),
            topLeft = Offset(stationX - 71f * scale, groundY - 83f * scale),
            size = Size(142f * scale, 18f * scale)
        )
        drawRect(
            color = Color(0xFFFFD54F),
            topLeft = Offset(stationX - 67f * scale, groundY - 80f * scale),
            size = Size(134f * scale, 12f * scale)
        )
        // Vintage Station Gas Lantern
        drawCircle(
            color = Color(0x33FFD54F),
            radius = 18f * scale,
            center = Offset(stationX, groundY - 95f * scale)
        )
        drawCircle(
            color = Color(0xFFFFEB3B),
            radius = 5f * scale,
            center = Offset(stationX, groundY - 95f * scale)
        )
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = 2.5f * scale,
            center = Offset(stationX, groundY - 95f * scale)
        )
    }
}

private fun DrawScope.drawRivetRow(
    startX: Float,
    endX: Float,
    y: Float,
    count: Int,
    scale: Float,
    color: Color = Color(0xFF607D8B)
) {
    if (count <= 1) return
    val step = (endX - startX) / (count - 1)
    for (i in 0 until count) {
        val rx = startX + i * step
        drawCircle(color = Color(0xFF141A1E), radius = 1.3f * scale, center = Offset(rx, y + 0.5f * scale))
        drawCircle(color = color, radius = 1.0f * scale, center = Offset(rx, y))
    }
}

private fun DrawScope.drawRivetColumn(
    x: Float,
    startY: Float,
    endY: Float,
    count: Int,
    scale: Float,
    color: Color = Color(0xFF607D8B)
) {
    if (count <= 1) return
    val step = (endY - startY) / (count - 1)
    for (i in 0 until count) {
        val ry = startY + i * step
        drawCircle(color = Color(0xFF141A1E), radius = 1.3f * scale, center = Offset(x, ry + 0.5f * scale))
        drawCircle(color = color, radius = 1.0f * scale, center = Offset(x, ry))
    }
}

/**
 * Real 2-Axle Bogie Truck (Carretilla de bogie ferroviario con resortes, cajas de grasa y ruedas radiadas)
 */
private fun DrawScope.drawBogieTruck(
    centerX: Float,
    floorScreenY: Float,
    scale: Float,
    wheelAngle: Float
) {
    val wheelRadius = 8.5f * scale
    val wheelY = floorScreenY + 6.5f * scale
    val axleSpacing = 15.5f * scale
    val wLeft = centerX - axleSpacing
    val wRight = centerX + axleSpacing

    // Bogie side equalizer frame (Bastidor de acero fundido)
    val frameY = floorScreenY + 2.5f * scale
    drawRect(
        color = Color(0xFF171F24),
        topLeft = Offset(centerX - 24f * scale, frameY),
        size = Size(48f * scale, 4f * scale)
    )
    drawRect(
        color = Color(0xFF29363D),
        topLeft = Offset(centerX - 22f * scale, frameY + 0.6f * scale),
        size = Size(44f * scale, 2.8f * scale)
    )

    // Center suspension bolster depression & dual coil springs
    val springCenterX = centerX
    val springY = floorScreenY + 4f * scale
    drawRect(
        color = Color(0xFF1B2329),
        topLeft = Offset(springCenterX - 6f * scale, springY),
        size = Size(12f * scale, 5.5f * scale)
    )
    // Coil spring ridges
    for (s in 0..2) {
        drawLine(
            color = Color(0xFF78909C),
            start = Offset(springCenterX - 5f * scale, springY + 1.2f * scale + s * 1.5f * scale),
            end = Offset(springCenterX + 5f * scale, springY + 1.2f * scale + s * 1.5f * scale),
            strokeWidth = 1.2f * scale
        )
    }

    // Two Flanged Steel Wheels
    listOf(wLeft, wRight).forEach { wx ->
        // Outer wheel flange (Pestaña del riel)
        drawCircle(
            color = Color(0xFF13191E),
            radius = wheelRadius + 1.2f * scale,
            center = Offset(wx, wheelY)
        )
        // Wheel steel rim tread (Llanta de acero)
        drawCircle(
            color = Color(0xFF263238),
            radius = wheelRadius,
            center = Offset(wx, wheelY)
        )
        // Specular chrome reflection band on wheel tread
        drawCircle(
            color = Color(0xFF78909C),
            radius = wheelRadius * 0.85f,
            center = Offset(wx, wheelY)
        )
        // Inset hub dish
        drawCircle(
            color = Color(0xFF192126),
            radius = wheelRadius * 0.65f,
            center = Offset(wx, wheelY)
        )

        // Rotating wheel spokes (Radios que giran según el avance)
        for (i in 0..3) {
            val spokeA = wheelAngle + i * (PI.toFloat() / 2f)
            val sx = wx + cos(spokeA) * (wheelRadius * 0.58f)
            val sy = wheelY + sin(spokeA) * (wheelRadius * 0.58f)
            drawLine(
                color = Color(0xFF546E7A),
                start = Offset(wx, wheelY),
                end = Offset(sx, sy),
                strokeWidth = 1.3f * scale
            )
        }

        // Axle center pin
        drawCircle(
            color = Color(0xFFB0BEC5),
            radius = wheelRadius * 0.25f,
            center = Offset(wx, wheelY)
        )

        // Journal Box over axle hub (Caja de grasa con tapa abisagrada y pernos)
        drawRect(
            color = Color(0xFF1E282D),
            topLeft = Offset(wx - 3f * scale, wheelY - 3f * scale),
            size = Size(6f * scale, 6f * scale)
        )
        drawRect(
            color = Color(0xFF37474F),
            topLeft = Offset(wx - 2.2f * scale, wheelY - 2.2f * scale),
            size = Size(4.4f * scale, 4.4f * scale)
        )
        // Brass grease cap center bolt
        drawCircle(
            color = Color(0xFFFFCA28),
            radius = 1.0f * scale,
            center = Offset(wx, wheelY)
        )
    }

    // Brake shoe pads hovering near wheel treads
    drawRect(
        color = Color(0xFF101418),
        topLeft = Offset(wLeft - wheelRadius - 1.2f * scale, wheelY - 3f * scale),
        size = Size(1.8f * scale, 6f * scale)
    )
    drawRect(
        color = Color(0xFF101418),
        topLeft = Offset(wRight + wheelRadius - 0.6f * scale, wheelY - 3f * scale),
        size = Size(1.8f * scale, 6f * scale)
    )
}

/**
 * Under-chassis equipment: I-beam frame, air brake reservoir tank, triple valve & train line pipe
 */
private fun DrawScope.drawTrainUndercarriage(
    wx: Float,
    wWidth: Float,
    floorScreenY: Float,
    scale: Float
) {
    // Heavy Steel I-Beam Side Sill
    drawRect(
        color = Color(0xFF161E23),
        topLeft = Offset(wx, floorScreenY - 1f * scale),
        size = Size(wWidth, 3.5f * scale)
    )
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(wx, floorScreenY),
        size = Size(wWidth, 1.8f * scale)
    )

    // Air Brake Trainline Pipe spanning the bottom edge
    drawRect(
        color = Color(0xFF37474F),
        topLeft = Offset(wx, floorScreenY + 2.5f * scale),
        size = Size(wWidth, 1.2f * scale)
    )

    // Suspended Compressed Air Brake Reservoir Tank in center
    val tankW = (wWidth * 0.28f).coerceAtMost(55f * scale)
    val tankX = wx + (wWidth - tankW) * 0.5f
    val tankY = floorScreenY + 3.2f * scale
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF37474F), Color(0xFF1E282D), Color(0xFF12171A)),
            startY = tankY,
            endY = tankY + 6.5f * scale
        ),
        topLeft = Offset(tankX, tankY),
        size = Size(tankW, 6.5f * scale)
    )
    // Steel tank mounting straps
    drawRect(
        color = Color(0xFF546E7A),
        topLeft = Offset(tankX + 4f * scale, tankY - 0.5f * scale),
        size = Size(2f * scale, 7.5f * scale)
    )
    drawRect(
        color = Color(0xFF546E7A),
        topLeft = Offset(tankX + tankW - 6f * scale, tankY - 0.5f * scale),
        size = Size(2f * scale, 7.5f * scale)
    )
}

/**
 * Inter-car Coupler: Heavy Janney Knuckle Coupler head, flexible rubber air brake hose & fold-down footplate
 */
private fun DrawScope.drawInterCarConnection(
    rightEdgeX: Float,
    floorScreenY: Float,
    scale: Float
) {
    val couplerLen = 20f * scale

    // Janney Knuckle Coupler shank & drawbar
    drawRect(
        color = Color(0xFF12171A),
        topLeft = Offset(rightEdgeX, floorScreenY - 3f * scale),
        size = Size(couplerLen, 4.5f * scale)
    )
    // Coupler knuckle locking head
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(rightEdgeX + couplerLen * 0.35f, floorScreenY - 4.5f * scale),
        size = Size(couplerLen * 0.35f, 7.5f * scale)
    )
    // Draft gear cushion springs
    drawRect(
        color = Color(0xFF37474F),
        topLeft = Offset(rightEdgeX + 2f * scale, floorScreenY - 2f * scale),
        size = Size(4f * scale, 2.5f * scale)
    )

    // Flexible rubber air brake hose drooping in catenary curve
    val hosePath = Path().apply {
        moveTo(rightEdgeX + 2f * scale, floorScreenY + 2f * scale)
        quadraticTo(
            rightEdgeX + couplerLen * 0.5f,
            floorScreenY + 9f * scale,
            rightEdgeX + couplerLen - 2f * scale,
            floorScreenY + 2f * scale
        )
    }
    drawPath(
        path = hosePath,
        color = Color(0xFF141A1E),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f * scale)
    )
    // Brass glad-hand coupling in center of hose
    drawCircle(
        color = Color(0xFFFFB300),
        radius = 1.3f * scale,
        center = Offset(rightEdgeX + couplerLen * 0.5f, floorScreenY + 8f * scale)
    )

    // Fold-down steel treadplate bridge
    drawRect(
        color = Color(0xFF455A64),
        topLeft = Offset(rightEdgeX + 1f * scale, floorScreenY - 6.5f * scale),
        size = Size(couplerLen - 2f * scale, 1.8f * scale)
    )
}

/**
 * Detailed Locomotive (Index 4):
 * Heavy steam boiler with metallic gradient, riveted boiler bands, engineer's cab with glowing arched windows,
 * front smokebox cap with locking wheel and numberplate, smokestack with billowing turbulent smoke,
 * steam dome, sand dome, brass whistle, cowcatcher with hazard stripes & ramming spikes,
 * searchlight with volumetric beam, large driver wheels with reciprocating connecting rods and steam cylinder!
 */
private fun DrawScope.drawDetailedLocomotive(
    wx: Float,
    wy: Float,
    wWidth: Float,
    wHeight: Float,
    floorScreenY: Float,
    scale: Float,
    engine: GameEngine,
    wheelAngle: Float
) {
    val cabWidth = 100f * scale
    val boilerStartX = wx + cabWidth
    val boilerEndX = wx + wWidth - 25f * scale
    val boilerWidth = boilerEndX - boilerStartX
    val boilerTopY = wy + 16f * scale
    val boilerHeight = floorScreenY - boilerTopY

    // 1. Horizontal Cylindrical Boiler with metallic cylindrical shading
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A2329),
                Color(0xFF37474F),
                Color(0xFF455A64),
                Color(0xFF263238),
                Color(0xFF151C21)
            ),
            startY = boilerTopY,
            endY = floorScreenY
        ),
        topLeft = Offset(boilerStartX, boilerTopY),
        size = Size(boilerWidth, boilerHeight)
    )

    // 2. Front Smokebox (Caja de humos de fundición gris oscura)
    val smokeboxWidth = 48f * scale
    val smokeboxStartX = boilerEndX - smokeboxWidth
    drawRect(
        color = Color(0xFF171E23),
        topLeft = Offset(smokeboxStartX, boilerTopY - 1f * scale),
        size = Size(smokeboxWidth, boilerHeight + 1f * scale)
    )
    // Smokebox convex front boiler door (Tapa frontal abovedada de la caldera)
    val frontCapPath = Path().apply {
        moveTo(boilerEndX, boilerTopY - 1f * scale)
        quadraticTo(boilerEndX + 18f * scale, boilerTopY + boilerHeight * 0.5f, boilerEndX, floorScreenY)
        close()
    }
    drawPath(frontCapPath, color = Color(0xFF14191D))

    // Front smokebox door radial locking dog-clamps & central handwheel
    val capCenterY = boilerTopY + boilerHeight * 0.5f
    val capCenterX = boilerEndX + 8f * scale
    drawCircle(color = Color(0xFF263238), radius = 6f * scale, center = Offset(capCenterX, capCenterY))
    drawCircle(color = Color(0xFFFFCA28), radius = 2.5f * scale, center = Offset(capCenterX, capCenterY))
    // Number plate banner on smokebox: "EXP-404"
    drawRect(
        color = Color(0xFFB71C1C),
        topLeft = Offset(capCenterX - 7f * scale, capCenterY - 14f * scale),
        size = Size(14f * scale, 5.5f * scale)
    )
    drawRect(
        color = Color(0xFFFFD54F),
        topLeft = Offset(capCenterX - 6f * scale, capCenterY - 13f * scale),
        size = Size(12f * scale, 3.5f * scale)
    )

    // 3. Riveted Brass Boiler Bands (Zunchos de caldera con remaches dorados)
    val bandPositions = listOf(0.18f, 0.40f, 0.65f, 0.88f)
    for (bp in bandPositions) {
        val bx = boilerStartX + boilerWidth * bp
        drawRect(
            color = Color(0xFF101417),
            topLeft = Offset(bx - 2.5f * scale, boilerTopY),
            size = Size(5f * scale, boilerHeight)
        )
        drawRect(
            color = Color(0xFFB58E34),
            topLeft = Offset(bx - 1.2f * scale, boilerTopY),
            size = Size(2.4f * scale, boilerHeight)
        )
        // Rivet studs along the band
        drawRivetColumn(bx, boilerTopY + 4f * scale, floorScreenY - 4f * scale, count = 7, scale = scale, color = Color(0xFFFFE082))
    }

    // 4. Running Board Walkway & Safety Handrail along boiler side
    val runningBoardY = boilerTopY + boilerHeight * 0.65f
    drawRect(
        color = Color(0xFF1E282D),
        topLeft = Offset(boilerStartX, runningBoardY),
        size = Size(boilerWidth + 12f * scale, 3.5f * scale)
    )
    drawRect(
        color = Color(0xFF455A64),
        topLeft = Offset(boilerStartX, runningBoardY),
        size = Size(boilerWidth + 12f * scale, 1.2f * scale)
    )
    // Handrail pipe with vertical stanchions
    val railPipeY = runningBoardY - 14f * scale
    drawLine(
        color = Color(0xFFFFCA28),
        start = Offset(boilerStartX, railPipeY),
        end = Offset(boilerEndX + 6f * scale, railPipeY),
        strokeWidth = 2f * scale
    )
    for (st in 0..4) {
        val sx = boilerStartX + (boilerWidth / 4f) * st
        drawLine(
            color = Color(0xFF78909C),
            start = Offset(sx, runningBoardY),
            end = Offset(sx, railPipeY),
            strokeWidth = 1.6f * scale
        )
    }

    // External Brass Steam Delivery & Air Pipes along boiler
    drawLine(
        color = Color(0xFFD4AF37),
        start = Offset(boilerStartX + 20f * scale, boilerTopY + 6f * scale),
        end = Offset(boilerEndX - 8f * scale, boilerTopY + 6f * scale),
        strokeWidth = 2.4f * scale
    )
    // Compressed Air Brake Cylinder on running board
    val airCylX = boilerStartX + boilerWidth * 0.35f
    val airCylY = runningBoardY - 7f * scale
    drawRect(
        color = Color(0xFF1E262C),
        topLeft = Offset(airCylX, airCylY),
        size = Size(36f * scale, 6.5f * scale)
    )
    drawRect(
        color = Color(0xFF37474F),
        topLeft = Offset(airCylX + 1f * scale, airCylY + 0.8f * scale),
        size = Size(34f * scale, 3f * scale)
    )

    // 5. Sand Dome & Steam Dome & Brass Whistle
    // Sand Dome (Cúpula de arena frontal)
    val sandDomeX = boilerStartX + boilerWidth * 0.42f
    val sandDomeY = boilerTopY - 8f * scale
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(sandDomeX - 7f * scale, sandDomeY),
        size = Size(14f * scale, 8f * scale)
    )
    drawCircle(color = Color(0xFF37474F), radius = 7f * scale, center = Offset(sandDomeX, sandDomeY))
    // Sand delivery pipes descending to wheels
    drawLine(
        color = Color(0xFF546E7A),
        start = Offset(sandDomeX + 4f * scale, sandDomeY + 4f * scale),
        end = Offset(sandDomeX + 16f * scale, floorScreenY + 2f * scale),
        strokeWidth = 1.5f * scale
    )

    // Steam Dome (Domo de vapor principal en centro)
    val steamDomeX = boilerStartX + boilerWidth * 0.18f
    val steamDomeY = boilerTopY - 11f * scale
    drawRect(
        color = Color(0xFF1E282D),
        topLeft = Offset(steamDomeX - 9f * scale, steamDomeY),
        size = Size(18f * scale, 11f * scale)
    )
    drawCircle(color = Color(0xFF37474F), radius = 9f * scale, center = Offset(steamDomeX, steamDomeY))
    // Polished Brass Safety Pressure Relief Valves
    drawRect(
        color = Color(0xFFFFD54F),
        topLeft = Offset(steamDomeX - 4f * scale, steamDomeY - 6f * scale),
        size = Size(3f * scale, 6f * scale)
    )
    drawRect(
        color = Color(0xFFFFD54F),
        topLeft = Offset(steamDomeX + 1f * scale, steamDomeY - 6f * scale),
        size = Size(3f * scale, 6f * scale)
    )

    // Vintage Brass Steam Whistle with subtle live steam wisp
    val whistleX = steamDomeX - 16f * scale
    val whistleY = boilerTopY - 8f * scale
    drawLine(
        color = Color(0xFFFFCA28),
        start = Offset(whistleX, boilerTopY),
        end = Offset(whistleX, whistleY),
        strokeWidth = 2.2f * scale
    )
    drawRect(
        color = Color(0xFFFFD700),
        topLeft = Offset(whistleX - 2f * scale, whistleY - 4f * scale),
        size = Size(4f * scale, 5f * scale)
    )
    // Whistle steam wisp
    drawCircle(
        color = Color(0x55FFFFFF),
        radius = 2.8f * scale,
        center = Offset(whistleX - 3f * scale, whistleY - 7f * scale)
    )

    // 6. Tapered Smokestack (Chimenea de fundición con corona abocinada)
    val stackCenterX = smokeboxStartX + smokeboxWidth * 0.45f
    val stackBaseY = boilerTopY
    val stackHeight = 24f * scale
    val stackTopY = stackBaseY - stackHeight
    val stackPath = Path().apply {
        moveTo(stackCenterX - 8f * scale, stackBaseY)
        lineTo(stackCenterX - 6.5f * scale, stackTopY + 4f * scale)
        lineTo(stackCenterX - 9f * scale, stackTopY)
        lineTo(stackCenterX + 9f * scale, stackTopY)
        lineTo(stackCenterX + 6.5f * scale, stackTopY + 4f * scale)
        lineTo(stackCenterX + 8f * scale, stackBaseY)
        close()
    }
    drawPath(stackPath, color = Color(0xFF14191D))
    // Brass rim collar at top of stack
    drawRect(
        color = Color(0xFFFFB300),
        topLeft = Offset(stackCenterX - 9.5f * scale, stackTopY),
        size = Size(19f * scale, 2.5f * scale)
    )

    // 7. Billowing Volumetric Steam & Smoke Puffs from the smokestack
    val smokePhase = (System.currentTimeMillis() * 0.005f)
    for (p in 0..4) {
        val puffAge = (smokePhase + p * 0.8f) % 4f
        val puffProgress = puffAge / 4f
        val puffX = stackCenterX - puffProgress * (110f * scale)
        val puffY = stackTopY - 8f * scale - puffProgress * (35f * scale)
        val puffRadius = (9f + puffProgress * 18f) * scale
        val puffAlpha = (0.75f - puffProgress * 0.7f).coerceIn(0f, 0.75f)
        drawCircle(
            color = Color(0xFF455A64).copy(alpha = puffAlpha),
            radius = puffRadius,
            center = Offset(puffX, puffY)
        )
        // Fiery cinder ember inside newly born smoke
        if (p == 0 || p == 1) {
            drawCircle(
                color = Color(0xFFFF7043).copy(alpha = (0.9f - puffProgress).coerceAtLeast(0f)),
                radius = 1.8f * scale,
                center = Offset(puffX + 2f * scale, puffY + 1f * scale)
            )
        }
    }

    // 8. Engineer's Cab (Cabina del Maquinista con techo curvado)
    drawRect(
        color = Color(0xFF1E272C),
        topLeft = Offset(wx, wy),
        size = Size(cabWidth, wHeight)
    )
    // Cab roof curved canopy with overhanging rain eaves
    val cabRoofPath = Path().apply {
        moveTo(wx - 4f * scale, wy + 5f * scale)
        quadraticTo(wx + cabWidth * 0.5f, wy - 4f * scale, wx + cabWidth + 5f * scale, wy + 5f * scale)
        lineTo(wx + cabWidth + 4f * scale, wy + 8f * scale)
        lineTo(wx - 3f * scale, wy + 8f * scale)
        close()
    }
    drawPath(cabRoofPath, color = Color(0xFF14191D))

    // Arched Cab Windows with warm incandescent golden lantern glow
    val winX = wx + 26f * scale
    val winY = wy + 14f * scale
    val winW = 32f * scale
    val winH = 26f * scale
    // Window outer wooden frame
    drawRect(
        color = Color(0xFF0F1519),
        topLeft = Offset(winX - 2f * scale, winY - 2f * scale),
        size = Size(winW + 4f * scale, winH + 4f * scale)
    )
    // Warm golden lantern light with internal glow
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFFF8F00)),
            center = Offset(winX + winW * 0.5f, winY + winH * 0.5f),
            radius = winW * 0.8f
        ),
        topLeft = Offset(winX, winY),
        size = Size(winW, winH)
    )
    // Dark window mullions (Cruceta de la ventana)
    drawLine(
        color = Color(0xFF1E282D),
        start = Offset(winX + winW * 0.5f, winY),
        end = Offset(winX + winW * 0.5f, winY + winH),
        strokeWidth = 2.2f * scale
    )
    drawLine(
        color = Color(0xFF1E282D),
        start = Offset(winX, winY + winH * 0.5f),
        end = Offset(winX + winW, winY + winH * 0.5f),
        strokeWidth = 2.2f * scale
    )
    // Silhouette of pressure gauge dials inside the cab
    drawCircle(
        color = Color(0xAA181C20),
        radius = 4f * scale,
        center = Offset(winX + winW * 0.72f, winY + winH * 0.65f)
    )

    // Exterior Cab Climbing Ladder & Grab Iron
    val cabLadderX = wx + 10f * scale
    drawLine(
        color = Color(0xFFFFCA28),
        start = Offset(cabLadderX, wy + 10f * scale),
        end = Offset(cabLadderX, floorScreenY),
        strokeWidth = 2.2f * scale
    )
    for (r in 0..4) {
        val ry = wy + 20f * scale + r * 11f * scale
        drawLine(
            color = Color(0xFF78909C),
            start = Offset(cabLadderX - 4f * scale, ry),
            end = Offset(cabLadderX + 4f * scale, ry),
            strokeWidth = 2f * scale
        )
    }

    // 9. Firebox / Furnace Door (Hogar de la caldera con fuego de carbón incandescente)
    val furnaceX = wx + cabWidth + 10f * scale
    val furnaceY = floorScreenY - 32f * scale
    val furnaceW = 34f * scale
    val furnaceH = 28f * scale
    // Cast iron frame
    drawRect(
        color = Color(0xFF13181C),
        topLeft = Offset(furnaceX - 2f * scale, furnaceY - 2f * scale),
        size = Size(furnaceW + 4f * scale, furnaceH + 4f * scale)
    )
    // Glowing coal bed
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFB300), Color(0xFFFF5722), Color(0xFFB71C1C)),
            startY = furnaceY,
            endY = furnaceY + furnaceH
        ),
        topLeft = Offset(furnaceX, furnaceY),
        size = Size(furnaceW, furnaceH)
    )
    // Furnace door ventilation slits
    for (v in 0..2) {
        val vy = furnaceY + 6f * scale + v * 6.5f * scale
        drawLine(
            color = Color(0xFF263238),
            start = Offset(furnaceX + 4f * scale, vy),
            end = Offset(furnaceX + furnaceW - 4f * scale, vy),
            strokeWidth = 2f * scale
        )
    }
    // Dynamic flickering firelight glow casting on floor
    val fireFlicker = (sin(System.currentTimeMillis() * 0.015f) * 0.15f + 0.85f).coerceIn(0.6f, 1f)
    drawCircle(
        color = Color(0x66FF5722).copy(alpha = 0.35f * fireFlicker),
        radius = 28f * scale * fireFlicker,
        center = Offset(furnaceX + furnaceW * 0.5f, furnaceY + furnaceH * 0.5f)
    )

    // 10. Heavy Front Cowcatcher / Pilot (Quitanieves de acero pesado con franjas de peligro)
    val hasArmor = engine.trainWeapons.armorLevel > 0
    val plowLen = (if (hasArmor) 38f else 28f) * scale
    val plowH = (if (hasArmor) 36f else 28f) * scale
    val plowStartX = wx + wWidth
    val plowPath = Path().apply {
        moveTo(plowStartX, floorScreenY)
        lineTo(plowStartX + plowLen, floorScreenY)
        lineTo(plowStartX, floorScreenY - plowH)
        close()
    }
    drawPath(plowPath, color = Color(0xFF1E282D))
    // Diagonal industrial safety hazard stripes on plow wedge
    for (hz in 0..4) {
        val hx = plowStartX + hz * (plowLen / 5f)
        drawLine(
            color = Color(0xFFFFB300),
            start = Offset(hx, floorScreenY),
            end = Offset(hx + 6f * scale, floorScreenY - 14f * scale),
            strokeWidth = 2.4f * scale
        )
    }
    // Front Janney Coupler on pilot
    drawRect(
        color = Color(0xFF12171A),
        topLeft = Offset(plowStartX + plowLen * 0.6f, floorScreenY - 7f * scale),
        size = Size(14f * scale, 6f * scale)
    )

    // Spikes ramming prow if Armor Upgrade
    if (hasArmor) {
        for (s in 0..2) {
            val spikeY = floorScreenY - (9f + s * 10f) * scale
            val spikeLen = (28f + s * 5f) * scale
            val spikePath = Path().apply {
                moveTo(plowStartX + 10f * scale, spikeY - 3f * scale)
                lineTo(plowStartX + spikeLen, spikeY)
                lineTo(plowStartX + 10f * scale, spikeY + 3f * scale)
                close()
            }
            drawPath(spikePath, color = Color(0xFFFF7043))
            drawLine(
                color = Color(0xFFFFD54F),
                start = Offset(plowStartX + 10f * scale, spikeY),
                end = Offset(plowStartX + spikeLen, spikeY),
                strokeWidth = 1.8f * scale
            )
        }
    }

    // 11. Large Brass/Steel Locomotive Headlight (Gran Farol de Carburo)
    val lightX = wx + wWidth + 6f * scale
    val lightY = boilerTopY + 8f * scale
    val hasSpotlight = engine.trainWeapons.spotlightLevel > 0
    val beamLength = if (hasSpotlight) 560f * scale else 400f * scale
    val lightRadius = (if (hasSpotlight) 11f else 8f) * scale

    // Heavy cast bracket
    drawLine(
        color = Color(0xFF263238),
        start = Offset(wx + wWidth, lightY + 8f * scale),
        end = Offset(lightX, lightY),
        strokeWidth = 3f * scale
    )
    // Round lantern casing
    drawCircle(color = Color(0xFF1B2329), radius = lightRadius + 2f * scale, center = Offset(lightX, lightY))
    drawCircle(color = Color(0xFFFFB300), radius = lightRadius, center = Offset(lightX, lightY))
    // Parabolic silver reflector and incandescent core
    drawCircle(color = Color(0xFFFFF9C4), radius = lightRadius * 0.7f, center = Offset(lightX, lightY))
    drawCircle(color = Color(0xFFFFFFFF), radius = lightRadius * 0.4f, center = Offset(lightX, lightY))

    // Volumetric Spotlight Beam Cone cutting through the night
    val beam = Path().apply {
        moveTo(lightX, lightY)
        lineTo(lightX + beamLength, lightY - 75f * scale)
        lineTo(lightX + beamLength, lightY + 130f * scale)
        close()
    }
    drawPath(
        path = beam,
        brush = Brush.horizontalGradient(
            colors = listOf(
                if (hasSpotlight) Color(0xB3FFF9C4) else Color(0x88FFF59D),
                Color(0x33FFE082),
                Color(0x0AFFE082),
                Color.Transparent
            ),
            startX = lightX,
            endX = lightX + beamLength
        )
    )

    // 12. Giant Driver Wheels, Steam Cylinder & Reciprocating Connecting Rods (Bielas y Pistón)
    val driverRadius = 13.5f * scale
    val driverWheelY = floorScreenY + 6.5f * scale
    val d1 = wx + 58f * scale
    val d2 = wx + 118f * scale
    val d3 = wx + 178f * scale
    val driverXs = listOf(d1, d2, d3)

    // Front 2-wheel pilot truck (Carretilla guía delantera de 2 ruedas pequeñas)
    val pilotRadius = 7.5f * scale
    val p1 = wx + 275f * scale
    val p2 = wx + 315f * scale
    listOf(p1, p2).forEach { px ->
        drawCircle(color = Color(0xFF14191D), radius = pilotRadius + 1f * scale, center = Offset(px, driverWheelY))
        drawCircle(color = Color(0xFF37474F), radius = pilotRadius, center = Offset(px, driverWheelY))
        drawCircle(color = Color(0xFF78909C), radius = pilotRadius * 0.5f, center = Offset(px, driverWheelY))
    }

    // Heavy Steam Cylinder at the lower front (Cilindro de vapor principal)
    val cylX = wx + 225f * scale
    val cylY = floorScreenY - 2f * scale
    val cylW = 42f * scale
    val cylH = 15f * scale
    drawRect(
        color = Color(0xFF14191D),
        topLeft = Offset(cylX, cylY),
        size = Size(cylW, cylH)
    )
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(cylX + 1.5f * scale, cylY + 1.2f * scale),
        size = Size(cylW - 3f * scale, cylH - 2.4f * scale)
    )
    // Cylinder head front round cover
    drawCircle(color = Color(0xFF37474F), radius = 5.5f * scale, center = Offset(cylX + cylW, cylY + cylH * 0.5f))
    drawCircle(color = Color(0xFFFFCA28), radius = 1.8f * scale, center = Offset(cylX + cylW, cylY + cylH * 0.5f))

    // 3 Large Driving Wheels with heavy counterweights
    driverXs.forEach { dwx ->
        // Flange
        drawCircle(color = Color(0xFF13191E), radius = driverRadius + 1.5f * scale, center = Offset(dwx, driverWheelY))
        // Steel Rim Tread
        drawCircle(color = Color(0xFF263238), radius = driverRadius, center = Offset(dwx, driverWheelY))
        drawCircle(color = Color(0xFF78909C), radius = driverRadius * 0.88f, center = Offset(dwx, driverWheelY))
        // Inset dish
        drawCircle(color = Color(0xFF181F24), radius = driverRadius * 0.72f, center = Offset(dwx, driverWheelY))

        // Crescent Counterweight (Contrapeso de fundición)
        val cwPath = Path().apply {
            val cwA = wheelAngle + PI.toFloat()
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    center = Offset(dwx, driverWheelY),
                    radius = driverRadius * 0.72f
                ),
                startAngleDegrees = ((cwA - 0.7f) * 180f / PI).toFloat(),
                sweepAngleDegrees = (1.4f * 180f / PI).toFloat(),
                forceMoveTo = true
            )
            close()
        }
        drawPath(cwPath, color = Color(0xFF37474F))

        // Wheel spokes
        for (sp in 0..5) {
            val a = wheelAngle + sp * (PI.toFloat() / 3f)
            drawLine(
                color = Color(0xFF546E7A),
                start = Offset(dwx, driverWheelY),
                end = Offset(dwx + cos(a) * (driverRadius * 0.7f), driverWheelY + sin(a) * (driverRadius * 0.7f)),
                strokeWidth = 1.5f * scale
            )
        }
        // Center Axle Hub Pin
        drawCircle(color = Color(0xFFB0BEC5), radius = driverRadius * 0.28f, center = Offset(dwx, driverWheelY))
    }

    // Side Coupling Rod (Biela de acoplamiento horizontal uniendo los 3 ejes)
    val crankRadius = 6.5f * scale
    val pinOffsetY = sin(wheelAngle) * crankRadius
    val pinOffsetX = cos(wheelAngle) * crankRadius
    val rodY = driverWheelY + pinOffsetY

    drawLine(
        color = Color(0xFF12171A),
        start = Offset(d1 + pinOffsetX - 3f * scale, rodY + 0.6f * scale),
        end = Offset(d3 + pinOffsetX + 3f * scale, rodY + 0.6f * scale),
        strokeWidth = 4.2f * scale
    )
    drawLine(
        color = Color(0xFFB0BEC5),
        start = Offset(d1 + pinOffsetX, rodY),
        end = Offset(d3 + pinOffsetX, rodY),
        strokeWidth = 2.8f * scale
    )

    // Main Driving Rod (Biela motriz que une la rueda central al pistón del cilindro)
    val pistonCrossheadX = cylX + 8f * scale + (cos(wheelAngle) * 7f * scale)
    val pistonCrossheadY = cylY + cylH * 0.5f
    drawLine(
        color = Color(0xFF12171A),
        start = Offset(d2 + pinOffsetX, rodY + 0.6f * scale),
        end = Offset(pistonCrossheadX, pistonCrossheadY + 0.6f * scale),
        strokeWidth = 4.2f * scale
    )
    drawLine(
        color = Color(0xFFCFD8DC),
        start = Offset(d2 + pinOffsetX, rodY),
        end = Offset(pistonCrossheadX, pistonCrossheadY),
        strokeWidth = 2.8f * scale
    )
    // Crosshead slide block
    drawRect(
        color = Color(0xFF37474F),
        topLeft = Offset(pistonCrossheadX - 4f * scale, pistonCrossheadY - 3.5f * scale),
        size = Size(8f * scale, 7f * scale)
    )

    // Crankpins on all 3 driver wheels
    driverXs.forEach { dwx ->
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = 2.2f * scale,
            center = Offset(dwx + pinOffsetX, rodY)
        )
    }
}

/**
 * Detailed Coal Tender (Index 3):
 * Sloped hopper walls with reinforcement ribs, heaped mound of glistening coal chunks with anthracite highlights,
 * rear water cistern with filler dome and ladder, stowed shovel and emergency fuel barrel, 2 bogie trucks.
 */
private fun DrawScope.drawDetailedTender(
    wx: Float,
    wy: Float,
    wWidth: Float,
    wHeight: Float,
    floorScreenY: Float,
    scale: Float,
    wheelAngle: Float
) {
    val bunkerWidth = wWidth * 0.65f
    val waterWidth = wWidth - bunkerWidth

    // Heavy side sill chassis
    drawTrainUndercarriage(wx, wWidth, floorScreenY, scale)

    // 1. Coal Bunker Body (Tolva de carbón reforzada con remaches)
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(wx, wy),
        size = Size(bunkerWidth, wHeight)
    )
    // Flared top rim on coal bunker
    drawRect(
        color = Color(0xFF192226),
        topLeft = Offset(wx - 2f * scale, wy - 3f * scale),
        size = Size(bunkerWidth + 4f * scale, 4f * scale)
    )
    // Vertical structural reinforcement stiffeners
    for (i in 1..3) {
        val sx = wx + (bunkerWidth / 4f) * i
        drawLine(
            color = Color(0xFF14191D),
            start = Offset(sx, wy),
            end = Offset(sx, floorScreenY),
            strokeWidth = 2.8f * scale
        )
        drawRivetColumn(sx + 2f * scale, wy + 4f * scale, floorScreenY - 4f * scale, count = 5, scale = scale)
    }

    // 2. Mountain of Glistening Anthracite Coal Chunks
    val coalBaseY = wy
    val coalPeakY = wy - 18f * scale
    val coalMound = Path().apply {
        moveTo(wx + 4f * scale, coalBaseY)
        lineTo(wx + bunkerWidth * 0.22f, coalPeakY + 4f * scale)
        lineTo(wx + bunkerWidth * 0.45f, coalPeakY)
        lineTo(wx + bunkerWidth * 0.75f, coalPeakY + 6f * scale)
        lineTo(wx + bunkerWidth - 4f * scale, coalBaseY)
        close()
    }
    drawPath(coalMound, color = Color(0xFF101416))
    // Multi-faceted coal chunk highlights (Brillo cristalino de antracita)
    val coalGlints = listOf(
        Offset(wx + 25f * scale, wy - 8f * scale),
        Offset(wx + 48f * scale, wy - 14f * scale),
        Offset(wx + 72f * scale, wy - 11f * scale),
        Offset(wx + 95f * scale, wy - 5f * scale),
        Offset(wx + 38f * scale, wy - 4f * scale),
        Offset(wx + 82f * scale, wy - 7f * scale)
    )
    coalGlints.forEach { cpt ->
        drawCircle(color = Color(0xFF37474F), radius = 2.4f * scale, center = cpt)
        drawCircle(color = Color(0xFF78909C), radius = 1.0f * scale, center = Offset(cpt.x - 0.5f * scale, cpt.y - 0.5f * scale))
    }

    // Stowed Fireman's Coal Shovel on bunker side
    val shovelX = wx + 18f * scale
    val shovelY = wy + 14f * scale
    drawLine(
        color = Color(0xFF8D6E63),
        start = Offset(shovelX, shovelY + 16f * scale),
        end = Offset(shovelX + 22f * scale, shovelY),
        strokeWidth = 1.8f * scale
    )
    drawRect(
        color = Color(0xFF90A4AE),
        topLeft = Offset(shovelX + 20f * scale, shovelY - 2f * scale),
        size = Size(6f * scale, 8f * scale)
    )

    // 3. Rear Water Cistern Section (Cisterna cilíndrica de agua)
    val waterStartX = wx + bunkerWidth
    drawRect(
        color = Color(0xFF2E3B43),
        topLeft = Offset(waterStartX, wy + 4f * scale),
        size = Size(waterWidth, wHeight - 4f * scale)
    )
    // Water filler dome manhole on top deck
    val fillerX = waterStartX + waterWidth * 0.45f
    val fillerY = wy + 1f * scale
    drawCircle(color = Color(0xFF1E282D), radius = 7f * scale, center = Offset(fillerX, fillerY))
    drawCircle(color = Color(0xFF455A64), radius = 5.5f * scale, center = Offset(fillerX, fillerY))
    drawCircle(color = Color(0xFFFFCA28), radius = 1.5f * scale, center = Offset(fillerX, fillerY))

    // Water level vertical sight glass
    val glassX = waterStartX + 12f * scale
    drawLine(
        color = Color(0xFFB0BEC5),
        start = Offset(glassX, wy + 10f * scale),
        end = Offset(glassX, floorScreenY - 8f * scale),
        strokeWidth = 2.2f * scale
    )
    drawLine(
        color = Color(0xFF40C4FF),
        start = Offset(glassX, wy + 18f * scale),
        end = Offset(glassX, floorScreenY - 8f * scale),
        strokeWidth = 1.2f * scale
    )

    // Rear inspection ladder to water deck
    val ladderX = waterStartX + waterWidth - 14f * scale
    drawLine(color = Color(0xFF78909C), start = Offset(ladderX, wy + 4f * scale), end = Offset(ladderX, floorScreenY), strokeWidth = 2f * scale)
    drawLine(color = Color(0xFF78909C), start = Offset(ladderX + 8f * scale, wy + 4f * scale), end = Offset(ladderX + 8f * scale, floorScreenY), strokeWidth = 2f * scale)
    for (r in 0..3) {
        val ry = wy + 10f * scale + r * 7f * scale
        drawLine(color = Color(0xFFB0BEC5), start = Offset(ladderX, ry), end = Offset(ladderX + 8f * scale, ry), strokeWidth = 1.5f * scale)
    }

    // Stenciled livery text on water tank: "AGUA 18KL / CARBÓN 12T"
    drawRect(
        color = Color(0xFF1B242A),
        topLeft = Offset(waterStartX + 18f * scale, wy + 15f * scale),
        size = Size(38f * scale, 8f * scale)
    )
    drawRect(
        color = Color(0xFFB0BEC5),
        topLeft = Offset(waterStartX + 20f * scale, wy + 17f * scale),
        size = Size(34f * scale, 3.5f * scale)
    )

    // 4. Two Bogie Trucks
    drawBogieTruck(wx + 42f * scale, floorScreenY, scale, wheelAngle)
    drawBogieTruck(wx + wWidth - 42f * scale, floorScreenY, scale, wheelAngle)
}

/**
 * Detailed Armored Boxcar (Index 2):
 * Heavy riveted armor plating, diagonal X-girders, heavy sliding center freight door with roller track & locking bar,
 * vision embrasures with amber interior lantern light, rooftop diamond catwalk with curved ladder, heavy cannon turret.
 */
private fun DrawScope.drawDetailedArmoredBoxcar(
    wx: Float,
    wy: Float,
    wWidth: Float,
    wHeight: Float,
    floorScreenY: Float,
    scale: Float,
    engine: GameEngine,
    wheelAngle: Float
) {
    // Heavy undercarriage
    drawTrainUndercarriage(wx, wWidth, floorScreenY, scale)

    // 1. Armor Plated Body with vertical panel divisions
    drawRect(
        color = Color(0xFF26333D),
        topLeft = Offset(wx, wy),
        size = Size(wWidth, wHeight)
    )

    // Diagonal X-girders (Vigas diagonales de refuerzo militar)
    val panelWidth = wWidth / 3f
    for (p in 0..2) {
        val px = wx + p * panelWidth
        // Cross X-lines
        drawLine(
            color = Color(0xFF1A232A),
            start = Offset(px + 4f * scale, wy + 4f * scale),
            end = Offset(px + panelWidth - 4f * scale, floorScreenY - 4f * scale),
            strokeWidth = 2.6f * scale
        )
        drawLine(
            color = Color(0xFF1A232A),
            start = Offset(px + 4f * scale, floorScreenY - 4f * scale),
            end = Offset(px + panelWidth - 4f * scale, wy + 4f * scale),
            strokeWidth = 2.6f * scale
        )
        // Vertical divider ribs with rivets
        drawLine(
            color = Color(0xFF151C22),
            start = Offset(px, wy),
            end = Offset(px, floorScreenY),
            strokeWidth = 3f * scale
        )
        drawRivetColumn(px + 3f * scale, wy + 4f * scale, floorScreenY - 4f * scale, count = 7, scale = scale)
    }
    // Perimeter rivet rows along top and bottom
    drawRivetRow(wx + 6f * scale, wx + wWidth - 6f * scale, wy + 3f * scale, count = 18, scale = scale)
    drawRivetRow(wx + 6f * scale, wx + wWidth - 6f * scale, floorScreenY - 4f * scale, count = 18, scale = scale)

    // 2. Massive Sliding Freight Cargo Door in center (wx + 105f to wx + 195f)
    val doorX = wx + wWidth * 0.35f
    val doorW = wWidth * 0.30f
    val doorY = wy + 6f * scale
    val doorH = wHeight - 8f * scale

    // Top door roller track rail
    drawRect(
        color = Color(0xFF151D22),
        topLeft = Offset(doorX - 12f * scale, doorY - 4f * scale),
        size = Size(doorW + 24f * scale, 3.5f * scale)
    )
    // Hanging roller wheels
    drawCircle(color = Color(0xFF78909C), radius = 2.5f * scale, center = Offset(doorX + 8f * scale, doorY - 2.5f * scale))
    drawCircle(color = Color(0xFF78909C), radius = 2.5f * scale, center = Offset(doorX + doorW - 8f * scale, doorY - 2.5f * scale))

    // Sliding Door panel with recessed steel plates
    drawRect(
        color = Color(0xFF1E2830),
        topLeft = Offset(doorX, doorY),
        size = Size(doorW, doorH)
    )
    drawRect(
        color = Color(0xFF2E3D48),
        topLeft = Offset(doorX + 3f * scale, doorY + 3f * scale),
        size = Size(doorW - 6f * scale, doorH - 6f * scale)
    )
    // Heavy central locking bar and padlock
    drawLine(
        color = Color(0xFF12171A),
        start = Offset(doorX + doorW * 0.5f, doorY + 6f * scale),
        end = Offset(doorX + doorW * 0.5f, doorY + doorH - 6f * scale),
        strokeWidth = 3f * scale
    )
    drawRect(
        color = Color(0xFFFFB300),
        topLeft = Offset(doorX + doorW * 0.5f - 3f * scale, doorY + doorH * 0.5f - 4f * scale),
        size = Size(6f * scale, 8f * scale)
    )

    // 3. Armored Vision Slits with warm interior lantern glow peeking through
    val slitXs = listOf(wx + 35f * scale, wx + wWidth - 55f * scale)
    slitXs.forEach { sx ->
        // Armor embrasure housing
        drawRect(
            color = Color(0xFF131A20),
            topLeft = Offset(sx - 2f * scale, wy + 16f * scale),
            size = Size(20f * scale, 8f * scale)
        )
        // Glowing slit
        drawRect(
            color = Color(0xFFFFD54F),
            topLeft = Offset(sx, wy + 18f * scale),
            size = Size(16f * scale, 4f * scale)
        )
        // Dark horizontal protective slit bar
        drawLine(
            color = Color(0xFF263238),
            start = Offset(sx, wy + 20f * scale),
            end = Offset(sx + 16f * scale, wy + 20f * scale),
            strokeWidth = 1.2f * scale
        )
    }

    // 4. Stencil Military Markings: "VAGÓN BLINDADO // R-02"
    drawRect(
        color = Color(0xFF182229),
        topLeft = Offset(wx + 18f * scale, floorScreenY - 18f * scale),
        size = Size(62f * scale, 7f * scale)
    )
    drawRect(
        color = Color(0xFFB0BEC5),
        topLeft = Offset(wx + 20f * scale, floorScreenY - 16.5f * scale),
        size = Size(58f * scale, 3.5f * scale)
    )

    // 5. Rooftop Catwalk & Climbing Ladder
    // Diamond-tread roof walkway
    drawRect(
        color = Color(0xFF455A64),
        topLeft = Offset(wx + 4f * scale, wy - 3.5f * scale),
        size = Size(wWidth - 8f * scale, 3.5f * scale)
    )
    // Safety kick-plate edges
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(wx + 4f * scale, wy - 4.5f * scale),
        size = Size(wWidth - 8f * scale, 1.2f * scale)
    )

    // End Climbing Ladder with curved grab-irons over the roof
    val ladderX = wx + 14f * scale
    drawLine(color = Color(0xFF78909C), start = Offset(ladderX, wy - 8f * scale), end = Offset(ladderX, floorScreenY), strokeWidth = 2.4f * scale)
    drawLine(color = Color(0xFF78909C), start = Offset(ladderX + 11f * scale, wy - 8f * scale), end = Offset(ladderX + 11f * scale, floorScreenY), strokeWidth = 2.4f * scale)
    for (r in 0..5) {
        val ry = wy + 4f * scale + r * 10.5f * scale
        drawLine(color = Color(0xFFB0BEC5), start = Offset(ladderX, ry), end = Offset(ladderX + 11f * scale, ry), strokeWidth = 2f * scale)
    }

    // 6. HEAVY CANNON UPGRADE (Mounted on roof at center of Armored Wagon)
    if (engine.trainWeapons.cannonLevel > 0) {
        val cannonCenterX = wx + wWidth * 0.5f
        val cannonCenterY = wy - 14f * scale

        // Heavy cupola turret armor base
        drawCircle(color = Color(0xFF14191D), radius = 18f * scale, center = Offset(cannonCenterX, wy - 4f * scale))
        drawCircle(color = Color(0xFF37474F), radius = 14f * scale, center = Offset(cannonCenterX, cannonCenterY))
        drawRivetRow(cannonCenterX - 12f * scale, cannonCenterX + 12f * scale, wy - 4f * scale, count = 6, scale = scale)

        // Cannon Barrel pointing at cannonAngle
        val angle = engine.trainWeapons.cannonAngle
        val barrelLen = (26f + engine.trainWeapons.cannonLevel * 6f) * scale
        val barrelEndX = cannonCenterX + cos(angle) * barrelLen
        val barrelEndY = cannonCenterY + sin(angle) * barrelLen

        // Heavy hydraulic recoil cylinders along barrel
        drawLine(
            color = Color(0xFF1B2329),
            start = Offset(cannonCenterX, cannonCenterY),
            end = Offset(barrelEndX, barrelEndY),
            strokeWidth = 7f * scale
        )
        drawLine(
            color = Color(0xFF455A64),
            start = Offset(cannonCenterX, cannonCenterY),
            end = Offset(barrelEndX, barrelEndY),
            strokeWidth = 5f * scale
        )
        // Cannon muzzle brake tip
        drawCircle(color = Color(0xFF90A4AE), radius = 4.5f * scale, center = Offset(barrelEndX, barrelEndY))

        // Cannon Muzzle Flash Flare
        if (engine.trainWeapons.cannonMuzzleTimer > 0f) {
            drawCircle(color = Color(0xFFFF5722), radius = 16f * scale, center = Offset(barrelEndX, barrelEndY))
            drawCircle(color = Color(0xFFFFEB3B), radius = 9f * scale, center = Offset(barrelEndX, barrelEndY))
            drawCircle(color = Color(0xFFFFFFFF), radius = 4f * scale, center = Offset(barrelEndX, barrelEndY))
        }
    }

    // 7. Two Bogie Trucks
    drawBogieTruck(wx + 52f * scale, floorScreenY, scale, wheelAngle)
    drawBogieTruck(wx + wWidth - 52f * scale, floorScreenY, scale, wheelAngle)
}

/**
 * Detailed Flatcar / Plataforma Artillada (Index 1):
 * Heavy fishbelly steel I-beam frame with oval lightening cutouts, weathered timber plank decking,
 * cargo ammo crates with steel bands and chained oil barrels, Gatling turret, 2 bogie trucks.
 */
private fun DrawScope.drawDetailedFlatcar(
    wx: Float,
    wy: Float,
    wWidth: Float,
    wHeight: Float,
    floorScreenY: Float,
    scale: Float,
    engine: GameEngine,
    wheelAngle: Float
) {
    // 1. Heavy Fishbelly Steel Center Sill Chassis (Viga I con curva panzona para soportar carga pesada)
    val fishbelly = Path().apply {
        moveTo(wx, floorScreenY)
        lineTo(wx + wWidth * 0.15f, floorScreenY + 6f * scale)
        lineTo(wx + wWidth * 0.85f, floorScreenY + 6f * scale)
        lineTo(wx + wWidth, floorScreenY)
        close()
    }
    drawPath(fishbelly, color = Color(0xFF1A2228))
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(wx, floorScreenY - 1f * scale),
        size = Size(wWidth, 4f * scale)
    )

    // Oval lightening holes in fishbelly frame
    for (h in 0..4) {
        val hx = wx + wWidth * 0.22f + h * (wWidth * 0.12f)
        drawCircle(color = Color(0xFF101518), radius = 2.4f * scale, center = Offset(hx, floorScreenY + 3.2f * scale))
    }

    // 2. Weathered Timber Railway Plank Deck (Tablones de madera gruesa de ferrocarril)
    val deckH = 5f * scale
    val deckY = floorScreenY - deckH
    drawRect(
        color = Color(0xFF3E2F27),
        topLeft = Offset(wx, deckY),
        size = Size(wWidth, deckH)
    )
    // Individual plank lines and nail bolts
    val plankCount = 18
    val plankW = wWidth / plankCount
    for (p in 0..plankCount) {
        val px = wx + p * plankW
        drawLine(
            color = Color(0xFF231A15),
            start = Offset(px, deckY),
            end = Offset(px, deckY + deckH),
            strokeWidth = 1.2f * scale
        )
        drawCircle(color = Color(0xFF5D4037), radius = 0.7f * scale, center = Offset(px + plankW * 0.5f, deckY + 1.2f * scale))
    }
    // Side stake pockets along the flatcar edge
    for (spk in 0..6) {
        val sx = wx + 12f * scale + spk * (wWidth - 24f * scale) / 6f
        drawRect(
            color = Color(0xFF1B2329),
            topLeft = Offset(sx - 2f * scale, deckY),
            size = Size(4f * scale, deckH)
        )
    }

    // 3. Stowed Cargo: Ammo Crates and Chained Steel Oil Barrels
    val cargoStartX = wx + 20f * scale
    // Wooden ammo crate
    val crateW = 34f * scale
    val crateH = 26f * scale
    val crateY = deckY - crateH
    drawRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(cargoStartX, crateY),
        size = Size(crateW, crateH)
    )
    // Steel reinforcement bands
    drawRect(color = Color(0xFF263238), topLeft = Offset(cargoStartX + 4f * scale, crateY), size = Size(3f * scale, crateH))
    drawRect(color = Color(0xFF263238), topLeft = Offset(cargoStartX + crateW - 7f * scale, crateY), size = Size(3f * scale, crateH))
    // Stencil text: "MUNICIÓN"
    drawRect(color = Color(0xFFFFB300), topLeft = Offset(cargoStartX + 10f * scale, crateY + 10f * scale), size = Size(14f * scale, 4f * scale))

    // Steel Fuel / Oil Drums chained down
    val drumX = cargoStartX + crateW + 8f * scale
    val drumW = 16f * scale
    val drumH = 24f * scale
    val drumY = deckY - drumH
    drawRect(
        color = Color(0xFF2E7D32),
        topLeft = Offset(drumX, drumY),
        size = Size(drumW, drumH)
    )
    // Barrel ribs
    drawLine(color = Color(0xFF1B5E20), start = Offset(drumX, drumY + 7f * scale), end = Offset(drumX + drumW, drumY + 7f * scale), strokeWidth = 1.6f * scale)
    drawLine(color = Color(0xFF1B5E20), start = Offset(drumX, drumY + 16f * scale), end = Offset(drumX + drumW, drumY + 16f * scale), strokeWidth = 1.6f * scale)
    // Tie-down chain crossing the barrel
    drawLine(
        color = Color(0xFF78909C),
        start = Offset(drumX - 3f * scale, deckY),
        end = Offset(drumX + drumW + 3f * scale, drumY + 4f * scale),
        strokeWidth = 1.4f * scale
    )

    // 4. GATLING / TURRET UPGRADE (Mounted on Flatcar)
    if (engine.trainWeapons.turretLevel > 0) {
        val turretCenterX = wx + wWidth * 0.5f
        val turretCenterY = deckY - 16f * scale

        // Heavy bolted tripod stand
        drawLine(
            color = Color(0xFF1E262C),
            start = Offset(turretCenterX - 16f * scale, deckY),
            end = Offset(turretCenterX, turretCenterY),
            strokeWidth = 3.5f * scale
        )
        drawLine(
            color = Color(0xFF1E262C),
            start = Offset(turretCenterX + 16f * scale, deckY),
            end = Offset(turretCenterX, turretCenterY),
            strokeWidth = 3.5f * scale
        )
        // Turret swivel dome
        drawCircle(color = Color(0xFF151B1F), radius = 10f * scale, center = Offset(turretCenterX, turretCenterY))
        drawCircle(color = Color(0xFF37474F), radius = 7.5f * scale, center = Offset(turretCenterX, turretCenterY))
        drawCircle(color = Color(0xFF00E676), radius = 2.4f * scale, center = Offset(turretCenterX, turretCenterY - 4f * scale))

        // Twin Gatling barrels pointing at turretAngle
        val angle = engine.trainWeapons.turretAngle
        val bLen = (20f + engine.trainWeapons.turretLevel * 4f) * scale
        val bEndX = turretCenterX + cos(angle) * bLen
        val bEndY = turretCenterY + sin(angle) * bLen

        drawLine(
            color = Color(0xFF1E282D),
            start = Offset(turretCenterX, turretCenterY),
            end = Offset(bEndX, bEndY),
            strokeWidth = 5f * scale
        )
        drawLine(
            color = Color(0xFF90A4AE),
            start = Offset(turretCenterX, turretCenterY),
            end = Offset(bEndX, bEndY),
            strokeWidth = 2.8f * scale
        )

        // Turret Muzzle Flash
        if (engine.trainWeapons.turretMuzzleTimer > 0f) {
            drawCircle(color = Color(0xFFFFD54F), radius = 10f * scale, center = Offset(bEndX, bEndY))
            drawCircle(color = Color(0xFFFFFFFF), radius = 4f * scale, center = Offset(bEndX, bEndY))
        }
    }

    // 5. Two Bogie Trucks
    drawBogieTruck(wx + 44f * scale, floorScreenY, scale, wheelAngle)
    drawBogieTruck(wx + wWidth - 44f * scale, floorScreenY, scale, wheelAngle)
}

/**
 * Detailed Caboose (Index 0):
 * Open-air rear observation balcony with wrought-iron railings & handbrake wheel, classic grooved siding,
 * center observation cupola (lookout tower) with 3 glowing windows, potbelly stove chimney with smoke wisp,
 * pulsing crimson Fresnel railroad marker lantern casting light onto rails behind.
 */
private fun DrawScope.drawDetailedCaboose(
    wx: Float,
    wy: Float,
    wWidth: Float,
    wHeight: Float,
    floorScreenY: Float,
    scale: Float,
    wheelAngle: Float
) {
    // Undercarriage
    drawTrainUndercarriage(wx, wWidth, floorScreenY, scale)

    val balconyW = 32f * scale
    val bodyStartX = wx + balconyW
    val bodyW = wWidth - balconyW - 10f * scale

    // 1. Open Rear Observation Balcony & Porch
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(wx, floorScreenY - 3.5f * scale),
        size = Size(balconyW, 3.5f * scale)
    )
    // Decorative wrought iron railing
    val railH = 18f * scale
    val railTopY = floorScreenY - railH
    drawLine(color = Color(0xFF151B1F), start = Offset(wx + 2f * scale, railTopY), end = Offset(bodyStartX, railTopY), strokeWidth = 2.4f * scale)
    drawLine(color = Color(0xFF78909C), start = Offset(wx + 2f * scale, railTopY), end = Offset(bodyStartX, railTopY), strokeWidth = 1.2f * scale)
    // Railing vertical pickets
    for (pk in 0..4) {
        val px = wx + 3f * scale + pk * (balconyW - 5f * scale) / 4f
        drawLine(color = Color(0xFF37474F), start = Offset(px, railTopY), end = Offset(px, floorScreenY - 3.5f * scale), strokeWidth = 1.6f * scale)
    }
    // Handbrake wheel (Volante de freno de mano de ferrocarril)
    val brakeStaffX = wx + 12f * scale
    drawLine(color = Color(0xFF263238), start = Offset(brakeStaffX, railTopY - 6f * scale), end = Offset(brakeStaffX, floorScreenY - 3.5f * scale), strokeWidth = 2f * scale)
    drawCircle(color = Color(0xFFD32F2F), radius = 4f * scale, center = Offset(brakeStaffX, railTopY - 6f * scale))
    drawCircle(color = Color(0xFFFFCA28), radius = 1.5f * scale, center = Offset(brakeStaffX, railTopY - 6f * scale))

    // Access steps down to tracks
    drawRect(color = Color(0xFF1E282D), topLeft = Offset(wx, floorScreenY), size = Size(8f * scale, 6f * scale))

    // 2. Enclosed Caboose Body with Antique Grooved Wooden Siding
    drawRect(
        color = Color(0xFF4E2622),
        topLeft = Offset(bodyStartX, wy),
        size = Size(bodyW, wHeight)
    )
    // Horizontal tongue-and-groove siding lines
    val sidingRows = 8
    val sidingH = wHeight / sidingRows
    for (sr in 1 until sidingRows) {
        val sry = wy + sr * sidingH
        drawLine(
            color = Color(0xFF321714),
            start = Offset(bodyStartX, sry),
            end = Offset(bodyStartX + bodyW, sry),
            strokeWidth = 1.2f * scale
        )
    }
    // Corner reinforcement trim
    drawRect(color = Color(0xFF261210), topLeft = Offset(bodyStartX, wy), size = Size(3.5f * scale, wHeight))
    drawRect(color = Color(0xFF261210), topLeft = Offset(bodyStartX + bodyW - 3.5f * scale, wy), size = Size(3.5f * scale, wHeight))

    // Arched Side Window with warm golden incandescent interior glow
    val cWinX = bodyStartX + bodyW * 0.65f
    val cWinY = wy + 14f * scale
    val cWinW = 24f * scale
    val cWinH = 22f * scale
    drawRect(color = Color(0xFF1A1210), topLeft = Offset(cWinX - 2f * scale, cWinY - 2f * scale), size = Size(cWinW + 4f * scale, cWinH + 4f * scale))
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFFF8F00)),
            center = Offset(cWinX + cWinW * 0.5f, cWinY + cWinH * 0.5f),
            radius = cWinW * 0.7f
        ),
        topLeft = Offset(cWinX, cWinY),
        size = Size(cWinW, cWinH)
    )
    // Window mullion cross
    drawLine(color = Color(0xFF3E2723), start = Offset(cWinX + cWinW * 0.5f, cWinY), end = Offset(cWinX + cWinW * 0.5f, cWinY + cWinH), strokeWidth = 1.8f * scale)
    drawLine(color = Color(0xFF3E2723), start = Offset(cWinX, cWinY + cWinH * 0.5f), end = Offset(cWinX + cWinW, cWinY + cWinH * 0.5f), strokeWidth = 1.8f * scale)

    // 3. Iconic Raised Roof Cupola (Torre de Observación / Vigía)
    val cupolaX = bodyStartX + bodyW * 0.22f
    val cupolaW = 44f * scale
    val cupolaH = 18f * scale
    val cupolaY = wy - cupolaH
    drawRect(
        color = Color(0xFF3E1F1C),
        topLeft = Offset(cupolaX, cupolaY),
        size = Size(cupolaW, cupolaH)
    )
    // Cupola curved roof canopy
    val cupolaRoof = Path().apply {
        moveTo(cupolaX - 3f * scale, cupolaY + 2f * scale)
        quadraticTo(cupolaX + cupolaW * 0.5f, cupolaY - 3f * scale, cupolaX + cupolaW + 3f * scale, cupolaY + 2f * scale)
        lineTo(cupolaX + cupolaW + 2f * scale, cupolaY + 4f * scale)
        lineTo(cupolaX - 2f * scale, cupolaY + 4f * scale)
        close()
    }
    drawPath(cupolaRoof, color = Color(0xFF1E282D))

    // 3 Glowing Windows on Cupola
    for (cw in 0..2) {
        val cwx = cupolaX + 4f * scale + cw * 13f * scale
        val cwy = cupolaY + 5f * scale
        drawRect(color = Color(0xFFFFD54F), topLeft = Offset(cwx, cwy), size = Size(10f * scale, 9f * scale))
        drawLine(color = Color(0xFF3E2723), start = Offset(cwx + 5f * scale, cwy), end = Offset(cwx + 5f * scale, cwy + 9f * scale), strokeWidth = 1.2f * scale)
    }

    // 4. Roof Stovepipe Chimney with subtle woodsmoke
    val pipeX = bodyStartX + bodyW - 20f * scale
    val pipeY = wy - 14f * scale
    drawLine(color = Color(0xFF1A1D20), start = Offset(pipeX, wy), end = Offset(pipeX, pipeY), strokeWidth = 3f * scale)
    drawRect(color = Color(0xFF37474F), topLeft = Offset(pipeX - 3f * scale, pipeY - 2f * scale), size = Size(6f * scale, 2.5f * scale))
    // Smoke wisp
    val woodSmokePhase = (System.currentTimeMillis() * 0.003f)
    for (sw in 0..2) {
        val swY = pipeY - (4f + sw * 5f) * scale
        val swX = pipeX - (sw * 4f) * scale + sin(woodSmokePhase + sw).toFloat() * 2f * scale
        drawCircle(color = Color(0x33CFD8DC), radius = (2f + sw * 1.5f) * scale, center = Offset(swX, swY))
    }

    // 5. Authentic Crimson Railroad Marker Lantern (Farol de Cola / Farol de Vía)
    val lanternX = wx + 3f * scale
    val lanternY = railTopY - 6f * scale
    val pulseRed = (sin(System.currentTimeMillis() * 0.008f) * 0.2f + 0.8f).toFloat()

    // Heavy lantern bracket & housing
    drawLine(color = Color(0xFF1B2329), start = Offset(wx + 8f * scale, railTopY), end = Offset(lanternX, lanternY), strokeWidth = 2.2f * scale)
    drawCircle(color = Color(0xFF14191D), radius = 7f * scale, center = Offset(lanternX, lanternY))
    drawCircle(color = Color(0xFFB71C1C), radius = 5.2f * scale, center = Offset(lanternX, lanternY))
    // Ruby Fresnel lens glowing core
    drawCircle(color = Color(0xFFFF1744), radius = 3.8f * scale, center = Offset(lanternX, lanternY))
    drawCircle(color = Color(0xFFFF8A80), radius = 1.6f * scale, center = Offset(lanternX, lanternY))

    // Pulsing crimson halo casting light backward onto tracks
    drawCircle(
        color = Color(0xFFFF1744).copy(alpha = 0.35f * pulseRed),
        radius = 26f * scale * pulseRed,
        center = Offset(lanternX, lanternY)
    )
    val redBeam = Path().apply {
        moveTo(lanternX, lanternY)
        lineTo(lanternX - 140f * scale, lanternY - 35f * scale)
        lineTo(lanternX - 140f * scale, lanternY + 65f * scale)
        close()
    }
    drawPath(
        path = redBeam,
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0x55FF1744).copy(alpha = 0.4f * pulseRed), Color.Transparent),
            startX = lanternX,
            endX = lanternX - 140f * scale
        )
    )

    // 6. Two Bogie Trucks
    drawBogieTruck(bodyStartX + 32f * scale, floorScreenY, scale, wheelAngle)
    drawBogieTruck(bodyStartX + bodyW - 32f * scale, floorScreenY, scale, wheelAngle)
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

    // Dynamic wheel rotation angle synchronized to train travel
    val wheelAngle = (engine.trainWheelOffset * 0.16f) % (2f * PI.toFloat())

    // Render corresponding authentic train car based on wagon index
    when (w.index) {
        4 -> drawDetailedLocomotive(wx, wy, wWidth, wHeight, floorScreenY, scale, engine, wheelAngle)
        3 -> drawDetailedTender(wx, wy, wWidth, wHeight, floorScreenY, scale, wheelAngle)
        2 -> drawDetailedArmoredBoxcar(wx, wy, wWidth, wHeight, floorScreenY, scale, engine, wheelAngle)
        1 -> drawDetailedFlatcar(wx, wy, wWidth, wHeight, floorScreenY, scale, engine, wheelAngle)
        0 -> drawDetailedCaboose(wx, wy, wWidth, wHeight, floorScreenY, scale, wheelAngle)
    }

    // Inter-car Janney Knuckle Coupler & flexible air brake hose (for wagons 0, 1, 2, 3 connecting to next car)
    if (w.index < 4) {
        drawInterCarConnection(wx + wWidth, floorScreenY, scale)
    }

    // Scavenge Supply Crate with brass hardware and glow
    if (w.hasCrate) {
        val cx = wx + wWidth * 0.7f - 14f * scale
        val cy = (if (w.hasRoof) w.roofY else w.floorY) * scale - 24f * scale
        if (!w.crateOpened) {
            // Unopened military supply crate
            drawRect(
                color = Color(0xFF6D4C41),
                topLeft = Offset(cx, cy),
                size = Size(28f * scale, 24f * scale)
            )
            // Steel corner brackets
            drawRect(color = Color(0xFF3E2723), topLeft = Offset(cx, cy), size = Size(5f * scale, 24f * scale))
            drawRect(color = Color(0xFF3E2723), topLeft = Offset(cx + 23f * scale, cy), size = Size(5f * scale, 24f * scale))
            drawRect(color = Color(0xFF3E2723), topLeft = Offset(cx, cy), size = Size(28f * scale, 4f * scale))
            // Gold padlock
            drawRect(
                color = Color(0xFFFFD54F),
                topLeft = Offset(cx + 10f * scale, cy + 9f * scale),
                size = Size(8f * scale, 7f * scale)
            )
            drawCircle(
                color = Color(0x33FFCA28),
                radius = 22f * scale,
                center = Offset(cx + 14f * scale, cy + 12f * scale)
            )
        } else {
            // Opened broken crate
            drawRect(
                color = Color(0xFF3E2723),
                topLeft = Offset(cx, cy + 8f * scale),
                size = Size(28f * scale, 16f * scale)
            )
            drawRect(
                color = Color(0xFF1E1513),
                topLeft = Offset(cx + 4f * scale, cy + 8f * scale),
                size = Size(20f * scale, 6f * scale)
            )
        }
    }
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

    drawCharacterMannequin(
        headSkin = player.headSkin,
        chestSkin = player.chestSkin,
        legsSkin = player.legsSkin,
        px = px,
        py = py,
        scale = scale,
        facingRight = player.facingRight,
        animFrame = player.animFrame,
        isCrouch = player.isCrouching,
        showWeapon = true
    )
}

internal fun DrawScope.drawCharacterMannequin(
    headSkin: String,
    chestSkin: String,
    legsSkin: String,
    px: Float,
    py: Float,
    scale: Float,
    facingRight: Boolean = true,
    animFrame: Float = 0f,
    isCrouch: Boolean = false,
    showWeapon: Boolean = true
) {
    val dir = if (facingRight) 1f else -1f
    val animOffset = (sin(animFrame.toDouble()) * 3f * scale).toFloat()

    // 1. Shadow beneath character
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(px - 14f * scale, py - 4f * scale),
        size = Size(28f * scale, 8f * scale)
    )

    // 2. Legs / Boots customization
    val legHeight = if (isCrouch) 10f * scale else 16f * scale
    val legY = py - legHeight

    when (legsSkin.uppercase()) {
        "LEGS_TACTICAL" -> {
            // Tactical camo cargo + steel knee pads
            drawRect(
                color = Color(0xFF2B3A33),
                topLeft = Offset(px - 6f * scale, legY),
                size = Size(5.5f * scale, legHeight)
            )
            drawRect(
                color = Color(0xFF2B3A33),
                topLeft = Offset(px + 1f * scale, legY + animOffset),
                size = Size(5.5f * scale, legHeight)
            )
            // Steel Knee guards
            drawRect(
                color = Color(0xFF78909C),
                topLeft = Offset(px - 6.5f * scale, legY + 5f * scale),
                size = Size(6.5f * scale, 4f * scale)
            )
            drawRect(
                color = Color(0xFF78909C),
                topLeft = Offset(px + 0.5f * scale, legY + 5f * scale + animOffset),
                size = Size(6.5f * scale, 4f * scale)
            )
            // Yellow ammo pouch strap
            drawRect(
                color = Color(0xFFFFCA28),
                topLeft = Offset(px - 5f * scale, legY + 2f * scale),
                size = Size(4f * scale, 2f * scale)
            )
        }
        "LEGS_CYBER" -> {
            // Dark navy exo-frame + neon cyan hydraulics
            drawRect(
                color = Color(0xFF101C24),
                topLeft = Offset(px - 6f * scale, legY),
                size = Size(5.5f * scale, legHeight)
            )
            drawRect(
                color = Color(0xFF101C24),
                topLeft = Offset(px + 1f * scale, legY + animOffset),
                size = Size(5.5f * scale, legHeight)
            )
            // Cyan led lines
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(px - 3.5f * scale, legY),
                end = Offset(px - 3.5f * scale, legY + legHeight),
                strokeWidth = 1.5f * scale
            )
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(px + 3.5f * scale, legY + animOffset),
                end = Offset(px + 3.5f * scale, legY + legHeight + animOffset),
                strokeWidth = 1.5f * scale
            )
            // Ankle thrusters
            drawCircle(
                color = Color(0xFF00B0FF),
                radius = 2f * scale,
                center = Offset(px - 4f * scale, py - 2f * scale)
            )
            drawCircle(
                color = Color(0xFF00B0FF),
                radius = 2f * scale,
                center = Offset(px + 4f * scale, py - 2f * scale + animOffset)
            )
        }
        "LEGS_PUNKER" -> {
            // Spiked grebas + chain wraps
            drawRect(
                color = Color(0xFF2C1B18),
                topLeft = Offset(px - 6f * scale, legY),
                size = Size(6f * scale, legHeight)
            )
            drawRect(
                color = Color(0xFF2C1B18),
                topLeft = Offset(px + 1f * scale, legY + animOffset),
                size = Size(6f * scale, legHeight)
            )
            // Steel spiked knee caps
            drawRect(
                color = Color(0xFFCFD8DC),
                topLeft = Offset(px - 7f * scale, legY + 4f * scale),
                size = Size(4f * scale, 4f * scale)
            )
            drawRect(
                color = Color(0xFFCFD8DC),
                topLeft = Offset(px + 3f * scale, legY + 4f * scale + animOffset),
                size = Size(4f * scale, 4f * scale)
            )
            // Blood-red studs
            drawCircle(color = Color(0xFFFF1744), radius = 1.2f * scale, center = Offset(px - 5f * scale, legY + 6f * scale))
            drawCircle(color = Color(0xFFFF1744), radius = 1.2f * scale, center = Offset(px + 5f * scale, legY + 6f * scale + animOffset))
        }
        "LEGS_TITAN" -> {
            // Heavy Colossal Bogie Armor + Molten Conduit
            drawRect(
                color = Color(0xFF1A2226),
                topLeft = Offset(px - 7f * scale, legY),
                size = Size(6.5f * scale, legHeight)
            )
            drawRect(
                color = Color(0xFF1A2226),
                topLeft = Offset(px + 0.5f * scale, legY + animOffset),
                size = Size(6.5f * scale, legHeight)
            )
            // Gold edge trim
            drawRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(px - 7f * scale, legY + legHeight - 4f * scale),
                size = Size(6.5f * scale, 3f * scale)
            )
            drawRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(px + 0.5f * scale, legY + legHeight - 4f * scale + animOffset),
                size = Size(6.5f * scale, 3f * scale)
            )
            // Molten vent
            drawCircle(color = Color(0xFFFF5722), radius = 2.2f * scale, center = Offset(px - 3.5f * scale, legY + 6f * scale))
            drawCircle(color = Color(0xFFFF5722), radius = 2.2f * scale, center = Offset(px + 3.5f * scale, legY + 6f * scale + animOffset))
        }
        else -> {
            // LEGS_DEFAULT: Classic survivor work pants
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
            // Leather boot tips
            drawRect(
                color = Color(0xFF1E282D),
                topLeft = Offset(px - 6f * scale, py - 4f * scale),
                size = Size(5f * scale, 4f * scale)
            )
            drawRect(
                color = Color(0xFF1E282D),
                topLeft = Offset(px + 1f * scale, py - 4f * scale + animOffset),
                size = Size(5f * scale, 4f * scale)
            )
        }
    }

    // 3. Torso (Pecho) customization
    val bodyHeight = if (isCrouch) 16f * scale else 22f * scale
    val bodyY = legY - bodyHeight

    when (chestSkin.uppercase()) {
        "CHEST_TACTICAL" -> {
            // Kevlar vest + ammo pouches + green radio LED
            drawRect(
                color = Color(0xFF1E2D2F),
                topLeft = Offset(px - 7.5f * scale, bodyY),
                size = Size(15f * scale, bodyHeight)
            )
            drawRect(
                color = Color(0xFF37474F),
                topLeft = Offset(px - 5.5f * scale, bodyY + 3f * scale),
                size = Size(11f * scale, bodyHeight - 6f * scale)
            )
            // Ammo magazine pouch
            drawRect(
                color = Color(0xFF455A64),
                topLeft = Offset(px - 4f * scale, bodyY + bodyHeight - 7f * scale),
                size = Size(8f * scale, 5f * scale)
            )
            // Comms LED
            drawCircle(
                color = Color(0xFF76FF03),
                radius = 1.6f * scale,
                center = Offset(px - 4f * scale * dir, bodyY + 4f * scale)
            )
        }
        "CHEST_CYBER" -> {
            // Nanotech exo-armor + bright cyan conduits + kinetic power core
            drawRect(
                color = Color(0xFF0B132B),
                topLeft = Offset(px - 7.5f * scale, bodyY),
                size = Size(15f * scale, bodyHeight)
            )
            // Circuit conduits
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(px - 6f * scale, bodyY + 2f * scale),
                end = Offset(px, bodyY + 9f * scale),
                strokeWidth = 1.8f * scale
            )
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(px + 6f * scale, bodyY + 2f * scale),
                end = Offset(px, bodyY + 9f * scale),
                strokeWidth = 1.8f * scale
            )
            // Kinetic Reactor Core
            drawCircle(
                color = Color(0xFF80D8FF),
                radius = 3.5f * scale,
                center = Offset(px, bodyY + 9f * scale)
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 1.8f * scale,
                center = Offset(px, bodyY + 9f * scale)
            )
        }
        "CHEST_PUNKER" -> {
            // Berserker leather jacket + crossed iron chains + spiked pauldrons
            drawRect(
                color = Color(0xFF3E1218),
                topLeft = Offset(px - 8f * scale, bodyY),
                size = Size(16f * scale, bodyHeight)
            )
            // Crossed steel chains
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(px - 7f * scale, bodyY + 2f * scale),
                end = Offset(px + 7f * scale, bodyY + bodyHeight - 2f * scale),
                strokeWidth = 2f * scale
            )
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(px + 7f * scale, bodyY + 2f * scale),
                end = Offset(px - 7f * scale, bodyY + bodyHeight - 2f * scale),
                strokeWidth = 2f * scale
            )
            // Spiked shoulder pads
            drawCircle(
                color = Color(0xFFFF5252),
                radius = 2.8f * scale,
                center = Offset(px - 8f * scale, bodyY + 3f * scale)
            )
            drawCircle(
                color = Color(0xFFFF5252),
                radius = 2.8f * scale,
                center = Offset(px + 8f * scale, bodyY + 3f * scale)
            )
        }
        "CHEST_TITAN" -> {
            // Heavy Locomotive Boiler Plate + Molten Magma Reactor
            drawRect(
                color = Color(0xFF192227),
                topLeft = Offset(px - 8.5f * scale, bodyY),
                size = Size(17f * scale, bodyHeight)
            )
            // Gold trim
            drawRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(px - 7f * scale, bodyY + 1f * scale),
                size = Size(14f * scale, 3f * scale)
            )
            // Molten Plasma Core
            drawCircle(
                color = Color(0xFFFF3D00),
                radius = 4.2f * scale,
                center = Offset(px, bodyY + 10f * scale)
            )
            drawCircle(
                color = Color(0xFFFFEB3B),
                radius = 2f * scale,
                center = Offset(px, bodyY + 10f * scale)
            )
            // Heat vent exhaust
            drawRect(
                color = Color(0xFF37474F),
                topLeft = Offset(px - 5f * scale, bodyY + bodyHeight - 5f * scale),
                size = Size(10f * scale, 4f * scale)
            )
        }
        else -> {
            // CHEST_DEFAULT: Classic leather coat + red bandana
            drawRect(
                color = Color(0xFF4E342E),
                topLeft = Offset(px - 7f * scale, bodyY),
                size = Size(14f * scale, bodyHeight)
            )
            drawRect(
                color = Color(0xFFD32F2F),
                topLeft = Offset(px - 6f * scale, bodyY - 1f * scale),
                size = Size(12f * scale, 5f * scale)
            )
        }
    }

    // 4. Head (Cabeza) customization
    val headRadius = 6.5f * scale
    val headY = bodyY - headRadius

    when (headSkin.uppercase()) {
        "HEAD_TACTICAL" -> {
            // SWAT Kevlar helmet + Amber blast visor + Flashlight beam
            drawCircle(
                color = Color(0xFF263238),
                radius = headRadius + 0.8f * scale,
                center = Offset(px, headY)
            )
            // Visor visor ámbar
            val eyeX = px + 4f * scale * dir
            drawRect(
                color = Color(0xFFFFB300),
                topLeft = Offset(eyeX - 3.5f * scale, headY - 2f * scale),
                size = Size(7f * scale, 4f * scale)
            )
            // Tactical flashlight
            val torchX = px + 6f * scale * dir
            val torchY = headY - 4f * scale
            drawRect(
                color = Color(0xFFECEFF1),
                topLeft = Offset(torchX - 2f * scale, torchY - 1.5f * scale),
                size = Size(4f * scale, 3f * scale)
            )
            // Light cone
            drawCircle(
                color = Color(0x66FFFFFF),
                radius = 5f * scale,
                center = Offset(torchX + 6f * scale * dir, torchY)
            )
        }
        "HEAD_CYBER" -> {
            // Aerodynamic Cyber helm + Neon HUD visor strip + Antenna
            drawCircle(
                color = Color(0xFF0D1B2A),
                radius = headRadius,
                center = Offset(px, headY)
            )
            // Magenta / Cyan HUD Visor
            val eyeX = px + 3f * scale * dir
            drawRect(
                color = Color(0xFFE040FB),
                topLeft = Offset(eyeX - 4f * scale, headY - 2f * scale),
                size = Size(8f * scale, 3.5f * scale)
            )
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(eyeX - 4f * scale, headY),
                end = Offset(eyeX + 4f * scale, headY),
                strokeWidth = 1.5f * scale
            )
            // Telemetry antenna
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(px - 3f * scale * dir, headY - 5f * scale),
                end = Offset(px - 5f * scale * dir, headY - 12f * scale),
                strokeWidth = 2f * scale
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 1.8f * scale,
                center = Offset(px - 5f * scale * dir, headY - 12f * scale)
            )
        }
        "HEAD_SKULL" -> {
            // Steel death mask + forged horn spikes + eye fire
            drawCircle(
                color = Color(0xFF212121),
                radius = headRadius,
                center = Offset(px, headY)
            )
            // Skull plate forehead
            drawRect(
                color = Color(0xFFECEFF1),
                topLeft = Offset(px - 4f * scale, headY - 4f * scale),
                size = Size(8f * scale, 4f * scale)
            )
            // Demonic horn
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(px - 2f * scale, headY - 6f * scale),
                end = Offset(px - 5f * scale, headY - 12f * scale),
                strokeWidth = 2.5f * scale
            )
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(px + 2f * scale, headY - 6f * scale),
                end = Offset(px + 5f * scale, headY - 12f * scale),
                strokeWidth = 2.5f * scale
            )
            // Glowing crimson eye flames
            val eyeX = px + 4f * scale * dir
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 2.5f * scale,
                center = Offset(eyeX, headY - 1f * scale)
            )
            drawCircle(
                color = Color(0xFFFFEB3B),
                radius = 1.2f * scale,
                center = Offset(eyeX, headY - 1f * scale)
            )
        }
        "HEAD_TITAN" -> {
            // Titan battle helm + Golden spiked crown + Solar flame visor
            drawCircle(
                color = Color(0xFF263238),
                radius = headRadius + 1f * scale,
                center = Offset(px, headY)
            )
            // Golden Spiked Crown
            listOf(-4f, 0f, 4f).forEach { cx ->
                drawLine(
                    color = Color(0xFFFFD700),
                    start = Offset(px + cx * scale, headY - 6f * scale),
                    end = Offset(px + cx * scale, headY - 12f * scale),
                    strokeWidth = 2.4f * scale
                )
            }
            // Incandescent Solar flare visor
            val eyeX = px + 4f * scale * dir
            drawRect(
                color = Color(0xFFFF5722),
                topLeft = Offset(eyeX - 4f * scale, headY - 2.5f * scale),
                size = Size(8f * scale, 4.5f * scale)
            )
            drawCircle(
                color = Color(0xFFFFEB3B),
                radius = 2.2f * scale,
                center = Offset(eyeX, headY - 0.5f * scale)
            )
        }
        else -> {
            // HEAD_DEFAULT: Gas mask & Cyan lens
            drawCircle(
                color = Color(0xFF37474F),
                radius = headRadius,
                center = Offset(px, headY)
            )
            // Mask snout filter
            drawRect(
                color = Color(0xFF263238),
                topLeft = Offset(px + 4f * scale * dir - 2f * scale, headY + 1f * scale),
                size = Size(5f * scale, 4f * scale)
            )
            // Glowing goggles
            val eyeX = px + 4f * scale * dir
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 2.2f * scale,
                center = Offset(eyeX, headY - 1f * scale)
            )
        }
    }

    // 5. Weapon held
    if (showWeapon) {
        val armY = bodyY + 8f * scale
        val gunEndX = px + 16f * scale * dir
        drawLine(
            color = Color(0xFFB0BEC5),
            start = Offset(px, armY),
            end = Offset(gunEndX, armY),
            strokeWidth = 3f * scale
        )
    }
}

private fun DrawScope.drawEnemy(
    enemy: Enemy,
    viewLeft: Float,
    scale: Float
) {
    val ex = (enemy.x - viewLeft) * scale
    val ey = enemy.y * scale
    val dir = if (enemy.facingRight) 1f else -1f
    val isHurt = enemy.state == EnemyState.HURT

    when {
        enemy.isBoss -> {
            // ==========================================
            // TITÁN DEL PÁRAMO - JEFE COLOSAL (Scale 2.4x)
            // ==========================================
            val bossScale = 2.4f * scale

            // 1. Heavy shadow
            drawOval(
                color = Color(0x99000000),
                topLeft = Offset(ex - 34f * scale, ey - 6f * scale),
                size = Size(68f * scale, 12f * scale)
            )

            // 2. Heavy Hydraulic Legs
            val legStride = (sin(enemy.animFrame.toDouble()) * 5f).toFloat() * scale
            val legColor = if (isHurt) Color(0xFFFFFFFF) else Color(0xFF1A2226)
            val jointColor = Color(0xFF37474F)

            // Back leg
            drawRect(
                color = legColor,
                topLeft = Offset(ex - 18f * scale * dir - 6f * scale, ey - 28f * scale),
                size = Size(11f * scale, 28f * scale)
            )
            drawCircle(
                color = jointColor,
                radius = 7f * scale,
                center = Offset(ex - 18f * scale * dir, ey - 14f * scale)
            )

            // Front leg with animated stride
            drawRect(
                color = legColor,
                topLeft = Offset(ex + 8f * scale * dir - 6f * scale, ey - 28f * scale + legStride),
                size = Size(12f * scale, 28f * scale - legStride)
            )
            drawCircle(
                color = jointColor,
                radius = 8f * scale,
                center = Offset(ex + 8f * scale * dir, ey - 14f * scale + legStride * 0.5f)
            )

            // Armored foot pads with ground spikes
            drawRect(
                color = Color(0xFF0D1215),
                topLeft = Offset(ex - 24f * scale * dir, ey - 6f * scale),
                size = Size(16f * scale, 6f * scale)
            )
            drawRect(
                color = Color(0xFF0D1215),
                topLeft = Offset(ex + 2f * scale * dir, ey - 6f * scale),
                size = Size(18f * scale, 6f * scale)
            )

            // 3. Colossal Armored Torso & Chassis
            val torsoY = ey - 64f * scale
            val bodyColor = if (isHurt) Color(0xFFFFFFFF) else Color(0xFF263238)
            drawRect(
                color = bodyColor,
                topLeft = Offset(ex - 22f * scale, torsoY),
                size = Size(44f * scale, 38f * scale)
            )
            // Heavy armor plating & rivet lines
            drawRect(
                color = Color(0xFF192227),
                topLeft = Offset(ex - 24f * scale, torsoY + 4f * scale),
                size = Size(48f * scale, 12f * scale)
            )
            for (r in -2..2) {
                drawCircle(
                    color = Color(0xFF90A4AE),
                    radius = 1.8f * scale,
                    center = Offset(ex + r * 9f * scale, torsoY + 10f * scale)
                )
            }

            // 4. Glowing Reactor Core in Chest
            val pulse = (sin(System.currentTimeMillis() * 0.008) * 0.25 + 0.75).toFloat()
            val coreY = torsoY + 22f * scale
            // Outer heat aura
            drawCircle(
                color = Color(0x66FF1744),
                radius = 16f * scale * pulse,
                center = Offset(ex, coreY)
            )
            // Core flame
            drawCircle(
                color = Color(0xFFFF5722),
                radius = 11f * scale,
                center = Offset(ex, coreY)
            )
            drawCircle(
                color = Color(0xFFFFEB3B),
                radius = 6f * scale,
                center = Offset(ex, coreY)
            )
            // Iron grill over reactor
            drawLine(
                color = Color(0xFF101416),
                start = Offset(ex - 10f * scale, coreY),
                end = Offset(ex + 10f * scale, coreY),
                strokeWidth = 2.5f * scale
            )
            drawLine(
                color = Color(0xFF101416),
                start = Offset(ex, coreY - 10f * scale),
                end = Offset(ex, coreY + 10f * scale),
                strokeWidth = 2.5f * scale
            )

            // 5. Twin Industrial Smokestacks on Back
            val stackX = ex - 16f * scale * dir
            drawRect(
                color = Color(0xFF161E22),
                topLeft = Offset(stackX - 5f * scale, torsoY - 16f * scale),
                size = Size(10f * scale, 18f * scale)
            )
            // Smoke puffs from stacks
            drawCircle(
                color = Color(0x44263238),
                radius = 8f * scale,
                center = Offset(stackX, torsoY - 22f * scale)
            )
            drawCircle(
                color = Color(0x3337474F),
                radius = 12f * scale,
                center = Offset(stackX - 6f * scale * dir, torsoY - 30f * scale)
            )

            // 6. Dorsal Heavy Bio-Plasma Cannon (Mounted on forward shoulder)
            val cannonX = ex + 14f * scale * dir
            val cannonY = torsoY - 4f * scale
            // Turret mount
            drawCircle(
                color = Color(0xFF1B242A),
                radius = 10f * scale,
                center = Offset(cannonX, cannonY)
            )
            // Cannon Barrel
            val barrelLen = 26f * scale
            val barrelEndX = cannonX + barrelLen * dir
            drawLine(
                color = Color(0xFF37474F),
                start = Offset(cannonX, cannonY),
                end = Offset(barrelEndX, cannonY),
                strokeWidth = 9f * scale
            )
            drawLine(
                color = Color(0xFF263238),
                start = Offset(cannonX, cannonY - 2f * scale),
                end = Offset(barrelEndX, cannonY - 2f * scale),
                strokeWidth = 3f * scale
            )
            // Plasma coil conduits on cannon
            drawLine(
                color = Color(0xFFFF1744),
                start = Offset(cannonX + 4f * scale * dir, cannonY - 5f * scale),
                end = Offset(cannonX + 18f * scale * dir, cannonY - 5f * scale),
                strokeWidth = 2.5f * scale
            )

            // Weapon charging animation when ready to shoot!
            if (enemy.shootCooldown in 0.01f..0.55f) {
                val chargeRatio = 1.0f - (enemy.shootCooldown / 0.55f)
                drawCircle(
                    color = Color(0xFFFF1744).copy(alpha = 0.7f),
                    radius = (14f * chargeRatio) * scale,
                    center = Offset(barrelEndX, cannonY)
                )
                drawCircle(
                    color = Color(0xFFFFFF00).copy(alpha = 0.9f),
                    radius = (7f * chargeRatio) * scale,
                    center = Offset(barrelEndX, cannonY)
                )
            }

            // 7. Colossal Demonic Skull & Horns
            val headX = ex + 4f * scale * dir
            val headY = torsoY - 14f * scale
            drawCircle(
                color = if (isHurt) Color(0xFFFFFFFF) else Color(0xFF1F282D),
                radius = 14f * scale,
                center = Offset(headX, headY)
            )
            // Massive horns curving upward
            val horn1 = Path().apply {
                moveTo(headX, headY - 10f * scale)
                lineTo(headX - 18f * scale * dir, headY - 28f * scale)
                lineTo(headX - 6f * scale * dir, headY - 14f * scale)
                close()
            }
            drawPath(horn1, color = Color(0xFFCFD8DC))

            val horn2 = Path().apply {
                moveTo(headX + 4f * scale * dir, headY - 10f * scale)
                lineTo(headX + 16f * scale * dir, headY - 26f * scale)
                lineTo(headX + 9f * scale * dir, headY - 12f * scale)
                close()
            }
            drawPath(horn2, color = Color(0xFFECEFF1))

            // Burning red visor / eye-slit
            val eyeX = headX + 7f * scale * dir
            drawCircle(
                color = Color(0x66FF1744),
                radius = 6f * scale,
                center = Offset(eyeX, headY - 1f * scale)
            )
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 3.5f * scale,
                center = Offset(eyeX, headY - 1f * scale)
            )
            drawCircle(
                color = Color(0xFFFFFFFF),
                radius = 1.5f * scale,
                center = Offset(eyeX, headY - 1f * scale)
            )

            // 8. Crushing Claw Arm (Front)
            val armStartX = ex + 16f * scale * dir
            val armStartY = torsoY + 8f * scale
            val armEndX = armStartX + 18f * scale * dir
            val armEndY = armStartY + 16f * scale
            drawLine(
                color = Color(0xFF37474F),
                start = Offset(armStartX, armStartY),
                end = Offset(armEndX, armEndY),
                strokeWidth = 7f * scale
            )
            // Spiked claw fingers
            drawCircle(
                color = Color(0xFF101619),
                radius = 6f * scale,
                center = Offset(armEndX, armEndY)
            )
            drawLine(
                color = Color(0xFFFF5722),
                start = Offset(armEndX, armEndY),
                end = Offset(armEndX + 10f * scale * dir, armEndY + 8f * scale),
                strokeWidth = 4f * scale
            )
            drawLine(
                color = Color(0xFFFF5722),
                start = Offset(armEndX, armEndY),
                end = Offset(armEndX + 8f * scale * dir, armEndY - 6f * scale),
                strokeWidth = 3f * scale
            )

            // 9. Floating Boss Name & Skull Icon above Boss
            val bossTagY = headY - 34f * scale
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 7f * scale,
                center = Offset(ex, bossTagY)
            )
            drawCircle(
                color = Color(0xFFFFD700),
                radius = 4.5f * scale,
                center = Offset(ex, bossTagY)
            )
        }

        enemy.type == EnemyType.BRUTO -> {
            // ==========================================
            // BRUTO CARMESÍ - BEHEMOTH ACORAZADO (Scale 1.6x)
            // ==========================================
            // Shadow
            drawOval(
                color = Color(0x77000000),
                topLeft = Offset(ex - 22f * scale, ey - 4f * scale),
                size = Size(44f * scale, 8f * scale)
            )

            val stride = (sin(enemy.animFrame.toDouble()) * 3.5f).toFloat() * scale
            val brutoColor = if (isHurt) Color(0xFFFFFFFF) else Color(0xFF4E1620)
            val armorColor = Color(0xFF263238)

            // Massive Stomping Legs
            drawRect(
                color = brutoColor,
                topLeft = Offset(ex - 12f * scale, ey - 18f * scale),
                size = Size(9f * scale, 18f * scale)
            )
            drawRect(
                color = brutoColor,
                topLeft = Offset(ex + 3f * scale, ey - 18f * scale + stride),
                size = Size(9f * scale, 18f * scale - stride)
            )
            // Steel knee plates
            drawRect(
                color = armorColor,
                topLeft = Offset(ex - 13f * scale, ey - 12f * scale),
                size = Size(11f * scale, 6f * scale)
            )
            drawRect(
                color = armorColor,
                topLeft = Offset(ex + 2f * scale, ey - 12f * scale + stride * 0.5f),
                size = Size(11f * scale, 6f * scale)
            )

            // Massive Torso with Molten Heart
            val torsoY = ey - 42f * scale
            drawRect(
                color = brutoColor,
                topLeft = Offset(ex - 15f * scale, torsoY),
                size = Size(30f * scale, 26f * scale)
            )
            // Scrap metal chest plate
            drawRect(
                color = armorColor,
                topLeft = Offset(ex - 16f * scale, torsoY + 2f * scale),
                size = Size(32f * scale, 12f * scale)
            )
            // Molten vent
            drawCircle(
                color = Color(0xFFFF5722),
                radius = 5.5f * scale,
                center = Offset(ex, torsoY + 16f * scale)
            )
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 2.5f * scale,
                center = Offset(ex, torsoY + 16f * scale)
            )

            // Spiked Shoulders & Arms
            val armY = torsoY + 4f * scale
            val fistX = ex + 18f * scale * dir
            drawLine(
                color = brutoColor,
                start = Offset(ex + 8f * scale * dir, armY),
                end = Offset(fistX, armY + 14f * scale),
                strokeWidth = 7f * scale
            )
            // Spiked knuckle
            drawCircle(
                color = Color(0xFFB0BEC5),
                radius = 5.5f * scale,
                center = Offset(fistX, armY + 14f * scale)
            )

            // Head with curved ram horns
            val headY = torsoY - 10f * scale
            drawCircle(
                color = brutoColor,
                radius = 9.5f * scale,
                center = Offset(ex + 3f * scale * dir, headY)
            )
            // Horns
            val horn = Path().apply {
                moveTo(ex, headY - 4f * scale)
                lineTo(ex + 14f * scale * dir, headY - 16f * scale)
                lineTo(ex + 6f * scale * dir, headY - 2f * scale)
                close()
            }
            drawPath(horn, color = Color(0xFFCFD8DC))

            // Glowing red eyes
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 2.4f * scale,
                center = Offset(ex + 7f * scale * dir, headY - 1f * scale)
            )
        }

        enemy.type == EnemyType.ERRANTE -> {
            // ==========================================
            // ERRANTE - ZOMBIE MUTANTE BIOMECÁNICO
            // ==========================================
            // Shadow
            drawOval(
                color = Color(0x55000000),
                topLeft = Offset(ex - 12f * scale, ey - 3f * scale),
                size = Size(24f * scale, 6f * scale)
            )

            val stride = (sin(enemy.animFrame.toDouble()) * 3f).toFloat() * scale
            val skinColor = if (isHurt) Color(0xFFFFFFFF) else Color(0xFF455A52)
            val clothColor = Color(0xFF263238)

            // Articulated legs
            drawLine(
                color = clothColor,
                start = Offset(ex - 4f * scale, ey - 14f * scale),
                end = Offset(ex - 4f * scale, ey),
                strokeWidth = 3.5f * scale
            )
            drawLine(
                color = clothColor,
                start = Offset(ex + 4f * scale, ey - 14f * scale),
                end = Offset(ex + 4f * scale, ey + stride),
                strokeWidth = 3.5f * scale
            )
            // Shin guard on front leg
            drawRect(
                color = Color(0xFF78909C),
                topLeft = Offset(ex + 2f * scale, ey - 9f * scale + stride * 0.5f),
                size = Size(5f * scale, 6f * scale)
            )

            // Hunched Torso
            val torsoY = ey - 32f * scale
            drawRect(
                color = skinColor,
                topLeft = Offset(ex - 8f * scale, torsoY),
                size = Size(16f * scale, 20f * scale)
            )
            // Tattered shirt / belt
            drawRect(
                color = clothColor,
                topLeft = Offset(ex - 8.5f * scale, torsoY + 12f * scale),
                size = Size(17f * scale, 7f * scale)
            )

            // Bony dorsal spikes on spine
            for (i in 0..2) {
                val spikeY = torsoY + 2f * scale + i * 5f * scale
                drawLine(
                    color = Color(0xFFBCAAA4),
                    start = Offset(ex - 8f * scale * dir, spikeY),
                    end = Offset(ex - 14f * scale * dir, spikeY - 3f * scale),
                    strokeWidth = 2.2f * scale
                )
            }

            // Glowing bio-luminescent veins on chest
            drawLine(
                color = Color(0xBB76FF03),
                start = Offset(ex - 3f * scale, torsoY + 4f * scale),
                end = Offset(ex + 2f * scale, torsoY + 10f * scale),
                strokeWidth = 1.8f * scale
            )

            // Asymmetrical Cybernetic Claw Arm
            val clawArmY = torsoY + 6f * scale
            val clawEndX = ex + 15f * scale * dir
            drawLine(
                color = skinColor,
                start = Offset(ex + 5f * scale * dir, clawArmY),
                end = Offset(clawEndX, clawArmY + 5f * scale),
                strokeWidth = 3.2f * scale
            )
            // Steel blades on hand
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(clawEndX, clawArmY + 5f * scale),
                end = Offset(clawEndX + 7f * scale * dir, clawArmY + 2f * scale),
                strokeWidth = 2f * scale
            )
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(clawEndX, clawArmY + 5f * scale),
                end = Offset(clawEndX + 7f * scale * dir, clawArmY + 8f * scale),
                strokeWidth = 2f * scale
            )

            // Mutated Skull Head
            val headY = torsoY - 8f * scale
            drawCircle(
                color = skinColor,
                radius = 7f * scale,
                center = Offset(ex + 1f * scale * dir, headY)
            )
            // Exposed metallic jaw
            drawRect(
                color = Color(0xFF78909C),
                topLeft = Offset(ex - 2f * scale + 2f * scale * dir, headY + 3f * scale),
                size = Size(7f * scale, 4f * scale)
            )
            // Glowing amber feral eyes with halo
            val eyeX = ex + 4f * scale * dir
            drawCircle(
                color = Color(0x66FF9800),
                radius = 4f * scale,
                center = Offset(eyeX, headY - 1f * scale)
            )
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 1.8f * scale,
                center = Offset(eyeX, headY - 1f * scale)
            )
        }

        enemy.type == EnemyType.CORREDOR -> {
            // ==========================================
            // CORREDOR - QUIMERA CUADRÚPEDA VELOZ
            // ==========================================
            // Shadow
            drawOval(
                color = Color(0x55000000),
                topLeft = Offset(ex - 18f * scale, ey - 3f * scale),
                size = Size(36f * scale, 6f * scale)
            )

            val runAnim = (sin(enemy.animFrame.toDouble() * 1.5) * 4f).toFloat() * scale
            val beastColor = if (isHurt) Color(0xFFFFFFFF) else Color(0xFF421D24)
            val spineColor = Color(0xFF880E4F)

            // Arched predatory body
            drawOval(
                color = beastColor,
                topLeft = Offset(ex - 16f * scale, ey - 18f * scale),
                size = Size(32f * scale, 12f * scale)
            )

            // Razor dorsal quills along spine
            for (q in 0..3) {
                val qx = ex - 10f * scale * dir + q * 6f * scale * dir
                drawLine(
                    color = spineColor,
                    start = Offset(qx, ey - 17f * scale),
                    end = Offset(qx - 5f * scale * dir, ey - 24f * scale),
                    strokeWidth = 2.4f * scale
                )
            }

            // Whipping spiked tail
            val tailPath = Path().apply {
                moveTo(ex - 15f * scale * dir, ey - 14f * scale)
                quadraticTo(
                    ex - 24f * scale * dir,
                    ey - 22f * scale + runAnim,
                    ex - 28f * scale * dir,
                    ey - 10f * scale + runAnim
                )
            }
            drawPath(
                path = tailPath,
                color = Color(0xFFB71C1C),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f * scale)
            )

            // 4 Articulated Running Legs
            // Back legs
            drawLine(
                color = beastColor,
                start = Offset(ex - 10f * scale * dir, ey - 10f * scale),
                end = Offset(ex - 14f * scale * dir, ey + runAnim),
                strokeWidth = 3f * scale
            )
            // Front legs (reaching forward)
            drawLine(
                color = beastColor,
                start = Offset(ex + 8f * scale * dir, ey - 10f * scale),
                end = Offset(ex + 16f * scale * dir, ey - runAnim),
                strokeWidth = 3f * scale
            )

            // Snarling Head & Crimson predator eyes
            val headX = ex + 14f * scale * dir
            val headY = ey - 14f * scale
            drawCircle(
                color = beastColor,
                radius = 6.5f * scale,
                center = Offset(headX, headY)
            )
            // Open fanged snout
            drawRect(
                color = Color(0xFFB71C1C),
                topLeft = Offset(headX, headY - 1f * scale),
                size = Size(6f * scale * dir, 4f * scale)
            )
            // Dual glowing crimson eyes
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 2.2f * scale,
                center = Offset(headX + 2f * scale * dir, headY - 2f * scale)
            )
            drawCircle(
                color = Color(0xFFFFFFFF),
                radius = 1f * scale,
                center = Offset(headX + 2f * scale * dir, headY - 2f * scale)
            )
        }

        enemy.type == EnemyType.TREPADOR -> {
            // ==========================================
            // TREPADOR - ACECHADOR ARACNOMORFO DE TECHO
            // ==========================================
            // Shadow
            drawOval(
                color = Color(0x44000000),
                topLeft = Offset(ex - 14f * scale, ey - 2f * scale),
                size = Size(28f * scale, 5f * scale)
            )

            val spiderColor = if (isHurt) Color(0xFFFFFFFF) else Color(0xFF281335)
            val venomColor = Color(0xFFE040FB)
            val legBob = (sin(enemy.animFrame.toDouble()) * 3f).toFloat() * scale

            // Bulbous pulsating abdomen
            val abdoX = ex - 7f * scale * dir
            val abdoY = ey - 16f * scale
            drawCircle(
                color = spiderColor,
                radius = 9f * scale,
                center = Offset(abdoX, abdoY)
            )
            // Bioluminescent venom marks on abdomen
            drawCircle(
                color = venomColor,
                radius = 4f * scale,
                center = Offset(abdoX, abdoY)
            )
            drawCircle(
                color = Color(0xFF18FFFF),
                radius = 2f * scale,
                center = Offset(abdoX, abdoY)
            )

            // Cephalothorax
            val headX = ex + 5f * scale * dir
            val headY = ey - 15f * scale
            drawCircle(
                color = spiderColor,
                radius = 6.5f * scale,
                center = Offset(headX, headY)
            )

            // 6 Articulated insectoid legs (3 pairs)
            for (p in 0..2) {
                val legBaseX = ex - 2f * scale * dir + p * 4f * scale * dir
                val legKneeX = legBaseX + (if (p == 0) -10f else if (p == 1) 2f else 12f) * scale * dir
                val legKneeY = ey - 25f * scale + (if (p % 2 == 0) legBob else -legBob)
                val legFootX = legKneeX + (if (p == 0) -6f else 6f) * scale * dir
                val legFootY = ey

                // Coxa to knee
                drawLine(
                    color = spiderColor,
                    start = Offset(legBaseX, ey - 16f * scale),
                    end = Offset(legKneeX, legKneeY),
                    strokeWidth = 2.4f * scale
                )
                // Knee to foot point
                drawLine(
                    color = Color(0xFF4A148C),
                    start = Offset(legKneeX, legKneeY),
                    end = Offset(legFootX, legFootY),
                    strokeWidth = 2f * scale
                )
            }

            // Cluster of 4 glowing cyan spider eyes
            drawCircle(
                color = Color(0xFF18FFFF),
                radius = 1.6f * scale,
                center = Offset(headX + 4f * scale * dir, headY - 2f * scale)
            )
            drawCircle(
                color = Color(0xFF18FFFF),
                radius = 1.6f * scale,
                center = Offset(headX + 2f * scale * dir, headY - 4f * scale)
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 1.2f * scale,
                center = Offset(headX + 5f * scale * dir, headY + 1f * scale)
            )
        }
    }

    // High-contrast polished Health Bar above enemy
    if (enemy.health < enemy.maxHealth && enemy.state != EnemyState.DYING) {
        val barWidth = (if (enemy.isBoss) 42f else (if (enemy.type == EnemyType.BRUTO) 30f else 22f)) * scale
        val barHeight = (if (enemy.isBoss) 5f else 3.5f) * scale
        val barX = ex - barWidth * 0.5f
        val barY = (if (enemy.isBoss) ey - 78f * scale else (if (enemy.type == EnemyType.BRUTO) ey - 52f * scale else ey - 42f * scale))

        // Background with rounded border
        drawRect(color = Color(0xDD000000), topLeft = Offset(barX - 1f * scale, barY - 1f * scale), size = Size(barWidth + 2f * scale, barHeight + 2f * scale))
        val hpRatio = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
        val hpColor = when {
            hpRatio > 0.55f -> Color(0xFF00E676)
            hpRatio > 0.25f -> Color(0xFFFFD54F)
            else -> Color(0xFFFF1744)
        }
        drawRect(color = hpColor, topLeft = Offset(barX, barY), size = Size(barWidth * hpRatio, barHeight))
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

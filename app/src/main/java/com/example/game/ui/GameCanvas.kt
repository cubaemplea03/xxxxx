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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import com.example.game.engine.GameEngine
import com.example.game.model.Enemy
import com.example.game.model.EnemyState
import com.example.game.model.EnemyType
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

    Canvas(modifier = modifier.fillMaxSize()) {
        // Read renderTickProvider inside draw scope to trigger hardware redraw on every frame
        val _tick = renderTickProvider()
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Logical game height is ~320 units. Scale to fit screen height.
        val scale = canvasHeight / 320f
        val camX = engine.cameraX
        val viewLeft = camX - (canvasWidth / scale) * 0.5f

        // Draw parallax sky and background
        drawBackground(engine, canvasWidth, canvasHeight, viewLeft, scale)

        // Draw Train Tracks and Ground
        drawTracksAndGround(engine, canvasWidth, canvasHeight, viewLeft, scale)

        // Draw Wagons
        for (w in engine.wagons) {
            drawWagon(w, viewLeft, scale)
        }

        // Draw Resources
        for (res in engine.resources) {
            drawResourceItem(res, viewLeft, scale)
        }

        // Draw Enemies
        for (enemy in engine.enemies) {
            drawEnemy(enemy, viewLeft, scale)
        }

        // Draw Player
        drawPlayer(engine.player, viewLeft, scale)

        // Draw Projectiles
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

        // Draw Slash Effects
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

        // Draw Particles
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

        // Draw Floating texts
        drawContext.canvas.nativeCanvas.let { nativeCanvas ->
            for (ft in engine.floatingTexts) {
                val sx = (ft.x - viewLeft) * scale
                val sy = ft.y * scale
                textPaint.color = ft.color.toInt()
                textPaint.alpha = (ft.life * 255).coerceIn(0f, 255f).toInt()
                nativeCanvas.drawText(ft.text, sx, sy, textPaint)
            }
        }

        // Draw Atmospheric Vignette & Foreground Fog / Rain
        drawAtmosphericOverlay(engine, canvasWidth, canvasHeight)
    }
}

private fun DrawScope.drawBackground(
    engine: GameEngine,
    width: Float,
    height: Float,
    viewLeft: Float,
    scale: Float
) {
    // 1. Sky gradient (Dark post-apocalyptic midnight blues/cyans)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0A0E14), Color(0xFF141C24), Color(0xFF1E2833)),
            startY = 0f,
            endY = height * 0.85f
        ),
        size = Size(width, height)
    )

    // Distant moon behind clouds
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

    // 2. Parallax Mountain / Skyline Silhouette (Layer 1)
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

    // 3. Parallax Pine Forest & Industrial Silhouettes (Layer 2)
    val p2Offset = -(viewLeft * 0.35f * scale) % (width * 0.5f)
    for (copy in -1..3) {
        val ox = p2Offset + copy * (width * 0.5f)
        // Tall pines & factory smokestacks
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

private fun DrawScope.drawTracksAndGround(
    engine: GameEngine,
    width: Float,
    height: Float,
    viewLeft: Float,
    scale: Float
) {
    val groundY = 224f * scale
    // Ground ballast (dark gravel)
    drawRect(
        color = Color(0xFF1A1D20),
        topLeft = Offset(0f, groundY),
        size = Size(width, height - groundY)
    )

    // Moving Railroad Ties (sleepers)
    val tieSpacing = 28f * scale
    val tieOffset = -(viewLeft * scale) % tieSpacing
    var tx = tieOffset - tieSpacing
    while (tx < width + tieSpacing) {
        drawRect(
            color = Color(0xFF2C2520),
            topLeft = Offset(tx, groundY + 4f * scale),
            size = Size(10f * scale, 12f * scale)
        )
        tx += tieSpacing
    }

    // Steel Rails (Two parallel steel bars with highlight)
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

    // Foreground power poles passing by (Layer 3)
    val poleSpacing = 420f * scale
    val poleOffset = -(viewLeft * 0.7f * scale) % poleSpacing
    var px = poleOffset - poleSpacing
    while (px < width + poleSpacing) {
        // Telegraph pole
        drawRect(
            color = Color(0x99212121),
            topLeft = Offset(px, groundY - 140f * scale),
            size = Size(6f * scale, 140f * scale)
        )
        // Crossbeam
        drawRect(
            color = Color(0x99212121),
            topLeft = Offset(px - 16f * scale, groundY - 130f * scale),
            size = Size(38f * scale, 4f * scale)
        )
        px += poleSpacing
    }
}

private fun DrawScope.drawWagon(
    w: Wagon,
    viewLeft: Float,
    scale: Float
) {
    val wx = (w.x - viewLeft) * scale
    val wy = w.roofY * scale
    val wWidth = w.width * scale
    val wHeight = (w.floorY - w.roofY) * scale
    val floorScreenY = w.floorY * scale

    when (w.index) {
        4 -> {
            // LOCOMOTIVE (Heavy armored steam engine)
            // Main body
            drawRect(
                color = Color(0xFF263238),
                topLeft = Offset(wx, wy + 15f * scale),
                size = Size(wWidth, wHeight - 15f * scale)
            )
            // Driver cabin
            drawRect(
                color = Color(0xFF1E272C),
                topLeft = Offset(wx + 10f * scale, wy),
                size = Size(95f * scale, wHeight)
            )
            // Cabin window with warm yellow glow inside
            drawRect(
                color = Color(0xFFFFD54F),
                topLeft = Offset(wx + 25f * scale, wy + 10f * scale),
                size = Size(25f * scale, 22f * scale)
            )
            // Furnace door glow (interaction spot)
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

            // Smokestack chimney
            drawRect(
                color = Color(0xFF1A2226),
                topLeft = Offset(wx + 280f * scale, wy - 10f * scale),
                size = Size(20f * scale, 30f * scale)
            )
            // Cowcatcher in front
            val cowcatcher = Path().apply {
                moveTo(wx + wWidth, floorScreenY)
                lineTo(wx + wWidth + 24f * scale, floorScreenY)
                lineTo(wx + wWidth, floorScreenY - 24f * scale)
                close()
            }
            drawPath(cowcatcher, color = Color(0xFF37474F))

            // Searchlight Headlight (Glowing conical beam cutting forward!)
            val lightX = wx + wWidth + 10f * scale
            val lightY = wy + 25f * scale
            drawCircle(
                color = Color(0xFFFFF9C4),
                radius = 7f * scale,
                center = Offset(lightX, lightY)
            )
            // Beam cone
            val beam = Path().apply {
                moveTo(lightX, lightY)
                lineTo(lightX + 380f * scale, lightY - 70f * scale)
                lineTo(lightX + 380f * scale, lightY + 120f * scale)
                close()
            }
            drawPath(
                path = beam,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x77FFF59D), Color(0x11FFF59D), Color.Transparent),
                    startX = lightX,
                    endX = lightX + 380f * scale
                )
            )
        }
        3 -> {
            // TENDER (Coal & Water Car)
            drawRect(
                color = Color(0xFF37474F),
                topLeft = Offset(wx, wy),
                size = Size(wWidth, wHeight)
            )
            // Coal mound
            val coal = Path().apply {
                moveTo(wx + 10f * scale, wy)
                lineTo(wx + wWidth * 0.45f, wy - 18f * scale)
                lineTo(wx + wWidth - 10f * scale, wy)
                close()
            }
            drawPath(coal, color = Color(0xFF121416))
        }
        2 -> {
            // ARMORED BOXCAR (With roof catwalk, ladder, metal rivets)
            drawRect(
                color = Color(0xFF2E3B43),
                topLeft = Offset(wx, wy),
                size = Size(wWidth, wHeight)
            )
            // Catwalk railing on roof
            drawRect(
                color = Color(0xFF546E7A),
                topLeft = Offset(wx + 5f * scale, wy - 4f * scale),
                size = Size(wWidth - 10f * scale, 3f * scale)
            )
            // Metal plating seams
            for (i in 1..3) {
                drawLine(
                    color = Color(0xFF1E282D),
                    start = Offset(wx + (wWidth / 4) * i, wy),
                    end = Offset(wx + (wWidth / 4) * i, floorScreenY),
                    strokeWidth = 2f * scale
                )
            }
            // Ladder on side
            val ladderX = wx + 18f * scale
            for (r in 0..5) {
                drawLine(
                    color = Color(0xFF78909C),
                    start = Offset(ladderX, wy + r * 11f * scale),
                    end = Offset(ladderX + 14f * scale, wy + r * 11f * scale),
                    strokeWidth = 2.5f * scale
                )
            }
        }
        1 -> {
            // FLATCAR (With cargo containers/crates)
            drawRect(
                color = Color(0xFF263238),
                topLeft = Offset(wx, wy),
                size = Size(wWidth, 14f * scale)
            )
            // Cargo crate obstacle
            drawRect(
                color = Color(0xFF5D4037),
                topLeft = Offset(wx + 35f * scale, wy - 35f * scale),
                size = Size(40f * scale, 35f * scale)
            )
            drawRect(
                color = Color(0xFF4E342E),
                topLeft = Offset(wx + 85f * scale, wy - 25f * scale),
                size = Size(35f * scale, 25f * scale)
            )
        }
        0 -> {
            // CABOOSE (Rear defense platform & red lantern)
            drawRect(
                color = Color(0xFF455A64),
                topLeft = Offset(wx + 20f * scale, wy),
                size = Size(wWidth - 20f * scale, wHeight)
            )
            // Rear open platform
            drawRect(
                color = Color(0xFF263238),
                topLeft = Offset(wx, floorScreenY - 8f * scale),
                size = Size(20f * scale, 8f * scale)
            )
            // Red tail lantern
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

    // Scavenge Crate on Wagon (if present and not opened)
    if (w.hasCrate) {
        val cx = wx + wWidth * 0.5f - 14f * scale
        val cy = (if (w.hasRoof) w.roofY else w.floorY) * scale - 24f * scale
        if (!w.crateOpened) {
            // Glowing scavenge chest/crate
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
            // Subtle pulse aura
            drawCircle(
                color = Color(0x33FFCA28),
                radius = 20f * scale,
                center = Offset(cx + 14f * scale, cy + 12f * scale)
            )
        } else {
            // Opened crate
            drawRect(
                color = Color(0xFF4E342E),
                topLeft = Offset(cx, cy + 6f * scale),
                size = Size(28f * scale, 18f * scale)
            )
        }
    }

    // Iron Wheels for each car (2 bogies, 4 wheels)
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

    // Coupler between wagons
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

    // Damage invulnerability blinking
    if (player.invulnerableTimer > 0f && (player.invulnerableTimer * 20).toInt() % 2 == 0) {
        return
    }

    val dir = if (player.facingRight) 1f else -1f
    val isCrouch = player.isCrouching
    val bodyHeight = if (isCrouch) 26f * scale else 42f * scale
    val topY = py - bodyHeight

    // Survivor Head & Cowl / Mask
    val headRadius = 7f * scale
    val headCenter = Offset(px, topY + headRadius)
    drawCircle(
        color = Color(0xFFCFD8DC),
        radius = headRadius,
        center = headCenter
    )
    // Goggles / Survivor mask
    drawRect(
        color = Color(0xFFFFB300),
        topLeft = Offset(px + (if (player.facingRight) 1f else -7f) * scale, topY + 4f * scale),
        size = Size(6f * scale, 4f * scale)
    )

    // Body (Trench coat / Leather jacket)
    val coatColor = Color(0xFF5D4037)
    val torsoTop = topY + headRadius * 2f
    val torsoHeight = bodyHeight - headRadius * 2f - 10f * scale
    drawRect(
        color = coatColor,
        topLeft = Offset(px - 7f * scale, torsoTop),
        size = Size(14f * scale, torsoHeight)
    )

    // Scarf / Cape flapping behind
    val scarfPath = Path().apply {
        moveTo(px, torsoTop + 2f * scale)
        lineTo(px - 14f * scale * dir, torsoTop + 6f * scale)
        lineTo(px - 18f * scale * dir, torsoTop + 14f * scale)
        lineTo(px - 4f * scale * dir, torsoTop + 8f * scale)
        close()
    }
    drawPath(scarfPath, color = Color(0xFFC62828))

    // Legs / Boots with walk animation
    val legOffset = sin(player.animFrame).toFloat() * 6f * scale
    val bootY = py - 6f * scale
    drawLine(
        color = Color(0xFF263238),
        start = Offset(px - 4f * scale, torsoTop + torsoHeight),
        end = Offset(px - 4f * scale - legOffset, bootY),
        strokeWidth = 4.5f * scale
    )
    drawLine(
        color = Color(0xFF263238),
        start = Offset(px + 4f * scale, torsoTop + torsoHeight),
        end = Offset(px + 4f * scale + legOffset, bootY),
        strokeWidth = 4.5f * scale
    )

    // Weapon in hand (Wrench / Machete / Gun)
    val handX = px + 8f * scale * dir
    val handY = torsoTop + 8f * scale
    if (player.isAttacking) {
        // Weapon swung forward
        drawLine(
            color = Color(0xFFECEFF1),
            start = Offset(handX, handY),
            end = Offset(handX + 18f * scale * dir, handY - 8f * scale),
            strokeWidth = 3.5f * scale
        )
    } else {
        // Weapon at ready
        drawLine(
            color = Color(0xFF90A4AE),
            start = Offset(handX, handY),
            end = Offset(handX + 8f * scale * dir, handY + 12f * scale),
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

    val isHurt = enemy.state == EnemyState.HURT
    val baseColor = if (isHurt) Color(0xFFFF5252) else when (enemy.type) {
        EnemyType.ERRANTE -> Color(0xFF546E7A) // Shambling ghoul
        EnemyType.CORREDOR -> Color(0xFFBF360C) // Fast feral stalker
        EnemyType.TREPADOR -> Color(0xFF4A148C) // Spidery climber
    }

    val dir = if (enemy.facingRight) 1f else -1f

    when (enemy.type) {
        EnemyType.ERRANTE -> {
            // Tall hunched walker
            val headCenter = Offset(ex + 4f * scale * dir, ey - 30f * scale)
            drawCircle(color = baseColor, radius = 6f * scale, center = headCenter)
            // Glowing pale eyes
            drawCircle(
                color = Color(0xFFFFFF72),
                radius = 1.8f * scale,
                center = Offset(ex + (if (enemy.facingRight) 6f else 2f) * scale, ey - 30f * scale)
            )
            // Ragged body
            drawRect(
                color = baseColor,
                topLeft = Offset(ex - 6f * scale, ey - 24f * scale),
                size = Size(12f * scale, 18f * scale)
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
            // Head with red glowing eye
            val headX = ex + 12f * scale * dir
            drawCircle(color = baseColor, radius = 5f * scale, center = Offset(headX, ey - 14f * scale))
            drawCircle(color = Color(0xFFFF1744), radius = 1.8f * scale, center = Offset(headX, ey - 14f * scale))
            // Spines
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
            // 4 Spiky legs
            drawLine(color = baseColor, start = Offset(ex, ey - 16f * scale), end = Offset(ex - 12f * scale, ey - 26f * scale), strokeWidth = 2f * scale)
            drawLine(color = baseColor, start = Offset(ex, ey - 16f * scale), end = Offset(ex + 12f * scale, ey - 26f * scale), strokeWidth = 2f * scale)
            drawLine(color = baseColor, start = Offset(ex, ey - 16f * scale), end = Offset(ex - 14f * scale, ey), strokeWidth = 2f * scale)
            drawLine(color = baseColor, start = Offset(ex, ey - 16f * scale), end = Offset(ex + 14f * scale, ey), strokeWidth = 2f * scale)
            // Purple eye cluster
            drawCircle(color = Color(0xFFE040FB), radius = 2.5f * scale, center = Offset(ex + 3f * scale * dir, ey - 16f * scale))
        }
    }

    // Health bar above enemy if damaged
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

    val color = when (res.type) {
        ResourceType.CHATARRA -> Color(0xFFFFCA28) // Gold scrap
        ResourceType.MUNICION -> Color(0xFF42A5F5) // Blue ammo
        ResourceType.COMIDA -> Color(0xFF66BB6A) // Green ration
        ResourceType.MEDICINA -> Color(0xFFEF5350) // Red medkit
        ResourceType.COMBUSTIBLE -> Color(0xFFFF7043) // Orange fuel
    }

    // Glowing aura
    drawCircle(
        color = color.copy(alpha = 0.35f),
        radius = 12f * scale,
        center = Offset(rx, ry)
    )
    // Core item icon
    drawCircle(
        color = color,
        radius = 5.5f * scale,
        center = Offset(rx, ry)
    )
}

private fun DrawScope.drawAtmosphericOverlay(
    engine: GameEngine,
    width: Float,
    height: Float
) {
    // Cinematic Vignette (darkened corners)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x33000000), Color(0x9905080C)),
            center = Offset(width * 0.5f, height * 0.5f),
            radius = width * 0.65f
        ),
        size = Size(width, height)
    )

    // Moving Mist / Fog bands across screen
    val mistOffset = (System.currentTimeMillis() * 0.04f) % width
    for (i in 0..1) {
        val mx = (mistOffset + i * width) % (width * 2) - width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0x18B0BEC5), Color.Transparent)
            ),
            topLeft = Offset(mx, height * 0.45f),
            size = Size(width, height * 0.35f)
        )
    }
}

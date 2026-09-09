package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.GameStatsEntity
import com.example.game.model.BESTIARY_ENTRIES
import com.example.game.model.EnemyType
import com.example.game.model.MonsterGuideEntry
import kotlin.math.sin

@Composable
fun MonsterGuideScreen(
    stats: GameStatsEntity,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = remember { BESTIARY_ENTRIES }
    var selectedType by remember { mutableStateOf(EnemyType.ERRANTE) }

    val discoveredCount = entries.count { stats.hasSeenMonster(it.type.name) }
    val progressPercent = (discoveredCount.toFloat() / entries.size.toFloat()).coerceIn(0f, 1f)

    val selectedEntry = entries.find { it.type == selectedType } ?: entries.first()
    val isSelectedSeen = stats.hasSeenMonster(selectedEntry.type.name)

    val infiniteTransition = rememberInfiniteTransition(label = "guide_anim")
    val animFrame by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_frame"
    )
    val shadowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadow_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E13))
    ) {
        // Atmospheric background grid with dark smoke
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Dark vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF141F2B), Color(0xFF080C10)),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.8f
                ),
                size = Size(w, h)
            )
            // Tech grid lines
            val step = 40.dp.toPx()
            var x = 0f
            while (x < w) {
                drawLine(
                    color = Color(0x0C00E5FF),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = 0f
            while (y < h) {
                drawLine(
                    color = Color(0x0C00E5FF),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Header Bar
            GuideHeader(
                discoveredCount = discoveredCount,
                totalCount = entries.size,
                progressPercent = progressPercent,
                onBackClick = onBackClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Two-Panel Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Column: Monster List / Selector
                MonsterListPanel(
                    entries = entries,
                    selectedType = selectedType,
                    stats = stats,
                    onSelectType = { selectedType = it },
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                )

                // Right Column: Monster Inspector
                MonsterDetailPanel(
                    entry = selectedEntry,
                    isSeen = isSelectedSeen,
                    animFrame = animFrame,
                    shadowPulse = shadowPulse,
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun GuideHeader(
    discoveredCount: Int,
    totalCount: Int,
    progressPercent: Float,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE6141D26))
            .border(1.2.dp, Color(0xFF37474F), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        OutlinedButton(
            onClick = onBackClick,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xAA1E2933),
                contentColor = Color(0xFFFFD54F)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .height(38.dp)
                .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                .testTag("btn_guide_back")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "VOLVER",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title & Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF3E2723))
                .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Libro Guía",
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "GUÍA DE SUPERVIVENCIA: BESTIARIO",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Registro de especímenes infectados, mutantes y colosos de las vías",
                fontSize = 10.sp,
                color = Color(0xFF90A4AE)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Progress counter pill
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "REGISTRO: ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB0BEC5)
                )
                Text(
                    text = "$discoveredCount / $totalCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (discoveredCount == totalCount) Color(0xFF00E676) else Color(0xFFFFD54F)
                )
                Text(
                    text = " (${(progressPercent * 100).toInt()}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90A4AE)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .width(130.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (discoveredCount == totalCount) Color(0xFF00E676) else Color(0xFFFFB300),
                trackColor = Color(0xFF263238)
            )
        }
    }
}

@Composable
private fun MonsterListPanel(
    entries: List<MonsterGuideEntry>,
    selectedType: EnemyType,
    stats: GameStatsEntity,
    onSelectType: (EnemyType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEE121922))
            .border(1.2.dp, Color(0xFF2C3946), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "ESPECÍMENES DETECTADOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Color(0xFFFFCA28),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    val isSeen = stats.hasSeenMonster(entry.type.name)
                    val isSelected = entry.type == selectedType

                    MonsterListItemCard(
                        entry = entry,
                        isSeen = isSeen,
                        isSelected = isSelected,
                        onClick = { onSelectType(entry.type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonsterListItemCard(
    entry: MonsterGuideEntry,
    isSeen: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> Color(0xFFFFD54F)
        isSeen -> Color(0xFF37474F)
        else -> Color(0x33B0BEC5)
    }
    val bgColor = when {
        isSelected -> Color(0xFF1E2E3D)
        isSeen -> Color(0xCC16222E)
        else -> Color(0x990C1217)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(if (isSelected) 1.8.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("guide_item_${entry.type.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature Canvas Icon or Shadow
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                .background(if (isSeen) Color(0xFF0F1720) else Color(0xFF080C10))
                .border(1.dp, if (isSeen) Color(entry.threatColorHex).copy(alpha = 0.6f) else Color(0x33455A64), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isSeen) {
                    Canvas(modifier = Modifier.size(38.dp)) {
                        drawGuideMonster(
                            type = entry.type,
                            isSilhouette = false,
                            animFrame = 0f,
                            scale = if (entry.type == EnemyType.JEFE) 0.45f else (if (entry.type == EnemyType.BRUTO) 0.65f else 0.85f),
                            centerX = size.width * 0.5f,
                            centerY = size.height * 0.85f
                        )
                    }
                } else {
                    // Dark Shadow Silhouette Thumbnail with question mark
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(38.dp)) {
                            drawGuideMonster(
                                type = entry.type,
                                isSilhouette = true,
                                animFrame = 0f,
                                scale = if (entry.type == EnemyType.JEFE) 0.45f else (if (entry.type == EnemyType.BRUTO) 0.65f else 0.85f),
                                centerX = size.width * 0.5f,
                                centerY = size.height * 0.85f
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = Color(0xAA90A4AE),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isSeen) entry.name else "???",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSeen) Color.White else Color(0xFF78909C),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (entry.type == EnemyType.JEFE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFB71C1C))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "JEFE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isSeen) entry.title else "Infectado no avistado",
                    fontSize = 9.sp,
                    color = if (isSeen) Color(0xFFB0BEC5) else Color(0xFF546E7A),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSeen) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(entry.threatColorHex).copy(alpha = 0.2f))
                                .border(0.8.dp, Color(entry.threatColorHex).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = entry.threatLevel,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(entry.threatColorHex)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x44263238))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SOMBRA OSCURA",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }

                    Text(
                        text = entry.appearanceUnlockLevel,
                        fontSize = 8.5.sp,
                        color = Color(0xFF78909C)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonsterDetailPanel(
    entry: MonsterGuideEntry,
    isSeen: Boolean,
    animFrame: Float,
    shadowPulse: Float,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEE141C25))
            .border(1.2.dp, if (isSeen) Color(entry.threatColorHex).copy(alpha = 0.6f) else Color(0xFF37474F), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Stage Showcase Box
            MonsterShowcaseStage(
                entry = entry,
                isSeen = isSeen,
                animFrame = animFrame,
                shadowPulse = shadowPulse
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isSeen) {
                // ==============================
                // DISCOVERED MONSTER INFORMATION
                // ==============================

                // Header Info: Title and Threat Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                        Text(
                            text = entry.title,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD54F)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(entry.threatColorHex).copy(alpha = 0.25f))
                            .border(1.2.dp, Color(entry.threatColorHex), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (entry.type == EnemyType.JEFE) Icons.Default.Warning else Icons.Default.Security,
                                contentDescription = "Amenaza",
                                tint = Color(entry.threatColorHex),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "AMENAZA: ${entry.threatLevel}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(entry.threatColorHex)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBadge(
                        label = "SALUD BASE",
                        value = "${entry.baseHp} HP",
                        color = Color(0xFFEF5350),
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "VELOCIDAD",
                        value = "${entry.baseSpeed} KM/H",
                        color = Color(0xFF29B6F6),
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "DAÑO BASE",
                        value = "${entry.baseDamage} DMG",
                        color = Color(0xFFFFA726),
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "APARICIÓN",
                        value = entry.appearanceUnlockLevel,
                        color = Color(0xFFAB47BC),
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lore Section
                GuideInfoCard(
                    title = "📖 ORIGEN Y BIOLOGÍA",
                    content = entry.lore,
                    accentColor = Color(0xFF90CAF9)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Attack Pattern Section
                GuideInfoCard(
                    title = "⚔️ COMPORTAMIENTO Y ATAQUE",
                    content = entry.attackPattern,
                    accentColor = Color(0xFFFFB74D)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Weakness & Strategy Section
                GuideInfoCard(
                    title = "🎯 PUNTOS DÉBILES Y COMBATE",
                    content = "${entry.weaknesses}\n\n💡 Consejo de Supervivencia: ${entry.survivalTip}",
                    accentColor = Color(0xFF81C784)
                )
            } else {
                // ==============================
                // UNDISCOVERED / SILHOUETTE VIEW
                // ==============================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D1217))
                        .border(1.dp, Color(0xFF263238), RoundedCornerShape(12.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = Color(0xFFFF7043),
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ARCHIVO CLASIFICADO: ESPÉCIMEN NO AVISTADO",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB74D),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Aún no has sobrevivido lo suficiente en las vías para documentar este infectado en persona. Los supervivientes que lo han visto de lejos solo reportan una sombra oscura en las tinieblas del páramo.",
                        fontSize = 11.5.sp,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x661E2933))
                            .border(1.dp, Color(0xFF455A64), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "📌 REQUISITO: Sobrevive hasta ${entry.appearanceUnlockLevel} para registrar a este espécimen.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80D8FF),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonsterShowcaseStage(
    entry: MonsterGuideEntry,
    isSeen: Boolean,
    animFrame: Float,
    shadowPulse: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF090D12))
            .border(1.dp, if (isSeen) Color(0xFF2C3E50) else Color(0xFF1E2833), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w * 0.5f
            val groundY = h * 0.85f

            // Spotlight from ceiling
            val spotBeam = Path().apply {
                moveTo(centerX, 0f)
                lineTo(centerX - 130f, groundY)
                lineTo(centerX + 130f, groundY)
                close()
            }
            drawPath(
                path = spotBeam,
                brush = Brush.verticalGradient(
                    colors = if (isSeen) listOf(
                        Color(entry.threatColorHex).copy(alpha = 0.18f),
                        Color.Transparent
                    ) else listOf(
                        Color(0x1800E5FF),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = groundY
                )
            )

            // Railway pedestal platform
            drawRect(
                color = Color(0xFF19222B),
                topLeft = Offset(centerX - 140f, groundY),
                size = Size(280f, 22f)
            )
            // Steel rail beams
            drawLine(
                color = Color(0xFF78909C),
                start = Offset(centerX - 140f, groundY + 2f),
                end = Offset(centerX + 140f, groundY + 2f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF455A64),
                start = Offset(centerX - 140f, groundY + 12f),
                end = Offset(centerX + 140f, groundY + 12f),
                strokeWidth = 2f
            )
            // Hazard yellow/black warning stripes along pedestal
            for (i in -6..6) {
                val stripeX = centerX + i * 20f
                drawLine(
                    color = Color(0xFFFFB300).copy(alpha = 0.4f),
                    start = Offset(stripeX, groundY + 16f),
                    end = Offset(stripeX + 10f, groundY + 22f),
                    strokeWidth = 3f
                )
            }

            // Draw Monster (Full color or Dark Shadow Silhouette)
            drawGuideMonster(
                type = entry.type,
                isSilhouette = !isSeen,
                animFrame = animFrame,
                scale = if (entry.type == EnemyType.JEFE) 1.25f else (if (entry.type == EnemyType.BRUTO) 1.55f else 1.85f),
                centerX = centerX,
                centerY = groundY
            )

            // If not seen, add shadow mist and subtle "?" watermark
            if (!isSeen) {
                drawOval(
                    color = Color(0x66000000),
                    topLeft = Offset(centerX - 60f * shadowPulse, groundY - 20f),
                    size = Size(120f * shadowPulse, 24f)
                )
            }
        }

        if (!isSeen) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC000000))
                    .border(1.dp, Color(0xFF546E7A), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒 SOMBRA OSCURA - NO AVISTADO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color(0xFFFF8A80)
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xEE111822))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90A4AE)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
private fun GuideInfoCard(
    title: String,
    content: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x9910161E))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = content,
                fontSize = 11.sp,
                color = Color(0xFFCFD8DC),
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Renders the monster either in full biological detail or as a complete dark silhouette ("sombra oscura").
 */
private fun DrawScope.drawGuideMonster(
    type: EnemyType,
    isSilhouette: Boolean,
    animFrame: Float,
    scale: Float,
    centerX: Float,
    centerY: Float
) {
    val ex = centerX
    val ey = centerY
    val dir = 1f // Face right in the bestiary

    // Silhouette Colors: Pitch black with very faint dark charcoal edges
    val silColor = Color(0xFF0D1217)
    val silEdge = Color(0xFF1E2833)

    when (type) {
        EnemyType.JEFE -> {
            val bossScale = 2.0f * scale
            // Shadow
            drawOval(
                color = Color(0x99000000),
                topLeft = Offset(ex - 34f * bossScale, ey - 5f * bossScale),
                size = Size(68f * bossScale, 10f * bossScale)
            )

            if (isSilhouette) {
                // Massive silhouette block
                drawRect(color = silColor, topLeft = Offset(ex - 22f * bossScale, ey - 64f * bossScale), size = Size(44f * bossScale, 64f * bossScale))
                drawCircle(color = silColor, radius = 18f * bossScale, center = Offset(ex + 12f * bossScale, ey - 64f * bossScale))
                // Smokestacks
                drawRect(color = silColor, topLeft = Offset(ex - 20f * bossScale, ey - 82f * bossScale), size = Size(10f * bossScale, 20f * bossScale))
                // Cannon silhouette
                drawRect(color = silColor, topLeft = Offset(ex + 12f * bossScale, ey - 74f * bossScale), size = Size(26f * bossScale, 10f * bossScale))
                // Faint mystery outline
                drawCircle(color = silEdge, radius = 10f * bossScale, center = Offset(ex + 14f * bossScale, ey - 64f * bossScale), style = Stroke(width = 1.5f))
                return
            }

            // Hydraulic Legs
            val legStride = (sin(animFrame.toDouble()) * 3f).toFloat() * bossScale
            drawRect(color = Color(0xFF1A2226), topLeft = Offset(ex - 18f * bossScale - 6f * bossScale, ey - 28f * bossScale), size = Size(11f * bossScale, 28f * bossScale))
            drawCircle(color = Color(0xFF37474F), radius = 7f * bossScale, center = Offset(ex - 18f * bossScale, ey - 14f * bossScale))
            drawRect(color = Color(0xFF1A2226), topLeft = Offset(ex + 8f * bossScale - 6f * bossScale, ey - 28f * bossScale + legStride), size = Size(12f * bossScale, 28f * bossScale - legStride))
            drawRect(color = Color(0xFF0D1215), topLeft = Offset(ex - 24f * bossScale, ey - 6f * bossScale), size = Size(16f * bossScale, 6f * bossScale))
            drawRect(color = Color(0xFF0D1215), topLeft = Offset(ex + 2f * bossScale, ey - 6f * bossScale), size = Size(18f * bossScale, 6f * bossScale))

            // Torso & Chassis
            val torsoY = ey - 64f * bossScale
            drawRect(color = Color(0xFF263238), topLeft = Offset(ex - 22f * bossScale, torsoY), size = Size(44f * bossScale, 38f * bossScale))
            drawRect(color = Color(0xFF192227), topLeft = Offset(ex - 24f * bossScale, torsoY + 4f * bossScale), size = Size(48f * bossScale, 12f * bossScale))
            for (r in -2..2) {
                drawCircle(color = Color(0xFF90A4AE), radius = 1.8f * bossScale, center = Offset(ex + r * 9f * bossScale, torsoY + 10f * bossScale))
            }

            // Glowing Reactor Core
            val coreY = torsoY + 22f * bossScale
            drawCircle(color = Color(0x66FF1744), radius = 16f * bossScale, center = Offset(ex, coreY))
            drawCircle(color = Color(0xFFFF5722), radius = 11f * bossScale, center = Offset(ex, coreY))
            drawCircle(color = Color(0xFFFFEB3B), radius = 6f * bossScale, center = Offset(ex, coreY))
            drawLine(color = Color(0xFF101416), start = Offset(ex - 10f * bossScale, coreY), end = Offset(ex + 10f * bossScale, coreY), strokeWidth = 2.5f * bossScale)
            drawLine(color = Color(0xFF101416), start = Offset(ex, coreY - 10f * bossScale), end = Offset(ex, coreY + 10f * bossScale), strokeWidth = 2.5f * bossScale)

            // Smokestacks
            val stackX = ex - 16f * bossScale
            drawRect(color = Color(0xFF161E22), topLeft = Offset(stackX - 5f * bossScale, torsoY - 16f * bossScale), size = Size(10f * bossScale, 18f * bossScale))
            drawCircle(color = Color(0x44263238), radius = 8f * bossScale, center = Offset(stackX, torsoY - 22f * bossScale))

            // Bio-plasma Cannon
            val cannonX = ex + 14f * bossScale
            val cannonY = torsoY - 4f * bossScale
            drawCircle(color = Color(0xFF1B242A), radius = 10f * bossScale, center = Offset(cannonX, cannonY))
            val barrelLen = 26f * bossScale
            drawLine(color = Color(0xFF37474F), start = Offset(cannonX, cannonY), end = Offset(cannonX + barrelLen, cannonY), strokeWidth = 9f * bossScale)
            drawLine(color = Color(0xFFFF1744), start = Offset(cannonX + 4f * bossScale, cannonY - 4f * bossScale), end = Offset(cannonX + 18f * bossScale, cannonY - 4f * bossScale), strokeWidth = 2.5f * bossScale)
            drawCircle(color = Color(0xFFFF5252), radius = 5f * bossScale, center = Offset(cannonX + barrelLen, cannonY))

            // Head & Horns
            val headY = torsoY - 14f * bossScale
            drawCircle(color = Color(0xFF263238), radius = 13f * bossScale, center = Offset(ex + 10f * bossScale, headY))
            drawRect(color = Color(0xFF161E22), topLeft = Offset(ex + 12f * bossScale, headY + 3f * bossScale), size = Size(14f * bossScale, 8f * bossScale))
            for (t in 0..2) {
                drawLine(color = Color(0xFFFFEB3B), start = Offset(ex + 14f * bossScale + t * 4f * bossScale, headY + 3f * bossScale), end = Offset(ex + 14f * bossScale + t * 4f * bossScale, headY + 8f * bossScale), strokeWidth = 2f * bossScale)
            }
            drawCircle(color = Color(0xFFFF1744), radius = 3.5f * bossScale, center = Offset(ex + 18f * bossScale, headY - 3f * bossScale))
            drawCircle(color = Color(0xFFFFFF00), radius = 1.8f * bossScale, center = Offset(ex + 18f * bossScale, headY - 3f * bossScale))
        }

        EnemyType.BRUTO -> {
            // Shadow
            drawOval(color = Color(0x77000000), topLeft = Offset(ex - 22f * scale, ey - 4f * scale), size = Size(44f * scale, 8f * scale))

            if (isSilhouette) {
                drawRect(color = silColor, topLeft = Offset(ex - 15f * scale, ey - 42f * scale), size = Size(30f * scale, 42f * scale))
                drawCircle(color = silColor, radius = 11f * scale, center = Offset(ex + 3f * scale, ey - 52f * scale))
                // Horns silhouette
                val horn = Path().apply {
                    moveTo(ex, ey - 56f * scale)
                    lineTo(ex + 14f * scale, ey - 68f * scale)
                    lineTo(ex + 6f * scale, ey - 54f * scale)
                    close()
                }
                drawPath(horn, color = silColor)
                return
            }

            val stride = (sin(animFrame.toDouble()) * 2.5f).toFloat() * scale
            val brutoColor = Color(0xFF4E1620)
            val armorColor = Color(0xFF263238)

            // Stomping Legs
            drawRect(color = brutoColor, topLeft = Offset(ex - 12f * scale, ey - 18f * scale), size = Size(9f * scale, 18f * scale))
            drawRect(color = brutoColor, topLeft = Offset(ex + 3f * scale, ey - 18f * scale + stride), size = Size(9f * scale, 18f * scale - stride))
            drawRect(color = armorColor, topLeft = Offset(ex - 13f * scale, ey - 12f * scale), size = Size(11f * scale, 6f * scale))
            drawRect(color = armorColor, topLeft = Offset(ex + 2f * scale, ey - 12f * scale + stride * 0.5f), size = Size(11f * scale, 6f * scale))

            // Torso with Molten Heart
            val torsoY = ey - 42f * scale
            drawRect(color = brutoColor, topLeft = Offset(ex - 15f * scale, torsoY), size = Size(30f * scale, 26f * scale))
            drawRect(color = armorColor, topLeft = Offset(ex - 16f * scale, torsoY + 2f * scale), size = Size(32f * scale, 12f * scale))
            drawCircle(color = Color(0xFFFF5722), radius = 5.5f * scale, center = Offset(ex, torsoY + 16f * scale))
            drawCircle(color = Color(0xFFFFD54F), radius = 2.5f * scale, center = Offset(ex, torsoY + 16f * scale))

            // Spiked Fist
            val armY = torsoY + 4f * scale
            val fistX = ex + 18f * scale
            drawLine(color = brutoColor, start = Offset(ex + 8f * scale, armY), end = Offset(fistX, armY + 14f * scale), strokeWidth = 7f * scale)
            drawCircle(color = Color(0xFFB0BEC5), radius = 5.5f * scale, center = Offset(fistX, armY + 14f * scale))

            // Head & Horns
            val headY = torsoY - 10f * scale
            drawCircle(color = brutoColor, radius = 9.5f * scale, center = Offset(ex + 3f * scale, headY))
            val horn = Path().apply {
                moveTo(ex, headY - 4f * scale)
                lineTo(ex + 14f * scale, headY - 16f * scale)
                lineTo(ex + 6f * scale, headY - 2f * scale)
                close()
            }
            drawPath(horn, color = Color(0xFFCFD8DC))
            drawCircle(color = Color(0xFFFF1744), radius = 2.4f * scale, center = Offset(ex + 7f * scale, headY - 1f * scale))
        }

        EnemyType.ERRANTE -> {
            drawOval(color = Color(0x55000000), topLeft = Offset(ex - 12f * scale, ey - 3f * scale), size = Size(24f * scale, 6f * scale))

            if (isSilhouette) {
                drawRect(color = silColor, topLeft = Offset(ex - 8f * scale, ey - 32f * scale), size = Size(16f * scale, 32f * scale))
                drawCircle(color = silColor, radius = 7f * scale, center = Offset(ex + 1f * scale, ey - 40f * scale))
                drawLine(color = silColor, start = Offset(ex + 5f * scale, ey - 26f * scale), end = Offset(ex + 18f * scale, ey - 20f * scale), strokeWidth = 4f * scale)
                return
            }

            val stride = (sin(animFrame.toDouble()) * 2f).toFloat() * scale
            val skinColor = Color(0xFF455A52)
            val clothColor = Color(0xFF263238)

            // Legs
            drawLine(color = clothColor, start = Offset(ex - 4f * scale, ey - 14f * scale), end = Offset(ex - 4f * scale, ey), strokeWidth = 3.5f * scale)
            drawLine(color = clothColor, start = Offset(ex + 4f * scale, ey - 14f * scale), end = Offset(ex + 4f * scale, ey + stride), strokeWidth = 3.5f * scale)
            drawRect(color = Color(0xFF78909C), topLeft = Offset(ex + 2f * scale, ey - 9f * scale + stride * 0.5f), size = Size(5f * scale, 6f * scale))

            // Torso
            val torsoY = ey - 32f * scale
            drawRect(color = skinColor, topLeft = Offset(ex - 8f * scale, torsoY), size = Size(16f * scale, 20f * scale))
            drawRect(color = clothColor, topLeft = Offset(ex - 8.5f * scale, torsoY + 12f * scale), size = Size(17f * scale, 7f * scale))
            for (i in 0..2) {
                val spikeY = torsoY + 2f * scale + i * 5f * scale
                drawLine(color = Color(0xFFBCAAA4), start = Offset(ex - 8f * scale, spikeY), end = Offset(ex - 14f * scale, spikeY - 3f * scale), strokeWidth = 2.2f * scale)
            }
            drawLine(color = Color(0xBB76FF03), start = Offset(ex - 3f * scale, torsoY + 4f * scale), end = Offset(ex + 2f * scale, torsoY + 10f * scale), strokeWidth = 1.8f * scale)

            // Claw Arm
            val clawArmY = torsoY + 6f * scale
            val clawEndX = ex + 15f * scale
            drawLine(color = skinColor, start = Offset(ex + 5f * scale, clawArmY), end = Offset(clawEndX, clawArmY + 5f * scale), strokeWidth = 3.2f * scale)
            drawLine(color = Color(0xFFCFD8DC), start = Offset(clawEndX, clawArmY + 5f * scale), end = Offset(clawEndX + 7f * scale, clawArmY + 2f * scale), strokeWidth = 2f * scale)
            drawLine(color = Color(0xFFCFD8DC), start = Offset(clawEndX, clawArmY + 5f * scale), end = Offset(clawEndX + 7f * scale, clawArmY + 8f * scale), strokeWidth = 2f * scale)

            // Head
            val headY = torsoY - 8f * scale
            drawCircle(color = skinColor, radius = 7f * scale, center = Offset(ex + 1f * scale, headY))
            drawRect(color = Color(0xFF78909C), topLeft = Offset(ex, headY + 3f * scale), size = Size(7f * scale, 4f * scale))
            val eyeX = ex + 4f * scale
            drawCircle(color = Color(0xFFFFD54F), radius = 1.8f * scale, center = Offset(eyeX, headY - 1f * scale))
        }

        EnemyType.CORREDOR -> {
            drawOval(color = Color(0x55000000), topLeft = Offset(ex - 18f * scale, ey - 3f * scale), size = Size(36f * scale, 6f * scale))

            if (isSilhouette) {
                drawOval(color = silColor, topLeft = Offset(ex - 16f * scale, ey - 18f * scale), size = Size(32f * scale, 18f * scale))
                drawCircle(color = silColor, radius = 7f * scale, center = Offset(ex + 14f * scale, ey - 14f * scale))
                return
            }

            val beastColor = Color(0xFF421D24)
            val spineColor = Color(0xFF880E4F)

            drawOval(color = beastColor, topLeft = Offset(ex - 16f * scale, ey - 18f * scale), size = Size(32f * scale, 12f * scale))
            for (q in 0..3) {
                val qx = ex - 10f * scale + q * 6f * scale
                drawLine(color = spineColor, start = Offset(qx, ey - 17f * scale), end = Offset(qx - 5f * scale, ey - 24f * scale), strokeWidth = 2.4f * scale)
            }
            // Tail
            val tailPath = Path().apply {
                moveTo(ex - 15f * scale, ey - 14f * scale)
                quadraticTo(ex - 24f * scale, ey - 22f * scale, ex - 28f * scale, ey - 10f * scale)
            }
            drawPath(tailPath, color = Color(0xFFB71C1C), style = Stroke(width = 2.5f * scale))

            // 4 Running Legs
            drawLine(color = beastColor, start = Offset(ex - 10f * scale, ey - 10f * scale), end = Offset(ex - 14f * scale, ey), strokeWidth = 3f * scale)
            drawLine(color = beastColor, start = Offset(ex + 8f * scale, ey - 10f * scale), end = Offset(ex + 16f * scale, ey), strokeWidth = 3f * scale)

            // Head & Eyes
            val headX = ex + 14f * scale
            val headY = ey - 14f * scale
            drawCircle(color = beastColor, radius = 6.5f * scale, center = Offset(headX, headY))
            drawRect(color = Color(0xFFB71C1C), topLeft = Offset(headX, headY - 1f * scale), size = Size(6f * scale, 4f * scale))
            drawCircle(color = Color(0xFFFF1744), radius = 2.2f * scale, center = Offset(headX + 2f * scale, headY - 2f * scale))
        }

        EnemyType.TREPADOR -> {
            drawOval(color = Color(0x44000000), topLeft = Offset(ex - 14f * scale, ey - 2f * scale), size = Size(28f * scale, 5f * scale))

            if (isSilhouette) {
                drawCircle(color = silColor, radius = 9f * scale, center = Offset(ex - 7f * scale, ey - 16f * scale))
                drawCircle(color = silColor, radius = 5.5f * scale, center = Offset(ex + 6f * scale, ey - 16f * scale))
                return
            }

            val spiderColor = Color(0xFF281335)
            val venomColor = Color(0xFFE040FB)
            val legBob = (sin(animFrame.toDouble()) * 2f).toFloat() * scale

            // Abdomen
            val abdoX = ex - 7f * scale
            val abdoY = ey - 16f * scale
            drawCircle(color = spiderColor, radius = 9f * scale, center = Offset(abdoX, abdoY))
            drawCircle(color = venomColor, radius = 4f * scale, center = Offset(abdoX, abdoY))

            // Head
            val headX = ex + 6f * scale
            val headY = ey - 16f * scale
            drawCircle(color = spiderColor, radius = 5.5f * scale, center = Offset(headX, headY))
            drawCircle(color = Color(0xFF00E5FF), radius = 1.8f * scale, center = Offset(headX + 3f * scale, headY - 2f * scale))

            // 6 Articulated Legs
            for (i in 0..2) {
                val legBaseX = ex - 4f * scale + i * 5f * scale
                val kneeY = ey - 26f * scale + (if (i % 2 == 0) legBob else -legBob)
                val footX = ex - 12f * scale + i * 12f * scale
                drawLine(color = spiderColor, start = Offset(legBaseX, headY), end = Offset(legBaseX - 3f * scale, kneeY), strokeWidth = 2.2f * scale)
                drawLine(color = Color(0xFF7B1FA2), start = Offset(legBaseX - 3f * scale, kneeY), end = Offset(footX, ey), strokeWidth = 2f * scale)
            }
        }
    }
}

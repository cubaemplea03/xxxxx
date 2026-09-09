package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

data class TrainUpgradeItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val currentLevel: Int,
    val maxLevel: Int,
    val nextCost: Int,
    val requiredLevel: Int,
    val benefits: List<String>
)

@Composable
fun TrainUpgradesScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val availableGold = maxOf(stats.totalGold, viewModel.engine.player.gold)
    val playerLevel = maxOf(stats.highestLevel, viewModel.engine.playerSurvivalLevel)

    val upgrades = listOf(
        TrainUpgradeItem(
            id = "TURRET",
            name = "Torreta Centinela (Gatling)",
            description = "Ametralladora automática montada en la plataforma. Dispara ráfagas a los enemigos cercanos para defenderte.",
            icon = Icons.Default.PrecisionManufacturing,
            iconColor = Color(0xFF00E676),
            currentLevel = stats.turretLevel,
            maxLevel = 3,
            nextCost = when (stats.turretLevel) {
                0 -> 50
                1 -> 110
                2 -> 220
                else -> 0
            },
            requiredLevel = when (stats.turretLevel) {
                0 -> 0
                1 -> 1
                2 -> 3
                else -> 0
            },
            benefits = listOf("Nivel 1: Disparo automático [Nivel 0]", "Nivel 2: +45% Cadencia [Nivel 1: 15 bajas]", "Nivel 3: Munición perforante [Nivel 3: 45 bajas]")
        ),
        TrainUpgradeItem(
            id = "CANNON",
            name = "Cañón Pesado de Techo",
            description = "Cañón de artillería montado sobre el vagón blindado. Lanza proyectiles explosivos de área masiva.",
            icon = Icons.Default.Shield,
            iconColor = Color(0xFFFF5722),
            currentLevel = stats.cannonLevel,
            maxLevel = 3,
            nextCost = when (stats.cannonLevel) {
                0 -> 75
                1 -> 160
                2 -> 300
                else -> 0
            },
            requiredLevel = when (stats.cannonLevel) {
                0 -> 1
                1 -> 2
                2 -> 4
                else -> 0
            },
            benefits = listOf("Nivel 1: Explosión en área [Nivel 1: 15 bajas]", "Nivel 2: +Radio de impacto AOE [Nivel 2: 30 bajas]", "Nivel 3: Daño crítico [Nivel 4: 60 bajas]")
        ),
        TrainUpgradeItem(
            id = "ARMOR",
            name = "Púas & Blindaje Frontal",
            description = "Defensa reforzada en la locomotora. Empala asaltantes al contacto y reduce el daño total que sufres.",
            icon = Icons.Default.Security,
            iconColor = Color(0xFFFFCA28),
            currentLevel = stats.armorLevel,
            maxLevel = 3,
            nextCost = when (stats.armorLevel) {
                0 -> 40
                1 -> 90
                2 -> 180
                else -> 0
            },
            requiredLevel = when (stats.armorLevel) {
                0 -> 0
                1 -> 1
                2 -> 3
                else -> 0
            },
            benefits = listOf("Nivel 1: -12% Daño recibido [Nivel 0]", "Nivel 2: Púas frontales letales [Nivel 1: 15 bajas]", "Nivel 3: -36% Daño blindado total [Nivel 3: 45 bajas]")
        ),
        TrainUpgradeItem(
            id = "SPOTLIGHT",
            name = "Foco Reflector Táctico",
            description = "Proyector de alta potencia en la cabina. Ilumina la niebla y ralentiza en 25% a los monstruos en su haz.",
            icon = Icons.Default.FlashlightOn,
            iconColor = Color(0xFF40C4FF),
            currentLevel = stats.spotlightLevel,
            maxLevel = 2,
            nextCost = when (stats.spotlightLevel) {
                0 -> 35
                1 -> 80
                else -> 0
            },
            requiredLevel = when (stats.spotlightLevel) {
                0 -> 0
                1 -> 2
                else -> 0
            },
            benefits = listOf("Nivel 1: Haz luminoso amplio [Nivel 0]", "Nivel 2: Ralentización cegadora (-25%) [Nivel 2: 30 bajas]")
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D131A), Color(0xFF141C24), Color(0xFF1B242D))
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row: Back button, title, current gold display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x99263238))
                            .border(1.dp, Color(0x55FFFFFF), CircleShape)
                            .testTag("upgrades_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "TALLER DE MEJORAS DEL TREN",
                            color = Color(0xFFFFD54F),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Instala y mejora torretas y cañones para defender el convoy",
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.5.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Player Survival Level badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xEE102A43))
                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Nivel",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "NIVEL $playerLevel SUPERVIVIENTE",
                                color = Color(0xFFE0F7FA),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Available Gold badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xEE2A1D05))
                            .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Oro",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$availableGold ORO",
                                color = Color(0xFFFFE082),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal row of upgrade cards
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(upgrades) { item ->
                    UpgradeCard(
                        item = item,
                        availableGold = availableGold,
                        playerSurvivalLevel = playerLevel,
                        onBuy = {
                            viewModel.buyTrainUpgrade(item.id, item.nextCost)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    item: TrainUpgradeItem,
    availableGold: Int,
    playerSurvivalLevel: Int,
    onBuy: () -> Unit
) {
    val isMaxed = item.currentLevel >= item.maxLevel
    val isLevelUnlocked = playerSurvivalLevel >= item.requiredLevel
    val canAfford = availableGold >= item.nextCost && !isMaxed && isLevelUnlocked

    Box(
        modifier = Modifier
            .width(210.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE6161F28))
            .border(
                1.dp,
                if (!isLevelUnlocked) Color(0x44E53935)
                else if (item.currentLevel > 0) item.iconColor.copy(alpha = 0.6f)
                else Color(0x33FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable upper content inside the card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top: Icon & Level Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLevelUnlocked) item.iconColor.copy(alpha = 0.15f) else Color(0x33B0BEC5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLevelUnlocked) item.icon else Icons.Default.Lock,
                            contentDescription = item.name,
                            tint = if (isLevelUnlocked) item.iconColor else Color(0xFFB0BEC5),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (!isLevelUnlocked) Color(0x33E53935)
                                else if (item.currentLevel > 0) item.iconColor.copy(alpha = 0.2f)
                                else Color(0x33FFFFFF)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = if (isMaxed) "MÁXIMO"
                                   else if (!isLevelUnlocked) "REQ. NIVEL ${item.requiredLevel}"
                                   else "NIVEL ${item.currentLevel}/${item.maxLevel}",
                            color = if (!isLevelUnlocked) Color(0xFFFF8A80)
                                    else if (item.currentLevel > 0) item.iconColor
                                    else Color(0xFFB0BEC5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Description
                Text(
                    text = item.description,
                    color = Color(0xFF90A4AE),
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp,
                    maxLines = 2
                )

                if (!isLevelUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33E53935))
                            .border(1.dp, Color(0x66FF5252), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🔒 Requiere Nivel ${item.requiredLevel} (${item.requiredLevel * 10} bajas)",
                            color = Color(0xFFFF8A80),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Benefits breakdown
                item.benefits.forEachIndexed { idx, benefit ->
                    val isUnlocked = idx < item.currentLevel
                    Text(
                        text = if (isUnlocked) "✓ $benefit" else "• $benefit",
                        color = if (isUnlocked) Color(0xFF81C784) else Color(0xFF546E7A),
                        fontSize = 8.5.sp,
                        lineHeight = 11.sp,
                        fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Purchase / Action Button - FIXED AT BOTTOM, ALWAYS VISIBLE
            Button(
                onClick = onBuy,
                enabled = canAfford,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("buy_${item.id.lowercase()}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) Color(0xFFFFB300) else Color(0xFF37474F),
                    disabledContainerColor = Color(0xFF263238),
                    contentColor = Color.Black,
                    disabledContentColor = Color(0xFF78909C)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                if (isMaxed) {
                    Text(
                        text = "COMPLETO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (!isLevelUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF90A4AE)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "REQ. NIVEL ${item.requiredLevel}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.currentLevel == 0) "INSTALAR (${item.nextCost})"
                                   else "MEJORAR (${item.nextCost})",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

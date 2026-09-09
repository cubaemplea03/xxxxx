package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.model.WARDROBE_SKINS
import com.example.game.model.WardrobeSkinItem
import com.example.game.model.WardrobeSlot

@Composable
fun WardrobeScreen(
    viewModel: GameViewModel,
    onBackClick: () -> Unit
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var selectedSlot by remember { mutableStateOf(WardrobeSlot.CABEZA) }

    // Preview state: temporarily shows selected skin on mannequin before purchasing or equipping
    var previewHead by remember(stats?.equippedHead) { mutableStateOf(stats?.equippedHead ?: "HEAD_DEFAULT") }
    var previewChest by remember(stats?.equippedChest) { mutableStateOf(stats?.equippedChest ?: "CHEST_DEFAULT") }
    var previewLegs by remember(stats?.equippedLegs) { mutableStateOf(stats?.equippedLegs ?: "LEGS_DEFAULT") }

    // Keep preview in sync when equipped items change
    LaunchedEffect(stats?.equippedHead, stats?.equippedChest, stats?.equippedLegs) {
        previewHead = stats?.equippedHead ?: "HEAD_DEFAULT"
        previewChest = stats?.equippedChest ?: "CHEST_DEFAULT"
        previewLegs = stats?.equippedLegs ?: "LEGS_DEFAULT"
    }

    val currentGold = stats?.totalGold ?: 0

    // Animation transition for idle breathing mannequin
    val infiniteTransition = rememberInfiniteTransition(label = "mannequin_anim")
    val idleFrame by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_frame"
    )

    val currentSlotSkins = remember(selectedSlot) {
        WARDROBE_SKINS.filter { it.slot == selectedSlot }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF090D16),
                        Color(0xFF05070B)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // TOP BAR: BACK, TITLE, CURRENT GOLD
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF131D2E).copy(alpha = 0.95f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .testTag("btn_wardrobe_back")
                                .size(44.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color(0xFFE2E8F0)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "VESTUARIO TÁCTICO",
                                    color = Color(0xFFF8FAFC),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "3 FASES",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = Color(0xFFFBBF24),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Personaliza Cabeza, Pecho y Piernas con Oro",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // GOLD CHIP
                    Surface(
                        color = Color(0xFF1E1B10),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "🪙", fontSize = 16.sp)
                            Text(
                                text = "$currentGold ORO",
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // ==========================================
            // MAIN BODY: SPLIT VIEW (MANNEQUIN vs CATALOG)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ----------------------------------------------------
                // LEFT PANE: LIVE CHARACTER PREVIEW ON PODIUM
                // ----------------------------------------------------
                Surface(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF111827).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title pill
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text(
                                text = "VISTA PREVIA EN VIVO",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Mannequin interactive display
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                val centerX = size.width / 2f
                                val groundY = size.height * 0.78f

                                // Hologram light beam from ceiling
                                drawOval(
                                    color = Color(0x1538BDF8),
                                    topLeft = Offset(centerX - 85.dp.toPx(), 10.dp.toPx()),
                                    size = Size(170.dp.toPx(), 40.dp.toPx())
                                )

                                // Illuminated Industrial Pedestal
                                drawOval(
                                    color = Color(0xFF0F172A),
                                    topLeft = Offset(centerX - 100.dp.toPx(), groundY - 12.dp.toPx()),
                                    size = Size(200.dp.toPx(), 34.dp.toPx())
                                )
                                drawOval(
                                    color = Color(0xFF1E293B),
                                    topLeft = Offset(centerX - 92.dp.toPx(), groundY - 8.dp.toPx()),
                                    size = Size(184.dp.toPx(), 26.dp.toPx())
                                )
                                // Cyan ring glow
                                drawOval(
                                    color = Color(0x3338BDF8),
                                    topLeft = Offset(centerX - 80.dp.toPx(), groundY - 5.dp.toPx()),
                                    size = Size(160.dp.toPx(), 20.dp.toPx())
                                )

                                // Draw character mannequin with equipped or previewed skins
                                drawCharacterMannequin(
                                    headSkin = previewHead,
                                    chestSkin = previewChest,
                                    legsSkin = previewLegs,
                                    px = centerX,
                                    py = groundY,
                                    scale = 3.8f,
                                    facingRight = true,
                                    animFrame = idleFrame,
                                    isCrouch = false,
                                    showWeapon = true
                                )
                            }
                        }

                        // Equipped slots quick badge indicator
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                EquippedSlotRow(
                                    label = "🪖 Cabeza",
                                    skinId = previewHead,
                                    isDefault = previewHead == "HEAD_DEFAULT"
                                )
                                EquippedSlotRow(
                                    label = "🦺 Pecho",
                                    skinId = previewChest,
                                    isDefault = previewChest == "CHEST_DEFAULT"
                                )
                                EquippedSlotRow(
                                    label = "👖 Piernas",
                                    skinId = previewLegs,
                                    isDefault = previewLegs == "LEGS_DEFAULT"
                                )
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // RIGHT PANE: 3 CATEGORY TABS & SKINS CATALOG
                // ----------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                ) {
                    // TAB SELECTOR FOR 3 PURCHASE PHASES
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            WardrobeSlot.entries.forEach { slot ->
                                val isSelected = selectedSlot == slot
                                val currentEquippedInSlot = when (slot) {
                                    WardrobeSlot.CABEZA -> stats?.equippedHead ?: "HEAD_DEFAULT"
                                    WardrobeSlot.PECHO -> stats?.equippedChest ?: "CHEST_DEFAULT"
                                    WardrobeSlot.PIERNAS -> stats?.equippedLegs ?: "LEGS_DEFAULT"
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedSlot = slot
                                            // Reset preview to current equipped for that slot
                                            when (slot) {
                                                WardrobeSlot.CABEZA -> previewHead = stats?.equippedHead ?: "HEAD_DEFAULT"
                                                WardrobeSlot.PECHO -> previewChest = stats?.equippedChest ?: "CHEST_DEFAULT"
                                                WardrobeSlot.PIERNAS -> previewLegs = stats?.equippedLegs ?: "LEGS_DEFAULT"
                                            }
                                        },
                                    color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${slot.iconTag} ${slot.displayName.uppercase()}",
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // SKINS LIST FOR ACTIVE CATEGORY
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentSlotSkins, key = { it.id }) { skin ->
                            val isOwned = stats?.isSkinOwned(skin.id) ?: (skin.costGold == 0)
                            val isEquipped = when (skin.slot) {
                                WardrobeSlot.CABEZA -> stats?.equippedHead == skin.id
                                WardrobeSlot.PECHO -> stats?.equippedChest == skin.id
                                WardrobeSlot.PIERNAS -> stats?.equippedLegs == skin.id
                            }
                            val isPreviewed = when (skin.slot) {
                                WardrobeSlot.CABEZA -> previewHead == skin.id
                                WardrobeSlot.PECHO -> previewChest == skin.id
                                WardrobeSlot.PIERNAS -> previewLegs == skin.id
                            }

                            SkinItemCard(
                                skin = skin,
                                isOwned = isOwned,
                                isEquipped = isEquipped,
                                isPreviewed = isPreviewed,
                                playerGold = currentGold,
                                onCardClick = {
                                    // Update preview mannequin on tap
                                    when (skin.slot) {
                                        WardrobeSlot.CABEZA -> previewHead = skin.id
                                        WardrobeSlot.PECHO -> previewChest = skin.id
                                        WardrobeSlot.PIERNAS -> previewLegs = skin.id
                                    }
                                },
                                onEquipClick = {
                                    viewModel.equipSkin(skin)
                                    when (skin.slot) {
                                        WardrobeSlot.CABEZA -> previewHead = skin.id
                                        WardrobeSlot.PECHO -> previewChest = skin.id
                                        WardrobeSlot.PIERNAS -> previewLegs = skin.id
                                    }
                                },
                                onBuyClick = {
                                    viewModel.purchaseSkin(skin) { success ->
                                        if (success) {
                                            when (skin.slot) {
                                                WardrobeSlot.CABEZA -> previewHead = skin.id
                                                WardrobeSlot.PECHO -> previewChest = skin.id
                                                WardrobeSlot.PIERNAS -> previewLegs = skin.id
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EquippedSlotRow(
    label: String,
    skinId: String,
    isDefault: Boolean
) {
    val skinItem = remember(skinId) { WARDROBE_SKINS.firstOrNull { it.id == skinId } }
    val displayName = skinItem?.name ?: if (isDefault) "Estándar" else skinId

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = displayName,
            color = if (isDefault) Color(0xFF94A3B8) else Color(0xFF38BDF8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun SkinItemCard(
    skin: WardrobeSkinItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    isPreviewed: Boolean,
    playerGold: Int,
    onCardClick: () -> Unit,
    onEquipClick: () -> Unit,
    onBuyClick: () -> Unit
) {
    val borderColor = when {
        isEquipped -> Color(0xFF10B981)
        isPreviewed -> Color(0xFF38BDF8)
        else -> Color(0xFF334155)
    }

    val canAfford = playerGold >= skin.costGold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("skin_card_${skin.id}")
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPreviewed) Color(0xFF1E293B) else Color(0xFF0F172A)
        ),
        border = androidx.compose.foundation.BorderStroke(if (isEquipped || isPreviewed) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Color Swatch / Icon Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(skin.primaryColorHex))
                    .border(1.dp, Color(skin.accentColorHex), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Secondary color accent line
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(skin.accentColorHex))
                )
            }

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = skin.name,
                        color = Color(0xFFF1F5F9),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    // Rarity pill
                    Surface(
                        color = Color(skin.rarityColorHex).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(skin.rarityColorHex).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = skin.rarity,
                            color = Color(skin.rarityColorHex),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = skin.description,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 2
                )

                // Cost / Status tag
                if (!isOwned) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(text = "🪙", fontSize = 11.sp)
                        Text(
                            text = "${skin.costGold} ORO",
                            color = if (canAfford) Color(0xFFFFD54F) else Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Action Button
            when {
                isEquipped -> {
                    Surface(
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "EQUIPADO",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                isOwned -> {
                    Button(
                        onClick = onEquipClick,
                        modifier = Modifier
                            .testTag("btn_equip_${skin.id}")
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "EQUIPAR",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = onBuyClick,
                        enabled = canAfford,
                        modifier = Modifier
                            .testTag("btn_buy_${skin.id}")
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            disabledContainerColor = Color(0xFF334155)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!canAfford) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = if (canAfford) "COMPRAR" else "FALTA ORO",
                                color = if (canAfford) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

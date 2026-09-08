package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.GameSettingsEntity

@Composable
fun GameControls(
    settings: GameSettingsEntity,
    onLeftPress: (Boolean) -> Unit,
    onRightPress: (Boolean) -> Unit,
    onCrouchPress: (Boolean) -> Unit,
    onJump: () -> Unit,
    onMeleeAttack: () -> Unit,
    onShootAttack: () -> Unit,
    onInteract: () -> Unit,
    ammoCount: Int,
    modifier: Modifier = Modifier
) {
    val isLarge = settings.controlSize == "GRANDE"
    val btnSize: Dp = if (isLarge) 72.dp else 58.dp
    val baseAlpha = settings.controlOpacity.coerceIn(0.3f, 1.0f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        // Left Side: D-Pad Directional Controls (Left, Right, Crouch)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Move Button
            HoldableButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "IZQ",
                size = btnSize,
                alpha = baseAlpha,
                testTag = "btn_left",
                onHoldState = onLeftPress
            )

            // Right Move Button
            HoldableButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "DER",
                size = btnSize,
                alpha = baseAlpha,
                testTag = "btn_right",
                onHoldState = onRightPress
            )

            // Crouch / Drop Button
            HoldableButton(
                icon = Icons.Default.ArrowDownward,
                label = "ABAJO",
                size = btnSize * 0.85f,
                alpha = baseAlpha * 0.9f,
                testTag = "btn_crouch",
                onHoldState = onCrouchPress
            )
        }

        // Right Side: Action Buttons (Jump, Melee, Shoot, Interact)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Interact / Stoke Furnace / Loot Crate
            TapActionButton(
                icon = Icons.Default.Bolt,
                label = "ACCIÓN",
                size = btnSize * 0.9f,
                alpha = baseAlpha,
                badge = null,
                accentColor = Color(0xFFFFB300),
                testTag = "btn_interact",
                onTap = onInteract
            )

            // Shoot Button
            TapActionButton(
                icon = Icons.Default.GpsFixed,
                label = "DISPARO",
                size = btnSize * 0.9f,
                alpha = if (ammoCount > 0) baseAlpha else baseAlpha * 0.45f,
                badge = if (ammoCount > 0) "$ammoCount" else "0",
                accentColor = Color(0xFF42A5F5),
                testTag = "btn_shoot",
                onTap = onShootAttack
            )

            // Melee Attack Button
            TapActionButton(
                icon = Icons.Default.SportsMma,
                label = "ATACAR",
                size = btnSize,
                alpha = baseAlpha,
                badge = null,
                accentColor = Color(0xFFFF7043),
                testTag = "btn_attack",
                onTap = onMeleeAttack
            )

            // Jump Button
            TapActionButton(
                icon = Icons.Default.ArrowUpward,
                label = "SALTAR",
                size = btnSize * 1.08f,
                alpha = baseAlpha,
                badge = null,
                accentColor = Color(0xFF26A69A),
                testTag = "btn_jump",
                onTap = onJump
            )
        }
    }
}

@Composable
private fun HoldableButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    alpha: Float,
    testTag: String,
    onHoldState: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    if (isPressed) listOf(
                        Color(0xFF455A64).copy(alpha = alpha),
                        Color(0xFF263238).copy(alpha = alpha)
                    )
                    else listOf(
                        Color(0xFF263238).copy(alpha = alpha * 0.85f),
                        Color(0xFF1E272C).copy(alpha = alpha * 0.95f)
                    )
                )
            )
            .border(
                1.5.dp,
                if (isPressed) Color(0xFFFFCA28).copy(alpha = alpha)
                else Color(0x66FFFFFF).copy(alpha = alpha),
                RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onHoldState(true)
                    waitForUpOrCancellation()
                    isPressed = false
                    onHoldState(false)
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPressed) Color(0xFFFFCA28) else Color.White,
                modifier = Modifier.size(size * 0.44f)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color(0xFFFFCA28) else Color(0xFFCFD8DC)
            )
        }
    }
}

@Composable
private fun TapActionButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    alpha: Float,
    badge: String?,
    accentColor: Color,
    testTag: String,
    onTap: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    if (isPressed) listOf(
                        accentColor.copy(alpha = alpha),
                        Color(0xFF1E272C).copy(alpha = alpha)
                    )
                    else listOf(
                        Color(0xFF2C3942).copy(alpha = alpha * 0.85f),
                        Color(0xFF151D22).copy(alpha = alpha * 0.95f)
                    )
                )
            )
            .border(
                2.dp,
                if (isPressed) accentColor else accentColor.copy(alpha = alpha * 0.8f),
                CircleShape
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onTap()
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPressed) Color.White else accentColor,
                modifier = Modifier.size(size * 0.42f)
            )
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        // Optional badge (e.g. Ammo count)
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

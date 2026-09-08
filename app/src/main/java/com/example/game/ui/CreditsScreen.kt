package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreditsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080C10), Color(0xFF11171E), Color(0xFF19222C))
                )
            )
            .padding(horizontal = 36.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF263238),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                        .testTag("btn_credits_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VOLVER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "CRÉDITOS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = Color(0xFFFFB300)
                )

                // Placeholder balance for alignment
                Spacer(modifier = Modifier.width(90.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Logo Title
            Text(
                text = "ÚLTIMO TREN",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "“Un juego de supervivencia y aventura 2D”",
                fontSize = 13.sp,
                color = Color(0xFFFFCA28),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Roles Grid in 2 Columns in Landscape
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x88131B22))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CreditRole(role = "DIRECCIÓN DE PROYECTO", name = "Antigravity Creative Studio")
                    CreditRole(role = "PROGRAMACIÓN & MOTOR 2D", name = "Android Jetpack & DeepMind Engine Team")
                    CreditRole(role = "DISEÑO DE JUEGO", name = "Survival Platformer Collective")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CreditRole(role = "ARTE & PIXEL ART", name = "Dark Atmospheric Pixel Lab")
                    CreditRole(role = "MÚSICA & ATMÓSFERA", name = "Dark Rails Synth Synthesizer")
                    CreditRole(role = "DISEÑO DE SONIDO", name = "Procedural Audio PCM Engine")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Desarrollado para Android en orientación horizontal con Jetpack Compose.",
                fontSize = 11.sp,
                color = Color(0xFF90A4AE)
            )
        }
    }
}

@Composable
private fun CreditRole(role: String, name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = role,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color(0xFFFFB300)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFECEFF1)
        )
    }
}

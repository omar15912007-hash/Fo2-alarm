package com.fajralarm.app.ui.ringing.missions

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fajralarm.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MemoryMatrixMissionView(
    gridDimension: Int = 3, // 3: 3x3, 4: 4x4, 5: 5x5
    totalRounds: Int = 3,
    onSuccess: () -> Unit,
    onUserInteraction: () -> Unit = {}
) {
    val totalTiles = gridDimension * gridDimension
    val tilesToMemorizeCount = (gridDimension + 1).coerceAtMost(totalTiles / 2)

    var currentRound by remember { mutableIntStateOf(1) }
    var targetTiles by remember { mutableStateOf(setOf<Int>()) }
    var userSelectedTiles by remember { mutableStateOf(setOf<Int>()) }
    var isMemorizingPhase by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    fun startNewRound() {
        val rng = Random(System.nanoTime())
        val indices = (0 until totalTiles).shuffled(rng).take(tilesToMemorizeCount).toSet()
        targetTiles = indices
        userSelectedTiles = emptySet()
        isMemorizingPhase = true
        isError = false
    }

    LaunchedEffect(currentRound) {
        startNewRound()
    }

    LaunchedEffect(isMemorizingPhase) {
        if (isMemorizingPhase) {
            delay(2200) // Show pattern for 2.2 seconds
            isMemorizingPhase = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = VividCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Memory Matrix",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Text(
                    text = "Round $currentRound / $totalRounds",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VividCyan
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isMemorizingPhase) "👀 Memorize the glowing blue tiles!" else "👉 Tap the tiles you memorized (${userSelectedTiles.size}/$tilesToMemorizeCount)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isMemorizingPhase) VividCyan else TextWhite
            )
        }

        // The Memory Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridDimension),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(totalTiles) { index ->
                    val shouldGlow = (isMemorizingPhase && targetTiles.contains(index)) ||
                            (!isMemorizingPhase && userSelectedTiles.contains(index))

                    val tileColor by animateColorAsState(
                        targetValue = when {
                            isError && userSelectedTiles.contains(index) -> DangerRed
                            shouldGlow -> ElectricBlue
                            else -> CardBackground
                        },
                        label = "tileColor"
                    )

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tileColor)
                            .bouncyClickable(enabled = !isMemorizingPhase) {
                                onUserInteraction()
                                if (!targetTiles.contains(index)) {
                                    // Wrong tile tapped!
                                    isError = true
                                    userSelectedTiles = userSelectedTiles + index
                                    // Restart round after brief penalty delay
                                    startNewRound()
                                } else {
                                    val updated = userSelectedTiles + index
                                    userSelectedTiles = updated
                                    if (updated.containsAll(targetTiles)) {
                                        if (currentRound >= totalRounds) {
                                            onSuccess()
                                        } else {
                                            currentRound++
                                        }
                                    }
                                }
                            }
                    )
                }
            }
        }

        // Subtitle Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Text(
                text = "Recalling tile patterns activates the prefrontal cortex, stimulating rapid morning awakeness.",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

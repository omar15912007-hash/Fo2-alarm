package com.fajralarm.app.ui.ringing.missions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fajralarm.app.ui.theme.*
import kotlin.random.Random

data class MathProblem(val expression: String, val answer: Int)

/**
 * Generates strictly non-negative math problems across all 5 difficulty levels.
 * Prevents negative answers because the keypad does not include a minus sign.
 */
fun generateMathProblem(difficulty: Int): MathProblem {
    val rng = Random(System.nanoTime())
    return when (difficulty) {
        1 -> { // Very Easy: single-digit addition
            val a = rng.nextInt(2, 10)
            val b = rng.nextInt(2, 10)
            MathProblem("$a + $b", a + b)
        }
        2 -> { // Easy: 2-digit addition or strictly positive subtraction
            var a = rng.nextInt(15, 50)
            var b = rng.nextInt(10, 40)
            if (rng.nextBoolean()) {
                MathProblem("$a + $b", a + b)
            } else {
                if (a < b) {
                    val temp = a
                    a = b
                    b = temp
                }
                MathProblem("$a - $b", a - b)
            }
        }
        3 -> { // Normal: multiplication + addition
            val a = rng.nextInt(3, 9)
            val b = rng.nextInt(4, 9)
            val c = rng.nextInt(5, 25)
            MathProblem("$a × $b + $c", (a * b) + c)
        }
        4 -> { // Hard: 2-digit multiplication minus number (guaranteed >= 0)
            val a = rng.nextInt(11, 18)
            val b = rng.nextInt(4, 8)
            val product = a * b
            val c = rng.nextInt(5, (product - 1).coerceAtLeast(6))
            val ans = (product - c).coerceAtLeast(0)
            MathProblem("$a × $b - $c", ans)
        }
        else -> { // Very Hard: compound equations
            val a = rng.nextInt(12, 22)
            val b = rng.nextInt(6, 11)
            val c = rng.nextInt(15, 40)
            MathProblem("($a × $b) + $c", (a * b) + c)
        }
    }
}

@Composable
fun MathMissionView(
    difficulty: Int,
    totalQuestions: Int,
    onSuccess: () -> Unit,
    onUserInteraction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentRound by remember { mutableIntStateOf(1) }
    var problem by remember { mutableStateOf(generateMathProblem(difficulty)) }
    var userInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = currentRound.toFloat() / totalQuestions.toFloat(),
        label = "mathProg"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Mission Header (Round X of Y)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = VividCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Math Mission",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Text(
                    text = "Round $currentRound of $totalQuestions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VividCyan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            GlowingGradientProgressBar(
                progress = progress,
                height = 8.dp
            )
        }

        // Center Problem Card (Takes Available Middle Space)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
                .glassCard(shape = RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = problem.expression,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (userInput.isEmpty()) "Tap answer below" else userInput,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (userInput.isEmpty()) TextMuted else VividCyan,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Custom Numeric Keypad (At Bottom, above navigation bar)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "OK")
            )

            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isOk = key == "OK"
                        val isClear = key == "C"

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isOk -> ElectricBlue
                                isClear -> CardBackground
                                else -> DarkSurface
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .bouncyClickable {
                                    onUserInteraction()
                                    when (key) {
                                        "C" -> {
                                            userInput = ""
                                            isError = false
                                        }
                                        "OK" -> {
                                            val entered = userInput.toIntOrNull()
                                            if (entered == problem.answer) {
                                                isError = false
                                                userInput = ""
                                                if (currentRound >= totalQuestions) {
                                                    onSuccess()
                                                } else {
                                                    currentRound++
                                                    problem = generateMathProblem(difficulty)
                                                }
                                            } else {
                                                isError = true
                                                userInput = ""
                                            }
                                        }
                                        else -> {
                                            if (userInput.length < 5) {
                                                isError = false
                                                userInput += key
                                            }
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "C") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Clear",
                                        tint = TextMuted
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = if (isOk) 18.sp else 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

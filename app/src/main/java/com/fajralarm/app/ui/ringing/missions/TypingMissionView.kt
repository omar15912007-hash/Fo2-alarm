package com.fajralarm.app.ui.ringing.missions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
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

@Composable
fun TypingMissionView(
    targetQuote: String,
    onSuccess: () -> Unit,
    onUserInteraction: () -> Unit = {}
) {
    var userText by remember { mutableStateOf("") }
    var hasCompleted by remember { mutableStateOf(false) }

    val cleanTarget = targetQuote
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifEmpty { "I am awake, energized, and ready for today!" }
    val cleanInput = userText.replace(Regex("\\s+"), " ").trim()

    // Calculate match percentage
    var matchingChars = 0
    for (i in 0 until minOf(cleanInput.length, cleanTarget.length)) {
        if (cleanInput[i].equals(cleanTarget[i], ignoreCase = true)) {
            matchingChars++
        }
    }
    val matchProgress = if (cleanTarget.isNotEmpty()) {
        (matchingChars.toFloat() / cleanTarget.length.toFloat()).coerceIn(0f, 1f)
    } else 1f

    val animatedProgress by animateFloatAsState(targetValue = matchProgress, label = "typingProg")

    LaunchedEffect(cleanInput, cleanTarget) {
        if (!hasCompleted && cleanInput.equals(cleanTarget, ignoreCase = true) && cleanInput.isNotEmpty()) {
            hasCompleted = true
            onSuccess()
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
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = VividCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Typing Mission",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Text(
                    text = "${(animatedProgress * 100).toInt()}% Match",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VividCyan
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            GlowingGradientProgressBar(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Target Affirmation Quote Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "TYPE THE FOLLOWING PHRASE:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VividCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$cleanTarget\"",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite,
                    lineHeight = 26.sp
                )
            }
        }

        // Text Input Field
        OutlinedTextField(
            value = userText,
            onValueChange = {
                onUserInteraction()
                userText = it
            },
            placeholder = { Text("Start typing the quote here...", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = CardBackground,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        // Helper Note
        Text(
            text = "Accurate typing confirms consciousness and prevents sleep inertia.",
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

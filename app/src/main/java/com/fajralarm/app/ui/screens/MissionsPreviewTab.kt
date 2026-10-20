package com.fajralarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fajralarm.app.data.MissionType
import com.fajralarm.app.ui.ringing.missions.*
import com.fajralarm.app.ui.theme.*

data class MissionPreviewItem(
    val type: MissionType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun MissionsPreviewTab() {
    var testingMission by remember { mutableStateOf<MissionType?>(null) }
    var showSoundLibrary by remember { mutableStateOf(false) }
    var previewSoundName by remember { mutableStateOf("siren") }

    val missions = listOf(
        MissionPreviewItem(
            MissionType.MATH,
            "Math Problems",
            "Solve equations from 1-digit addition to multi-step arithmetic.",
            Icons.Default.Calculate
        ),
        MissionPreviewItem(
            MissionType.SHAKE,
            "Shake Phone",
            "Shake your phone vigorously using accelerometer detection.",
            Icons.Default.Vibration
        ),
        MissionPreviewItem(
            MissionType.MEMORY_MATRIX,
            "Memory Matrix",
            "Memorize and tap glowing blue tile patterns on 3x3, 4x4, or 5x5 grids.",
            Icons.Default.Psychology
        ),
        MissionPreviewItem(
            MissionType.TYPING,
            "Typing Affirmations",
            "Type wake-up affirmations and quotes accurately.",
            Icons.Default.Keyboard
        ),
        MissionPreviewItem(
            MissionType.STEPS,
            "Walk Steps Mission",
            "Hardware step detector with cadence validation (no fast shaking).",
            Icons.AutoMirrored.Filled.DirectionsWalk
        ),
        MissionPreviewItem(
            MissionType.SQUAT,
            "Squat Exercise Mission",
            "Vertical body movement detection with mandatory 800ms cooldown.",
            Icons.Default.FitnessCenter
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Missions & Preview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Test any wake-up mission instantly",
                    fontSize = 12.sp,
                    color = VividCyan
                )
            }
        }

        // Dedicated Sound & Siren Library Test Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .clickable { showSoundLibrary = true },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = VividCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ringtone & Siren Library",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Listen to and test all 22 alarm sounds & sirens",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { showSoundLibrary = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(76.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Try", fontSize = 12.sp, color = TextWhite)
                }
            }
        }

        Text(
            text = "Wake-Up Missions",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SoftBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(missions) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { testingMission = item.type },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = VividCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.subtitle,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    color = Color(0xFF8A99AD)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { testingMission = item.type },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(76.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Try", fontSize = 12.sp, color = TextWhite)
                        }
                    }
                }
            }
        }
    }

    // Interactive Mission Preview Modal
    testingMission?.let { mission ->
        Dialog(onDismissRequest = { testingMission = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PitchBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (mission) {
                        MissionType.MATH -> {
                            MathMissionView(
                                difficulty = 2,
                                totalQuestions = 2,
                                onSuccess = { testingMission = null }
                            )
                        }
                        MissionType.SHAKE -> {
                            ShakeMissionView(
                                targetShakes = 15,
                                sensitivityLevel = 2,
                                onSuccess = { testingMission = null }
                            )
                        }
                        MissionType.MEMORY_MATRIX -> {
                            MemoryMatrixMissionView(
                                gridDimension = 3,
                                totalRounds = 2,
                                onSuccess = { testingMission = null }
                            )
                        }
                        MissionType.TYPING -> {
                            TypingMissionView(
                                targetQuote = "I am awake, energized, and ready for today!",
                                onSuccess = { testingMission = null }
                            )
                        }
                        MissionType.STEPS -> {
                            WalkStepsMissionView(
                                targetCount = 10,
                                onSuccess = { testingMission = null }
                            )
                        }
                        MissionType.SQUAT -> {
                            SquatExerciseMissionView(
                                targetCount = 5,
                                onSuccess = { testingMission = null }
                            )
                        }
                        MissionType.NONE -> {
                            testingMission = null
                        }
                    }

                    // Functional "Exit Preview" / "Close" Button (Pinned Top-End)
                    Surface(
                        color = DarkSurface.copy(alpha = 0.9f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBackground),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .size(36.dp)
                    ) {
                        IconButton(
                            onClick = { testingMission = null },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Preview",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSoundLibrary) {
        SoundPickerSheet(
            currentSound = previewSoundName,
            onSoundSelected = { sound ->
                previewSoundName = sound
                showSoundLibrary = false
            },
            onDismiss = { showSoundLibrary = false }
        )
    }
}

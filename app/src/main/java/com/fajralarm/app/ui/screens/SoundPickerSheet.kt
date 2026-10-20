package com.fajralarm.app.ui.screens

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fajralarm.app.ui.theme.*

data class SoundItem(val resName: String, val displayName: String, val category: String)

@Composable
fun SoundPickerSheet(
    currentSound: String,
    onSoundSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingRes by remember { mutableStateOf<String?>(null) }
    var selectedSound by remember { mutableStateOf(currentSound) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            previewPlayer = null
        }
    }

    val sounds = listOf(
        SoundItem("siren", "Alarmy Loud Siren", "Extreme / Loud"),
        SoundItem("loud1", "Super Loud 1", "Extreme / Loud"),
        SoundItem("loud2", "Super Loud 2", "Extreme / Loud"),
        SoundItem("loud3", "Super Loud 3", "Extreme / Loud"),
        SoundItem("loud4", "Super Loud 4", "Extreme / Loud"),
        SoundItem("loud5", "Super Loud 5", "Extreme / Loud"),
        SoundItem("loud6", "Super Loud 6", "Extreme / Loud"),
        SoundItem("loud7", "Super Loud 7", "Extreme / Loud"),
        SoundItem("loud8", "Super Loud 8", "Extreme / Loud"),
        SoundItem("fire_alarm", "Fire Alarm", "Extreme / Loud"),
        SoundItem("noise", "White Noise Blast", "Extreme / Loud"),
        SoundItem("digital_alarm", "Digital Alarm", "Classic"),
        SoundItem("analog_alarm", "Classic Bell Alarm", "Classic"),
        SoundItem("beep", "Urgent Beeps", "Classic"),
        SoundItem("rooster", "Morning Rooster", "Nature"),
        SoundItem("awakening", "Gentle Awakening", "Gentle"),
        SoundItem("sunshine", "Sunshine Melody", "Gentle"),
        SoundItem("morning_park", "Morning Park Birds", "Nature"),
        SoundItem("picnic", "Sunny Picnic", "Gentle"),
        SoundItem("orkney", "Orkney Breeze", "Gentle"),
        SoundItem("sleepsound_light_rain", "Light Rain", "Nature"),
        SoundItem("happy_claps", "Happy Rhythm", "Rhythmic")
    )

    fun togglePreview(resName: String) {
        if (currentlyPlayingRes == resName) {
            previewPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            previewPlayer = null
            currentlyPlayingRes = null
        } else {
            previewPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            if (resId != 0) {
                val player = MediaPlayer.create(context, resId)
                player?.isLooping = true
                player?.start()
                previewPlayer = player
                currentlyPlayingRes = resName
            }
        }
    }

    fun dismissSheet() {
        previewPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        previewPlayer = null
        currentlyPlayingRes = null
        onDismiss()
    }

    Dialog(onDismissRequest = { dismissSheet() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = VividCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ringtone",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                    IconButton(onClick = { dismissSheet() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Tap any ringtone to preview & listen instantly",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Sound List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sounds) { item ->
                        val isSelected = selectedSound == item.resName
                        val isPlaying = currentlyPlayingRes == item.resName

                        // Strict 64dp Row Height to prevent card ballooning or vertical text collapse
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isPlaying) PrimaryBlue.copy(alpha = 0.35f) else if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else CardBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPlaying) VividCyan else if (isSelected) ElectricBlue else CardBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedSound = item.resName
                                    togglePreview(item.resName)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Radio Selector (24px)
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSound = item.resName
                                        togglePreview(item.resName)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = VividCyan,
                                        unselectedColor = TextMuted
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )

                                // 2. Title & Subtitle Column (flex: 1, paddingHorizontal: 12dp)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = item.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isPlaying) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPlaying) VividCyan else TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.category,
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // 3. Action Pill / Play Button (flexShrink: 0)
                                if (isPlaying) {
                                    // Unified horizontal PLAYING pill with circular Stop icon
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = VividCyan.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, VividCyan),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { togglePreview(item.resName) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "PLAYING",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VividCyan,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(VividCyan),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Stop,
                                                    contentDescription = "Stop",
                                                    tint = PitchBlack,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Standard Circular Play Button
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurface)
                                            .clickable {
                                                selectedSound = item.resName
                                                togglePreview(item.resName)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Preview",
                                            tint = VividCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { dismissSheet() }) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            previewPlayer?.let {
                                if (it.isPlaying) it.stop()
                                it.release()
                            }
                            previewPlayer = null
                            onSoundSelected(selectedSound)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

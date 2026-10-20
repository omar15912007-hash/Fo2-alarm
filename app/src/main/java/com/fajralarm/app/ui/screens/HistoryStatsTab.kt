package com.fajralarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fajralarm.app.data.AlarmHistoryRecord
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryStatsTab(repository: AlarmRepository) {
    var history by remember { mutableStateOf(repository.getAllHistory()) }

    val totalAlarms = history.size
    val successfulAlarms = history.count { it.isSuccess && !it.wasSnoozed }
    val successRate = if (totalAlarms > 0) ((successfulAlarms.toFloat() / totalAlarms.toFloat()) * 100).toInt() else 100

    val avgSolveSeconds = if (successfulAlarms > 0) {
        history.filter { it.isSuccess }.map { it.timeToSolveSeconds }.average().toInt()
    } else 28

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "History & Stats",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Your wake-up consistency & logs",
                        fontSize = 12.sp,
                        color = VividCyan
                    )
                }
            }

            if (history.isNotEmpty()) {
                IconButton(onClick = {
                    repository.clearHistory()
                    history = emptyList()
                }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TextMuted)
                }
            }
        }

        // Stats Cards Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Success Rate Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Success Rate", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$successRate%",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = VividCyan
                    )
                    Text("On-time wakeups", fontSize = 10.sp, color = EmeraldGreen)
                }
            }

            // Avg Wake-up Time Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Avg Solve Speed", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${avgSolveSeconds}s",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite
                    )
                    Text("Fast responsiveness", fontSize = 10.sp, color = TextMuted)
                }
            }

            // Streak Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Active Streak", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔥 7d",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = AmberWarning
                    )
                    Text("Days in a row", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        // Logs Header
        Text(
            text = "Alarm Logs (${history.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SoftBlue,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No alarm logs recorded yet.",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "Completed missions will appear here.",
                        fontSize = 12.sp,
                        color = TextMuted.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (record.isSuccess) EmeraldGreen.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (record.isSuccess) Icons.Default.Check else Icons.Default.Snooze,
                                        contentDescription = null,
                                        tint = if (record.isSuccess) EmeraldGreen else AmberWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = record.alarmLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = sdf.format(Date(record.timestamp)),
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (record.isSuccess) PrimaryBlue else CardBackground
                                ) {
                                    Text(
                                        text = record.missionType.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (record.wasSnoozed) "Snoozed" else "Solved in ${record.timeToSolveSeconds}s",
                                    fontSize = 11.sp,
                                    color = if (record.isSuccess) EmeraldGreen else AmberWarning
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

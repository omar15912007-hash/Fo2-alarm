package com.fajralarm.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import com.fajralarm.app.data.AlarmModel
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.receiver.AlarmScheduler
import com.fajralarm.app.service.AlarmService
import com.fajralarm.app.ui.components.TwinBellAlarmClockLogo
import com.fajralarm.app.ui.ringing.AlarmRingingActivity
import com.fajralarm.app.ui.screens.*
import com.fajralarm.app.ui.theme.*
import com.fajralarm.app.updater.AppUpdateManager
import com.fajralarm.app.updater.GlassmorphicUpdateDialog
import com.fajralarm.app.updater.UpdateManifest
import com.fajralarm.app.updater.UpdateState

enum class MainTab(val title: String, val icon: ImageVector) {
    ALARMS("Alarms", Icons.Default.AccessTime),
    MISSIONS("Missions", Icons.Default.Extension),
    HISTORY("History", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

private sealed class AppScreenState {
    object Main : AppScreenState()
    object AddAlarm : AppScreenState()
    data class EditAlarm(val alarm: AlarmModel) : AppScreenState()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for background alarms on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val repository = AlarmRepository(this)

        setContent {
            FajrAlarmTheme {
                var alarms by remember { mutableStateOf(repository.getAllAlarms()) }
                var currentTab by remember { mutableStateOf(MainTab.ALARMS) }
                var activeScreen by remember { mutableStateOf<AppScreenState>(AppScreenState.Main) }
                var startupUpdateManifest by remember { mutableStateOf<UpdateManifest?>(null) }

                // Non-intrusive OTA update check on app launch
                LaunchedEffect(Unit) {
                    val updateResult = AppUpdateManager.checkUpdate()
                    if (updateResult is UpdateState.UpdateAvailable) {
                        startupUpdateManifest = updateResult.manifest
                    }
                }

                // Fluid AnimatedContent Screen Switching with Spring Physics
                AnimatedContent(
                    targetState = activeScreen,
                    transitionSpec = {
                        if (targetState is AppScreenState.Main) {
                            (slideInVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
                                initialOffsetY = { -it / 4 }
                            ) + fadeIn()).togetherWith(
                                slideOutVertically(
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
                                    targetOffsetY = { it }
                                ) + fadeOut()
                            )
                        } else {
                            (slideInVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                initialOffsetY = { it }
                            ) + fadeIn()).togetherWith(
                                slideOutVertically(
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
                                    targetOffsetY = { -it / 4 }
                                ) + fadeOut()
                            )
                        }
                    },
                    label = "screenTransition"
                ) { screenState ->
                    when (screenState) {
                        is AppScreenState.AddAlarm -> {
                            AlarmEditorScreen(
                                onDismiss = { activeScreen = AppScreenState.Main },
                                onSave = { newAlarm ->
                                    repository.saveOrUpdateAlarm(newAlarm)
                                    AlarmScheduler.scheduleAlarm(this@MainActivity, newAlarm)
                                    alarms = repository.getAllAlarms()
                                    activeScreen = AppScreenState.Main
                                }
                            )
                        }

                        is AppScreenState.EditAlarm -> {
                            AlarmEditorScreen(
                                initialAlarm = screenState.alarm,
                                onDismiss = { activeScreen = AppScreenState.Main },
                                onSave = { updated ->
                                    repository.saveOrUpdateAlarm(updated)
                                    if (updated.isEnabled) {
                                        AlarmScheduler.scheduleAlarm(this@MainActivity, updated)
                                    } else {
                                        AlarmScheduler.cancelAlarm(this@MainActivity, updated.id)
                                    }
                                    alarms = repository.getAllAlarms()
                                    activeScreen = AppScreenState.Main
                                },
                                onDelete = {
                                    AlarmScheduler.cancelAlarm(this@MainActivity, screenState.alarm.id)
                                    repository.deleteAlarm(screenState.alarm.id)
                                    alarms = repository.getAllAlarms()
                                    activeScreen = AppScreenState.Main
                                }
                            )
                        }

                        is AppScreenState.Main -> {
                            Scaffold(
                                topBar = {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DeepObsidian)
                                            .statusBarsPadding()
                                            .height(64.dp)
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Custom Vector Twin-Bell Logo
                                            TwinBellAlarmClockLogo(
                                                size = 38.dp,
                                                isRinging = false
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Fo2",
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = TextWhite
                                                )
                                                Text(
                                                    text = "Fo2 men el noom!",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CyanGlow
                                                )
                                            }
                                        }
                                    }
                                },
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = GlassCardStart,
                                        contentColor = TextWhite
                                    ) {
                                        MainTab.values().forEach { tab ->
                                            val isSelected = currentTab == tab
                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = { currentTab = tab },
                                                icon = {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = tab.title,
                                                        tint = if (isSelected) CyanGlow else TextMuted
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = tab.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) CyanGlow else TextMuted,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    indicatorColor = ElectricBlue.copy(alpha = 0.25f)
                                                )
                                            )
                                        }
                                    }
                                },
                                floatingActionButton = {
                                    if (currentTab == MainTab.ALARMS) {
                                        FloatingActionButton(
                                            onClick = { activeScreen = AppScreenState.AddAlarm },
                                            containerColor = ElectricBlue,
                                            contentColor = TextWhite,
                                            shape = CircleShape,
                                            modifier = Modifier.bouncyClickable {
                                                activeScreen = AppScreenState.AddAlarm
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Alarm")
                                        }
                                    }
                                },
                                containerColor = DeepObsidian
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    // Fluid Animated Tab Switching
                                    AnimatedContent(
                                        targetState = currentTab,
                                        transitionSpec = {
                                            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                                            (slideInHorizontally(
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                initialOffsetX = { fullWidth -> direction * fullWidth }
                                            ) + fadeIn()).togetherWith(
                                                slideOutHorizontally(
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
                                                    targetOffsetX = { fullWidth -> -direction * fullWidth }
                                                ) + fadeOut()
                                            )
                                        },
                                        label = "tabTransition"
                                    ) { tab ->
                                        when (tab) {
                                            MainTab.ALARMS -> {
                                                AlarmsTabContent(
                                                    alarms = alarms,
                                                    onAlarmClick = { activeScreen = AppScreenState.EditAlarm(it) },
                                                    onToggle = { alarm, isEnabled ->
                                                        val updated = alarm.copy(isEnabled = isEnabled)
                                                        repository.saveOrUpdateAlarm(updated)
                                                        if (isEnabled) {
                                                            AlarmScheduler.scheduleAlarm(this@MainActivity, updated)
                                                        } else {
                                                            AlarmScheduler.cancelAlarm(this@MainActivity, updated.id)
                                                        }
                                                        alarms = repository.getAllAlarms()
                                                    },
                                                    onDelete = { alarm ->
                                                        AlarmScheduler.cancelAlarm(this@MainActivity, alarm.id)
                                                        repository.deleteAlarm(alarm.id)
                                                        alarms = repository.getAllAlarms()
                                                    },
                                                    onTest = { testAlarmNow(alarms.firstOrNull()) }
                                                )
                                            }

                                            MainTab.MISSIONS -> {
                                                MissionsPreviewTab()
                                            }

                                            MainTab.HISTORY -> {
                                                HistoryStatsTab(repository = repository)
                                            }

                                            MainTab.SETTINGS -> {
                                                GlobalSettingsTab(
                                                    repository = repository,
                                                    onTestShield = { testAlarmNow(alarms.firstOrNull()) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Glassmorphic OTA Update Dialog on Startup if available
                if (startupUpdateManifest != null) {
                    GlassmorphicUpdateDialog(
                        manifest = startupUpdateManifest!!,
                        onDismiss = { startupUpdateManifest = null }
                    )
                }
            }
        }
    }

    private fun testAlarmNow(alarm: AlarmModel?) {
        val target = alarm ?: AlarmModel(hour = 7, minute = 0)
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            putExtra("EXTRA_ALARM_ID", target.id)
            putExtra("EXTRA_SOUND_RES", target.soundResName)
            putExtra("EXTRA_LABEL", target.label)
            putExtra("EXTRA_GENTLE_MINUTES", target.gentleWakeUpMinutes)
            putExtra("EXTRA_VIBRATE", target.isVibrate)
        }
        startService(serviceIntent)

        val ringIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra("EXTRA_ALARM_ID", target.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(ringIntent)
    }
}

@Composable
fun AlarmsTabContent(
    alarms: List<AlarmModel>,
    onAlarmClick: (AlarmModel) -> Unit,
    onToggle: (AlarmModel, Boolean) -> Unit,
    onDelete: (AlarmModel) -> Unit,
    onTest: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Next Alarm Dynamic Banner
        val enabledAlarms = alarms.filter { it.isEnabled }
        val nextAlarmText = if (enabledAlarms.isNotEmpty()) {
            val first = enabledAlarms.first()
            formatAlarmRemainingTime(first.hour, first.minute, first.daysOfWeek)
        } else {
            "No active alarms scheduled"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .glassCard(shape = RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Next Alarm",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = nextAlarmText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                }
            }
        }

        Text(
            text = "Scheduled Alarms (${alarms.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = SoftBlue,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmItemCard(
                    alarm = alarm,
                    onClick = { onAlarmClick(alarm) },
                    onToggle = { isChecked -> onToggle(alarm, isChecked) },
                    onDelete = { onDelete(alarm) }
                )
            }
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: AlarmModel,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isPm = alarm.hour >= 12
    val hour12 = when {
        alarm.hour == 0 -> 12
        alarm.hour > 12 -> alarm.hour - 12
        else -> alarm.hour
    }
    val amPmString = if (isPm) "PM" else "AM"
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(
                shape = RoundedCornerShape(20.dp),
                borderGradient = if (alarm.isEnabled) {
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
                            ElectricBlue.copy(alpha = 0.45f)
                        )
                    )
                } else GlassBorderGradient
            )
            .bouncyClickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%02d:%02d", hour12, alarm.minute),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (alarm.isEnabled) TextWhite else TextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amPmString,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) CyanGlow else TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alarm.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (alarm.isEnabled) TextWhite else TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (alarm.isEnabled) ElectricBlue.copy(alpha = 0.25f) else CardBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = if (alarm.isEnabled) androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.35f)) else null
                    ) {
                        Text(
                            text = alarm.missionType.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) CyanGlow else TextWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Active days pills row (S M T W T F S)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayLabels.forEachIndexed { idx, dayName ->
                        val dayNum = idx + 1
                        val isActive = alarm.daysOfWeek.contains(dayNum)
                        Surface(
                            color = if (isActive && alarm.isEnabled) CyanGlow.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent,
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !alarm.isEnabled -> TextMuted.copy(alpha = 0.4f)
                                        isActive -> CyanGlow
                                        else -> TextMuted.copy(alpha = 0.25f)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.bouncyClickable(onClick = onDelete)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = DangerRed.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = ElectricBlue,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CardBackground
                    )
                )
            }
        }
    }
}

package com.fajralarm.app.ui.screens

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fajralarm.app.BuildConfig
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.data.AppGlobalSettings
import com.fajralarm.app.receiver.AlarmDeviceAdminReceiver
import com.fajralarm.app.service.AlarmAccessibilityService
import com.fajralarm.app.ui.theme.*
import com.fajralarm.app.updater.AppUpdateManager
import com.fajralarm.app.updater.GlassmorphicUpdateDialog
import com.fajralarm.app.updater.UpdateManifest
import com.fajralarm.app.updater.UpdateState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Centralized, resilient permission verification & deep-link launcher utility.
 * Handles OEM quirks and delayed AppOp/DeviceIdle state propagation on Android 12–15+.
 */
object PermissionStatusHelper {

    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        return runCatching {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val compName = ComponentName(context, AlarmDeviceAdminReceiver::class.java)
            dpm?.isAdminActive(compName) ?: false
        }.getOrDefault(false)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return runCatching {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            val enabledList = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).orEmpty()
            val targetClass = AlarmAccessibilityService::class.java.name
            val targetShortClass = ".service.AlarmAccessibilityService"
            if (enabledList.any { info ->
                    val si = info.resolveInfo?.serviceInfo
                    si?.packageName == context.packageName &&
                        (si.name == targetClass || si.name.endsWith("AlarmAccessibilityService"))
                }
            ) {
                return@runCatching true
            }

            val colonSplitter = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val fullId1 = "${context.packageName}/$targetClass"
            val fullId2 = "${context.packageName}/$targetShortClass"
            colonSplitter.split(':').any {
                it.equals(fullId1, ignoreCase = true) || it.equals(fullId2, ignoreCase = true)
            }
        }.getOrDefault(false)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.canUseFullScreenIntent() ?: true
        } else {
            true
        }
    }

    fun requestExactAlarm(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }.onFailure {
                openAppDetails(context)
            }
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }.onFailure {
                runCatching {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                }.onFailure {
                    openAppDetails(context)
                }
            }
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }.onFailure {
                openAppDetails(context)
            }
        }
    }

    fun requestDeviceAdmin(context: Context) {
        runCatching {
            val comp = ComponentName(context, AlarmDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Prevents app uninstall during active alarms")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            openAppDetails(context)
        }
    }

    fun openAccessibilitySettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            openAppDetails(context)
        }
    }

    fun openNotificationSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            openAppDetails(context)
        }
    }

    fun requestFullScreenIntentPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }.onFailure {
                openNotificationSettings(context)
            }
        }
    }

    fun openAppDetails(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

/**
 * Multi-stage reactive refresh tick for permission states:
 * Refreshes immediately on ON_RESUME, plus delayed ticks at 250ms, 600ms, and 1200ms
 * to catch asynchronous AppOps / PowerManager commits when system permission dialogs close.
 */
@Composable
fun rememberPermissionRefreshTick(): Int {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tick++
                scope.launch {
                    delay(250)
                    tick++
                    delay(400)
                    tick++
                    delay(600)
                    tick++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            tick++
        }
    }

    return tick
}

@Composable
fun GlobalSettingsTab(
    repository: AlarmRepository,
    onTestShield: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(repository.getGlobalSettings()) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var currentTestSound by remember { mutableStateOf("siren") }

    // OTA Updater State
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateDialogManifest by remember { mutableStateOf<UpdateManifest?>(null) }
    var updateInfoBanner by remember { mutableStateOf<String?>(null) }

    fun updateSettings(newSettings: AppGlobalSettings) {
        settings = newSettings
        repository.saveGlobalSettings(newSettings)
    }

    val permissionRefreshTick = rememberPermissionRefreshTick()

    val hasExactAlarm = remember(permissionRefreshTick) {
        PermissionStatusHelper.hasExactAlarmPermission(context)
    }
    val isIgnoringBattery = remember(permissionRefreshTick) {
        PermissionStatusHelper.isIgnoringBatteryOptimizations(context)
    }
    val canDrawOverlays = remember(permissionRefreshTick) {
        PermissionStatusHelper.canDrawOverlays(context)
    }
    val isDeviceAdmin = remember(permissionRefreshTick) {
        PermissionStatusHelper.isDeviceAdminActive(context)
    }
    val isAccessibilityEnabled = remember(permissionRefreshTick) {
        PermissionStatusHelper.isAccessibilityServiceEnabled(context)
    }
    val hasNotifications = remember(permissionRefreshTick) {
        PermissionStatusHelper.hasNotificationPermission(context)
    }
    val canFullScreenIntent = remember(permissionRefreshTick) {
        PermissionStatusHelper.canUseFullScreenIntent(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Global Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Anti-Cheat Protection & System Reliability",
                    fontSize = 12.sp,
                    color = VividCyan
                )
            }
        }

        // 0. GitHub OTA Auto-Updater Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .glassCard(shape = RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GitHub OTA Updates",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ElectricBlue.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanGlow,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Instant in-place upgrades from official GitHub release",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    val checkUpdateInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            if (!isCheckingUpdates) {
                                isCheckingUpdates = true
                                updateInfoBanner = null
                                coroutineScope.launch {
                                    val result = AppUpdateManager.checkUpdate()
                                    isCheckingUpdates = false
                                    when (result) {
                                        is UpdateState.UpdateAvailable -> {
                                            updateDialogManifest = result.manifest
                                        }
                                        is UpdateState.UpToDate -> {
                                            updateInfoBanner = "App is up to date (v${BuildConfig.VERSION_NAME})!"
                                        }
                                        is UpdateState.Error -> {
                                            updateInfoBanner = "Update check: ${result.message}"
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        },
                        enabled = !isCheckingUpdates,
                        interactionSource = checkUpdateInteraction,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.bouncyPress(checkUpdateInteraction)
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TextWhite
                            )
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (updateInfoBanner != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PitchBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (updateInfoBanner?.startsWith("App is up") == true) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (updateInfoBanner?.startsWith("App is up") == true) EmeraldGreen else AmberWarning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = updateInfoBanner ?: "",
                                fontSize = 11.sp,
                                color = TextWhite
                            )
                        }
                    }
                }
            }
        }

        // Sound / Siren Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .glassCard(shape = RoundedCornerShape(18.dp))
                .clickable { showSoundPicker = true },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Preview Alarm Ringtones & Sirens",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Test and listen to all 22 alarm sounds right now",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = { showSoundPicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test", fontSize = 12.sp, color = TextWhite)
                }
            }
        }

        // 1. Anti-Cheat Engine Section
        Text(
            text = "Anti-Cheat Engine",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = VividCyan,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Volume Lock Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Prevent Volume Down (Volume Lock)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Forces STREAM_ALARM volume back to 100% every 500ms while ringing",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = settings.isVolumeLockEnabled,
                        onCheckedChange = {
                            updateSettings(settings.copy(isVolumeLockEnabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = PrimaryBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CardBackground
                        )
                    )
                }

                HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 12.dp))

                // Prevent Turn Off / Relentless Anti-Dismiss Watchdog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Prevent Turn Off / Screen Shield",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Blocks back button & runs 400ms watchdog to force alarm to top if minimized",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = settings.isPreventTurnOffEnabled,
                        onCheckedChange = {
                            updateSettings(settings.copy(isPreventTurnOffEnabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = PrimaryBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CardBackground
                        )
                    )
                }

                HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 12.dp))

                // Mute Audio During Mission Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mute Audio While Solving Mission",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Mutes siren during active interaction; resumes on timeout",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = settings.isMuteAudioDuringMissionEnabled,
                        onCheckedChange = {
                            updateSettings(settings.copy(isMuteAudioDuringMissionEnabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = PrimaryBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CardBackground
                        )
                    )
                }

                HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 12.dp))

                // Max Quick Mutes (Including Unlimited)
                Text(
                    text = "Max Quick Mutes During Mission:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = "Configures allowed emergency mutes per alarm. Unlimited allows infinite 10s pauses.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1 to "1x", 2 to "2x", 3 to "3x", 5 to "5x", -1 to "Unlimited").forEach { (mutes, label) ->
                        FilterChip(
                            selected = settings.maxQuickMutes == mutes,
                            onClick = {
                                updateSettings(settings.copy(maxQuickMutes = mutes))
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = TextWhite,
                                containerColor = CardBackground,
                                labelColor = TextMuted
                            )
                        )
                    }
                }

                HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 12.dp))

                // Mission Inactivity Time Limit
                Text(
                    text = "Mission Inactivity Time Limit:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = "If no touch is detected for X seconds, siren unmutes at 100% volume",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 20, 30, 60).forEach { sec ->
                        FilterChip(
                            selected = settings.missionTimeLimitSeconds == sec,
                            onClick = {
                                updateSettings(settings.copy(missionTimeLimitSeconds = sec))
                            },
                            label = { Text("${sec}s", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = TextWhite,
                                containerColor = CardBackground,
                                labelColor = TextMuted
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Essential Permissions Checklist
        Text(
            text = "Essential Permissions Checklist",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = VividCyan,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Exact Alarms
                PermissionCheckRow(
                    title = "Exact Alarms Schedule",
                    subtitle = "Guarantees alarms fire on the exact millisecond",
                    isGranted = hasExactAlarm,
                    onGrantClick = { PermissionStatusHelper.requestExactAlarm(context) }
                )

                HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 8.dp))

                // Notifications Permission
                PermissionCheckRow(
                    title = "Alarm & Wake-Up Notifications",
                    subtitle = "Required for alarm alerts and Wake-Up Check confirmations",
                    isGranted = hasNotifications,
                    onGrantClick = { PermissionStatusHelper.openNotificationSettings(context) }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 8.dp))

                    PermissionCheckRow(
                        title = "Full-Screen Lockscreen Alerts",
                        subtitle = "Allows alarms & wake-up checks to wake and cover the lockscreen",
                        isGranted = canFullScreenIntent,
                        onGrantClick = { PermissionStatusHelper.requestFullScreenIntentPermission(context) }
                    )
                }

                HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 8.dp))

                // Battery Optimization
                PermissionCheckRow(
                    title = "Unrestricted Battery Mode",
                    subtitle = "Prevents Android OEM task-killers from putting alarms to sleep",
                    isGranted = isIgnoringBattery,
                    onGrantClick = { PermissionStatusHelper.requestIgnoreBatteryOptimizations(context) }
                )

                HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 8.dp))

                // Draw Over Other Apps
                PermissionCheckRow(
                    title = "Display Over Other Apps (Overlay)",
                    subtitle = "Displays full-screen alarm & wake-up check immediately over any screen",
                    isGranted = canDrawOverlays,
                    onGrantClick = { PermissionStatusHelper.requestOverlayPermission(context) }
                )

                HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 8.dp))

                // Accessibility Shield Service
                PermissionCheckRow(
                    title = "Alarm Shield (Accessibility Service)",
                    subtitle = "Blocks force-stop and power-off evasion while an alarm is ringing",
                    isGranted = isAccessibilityEnabled,
                    onGrantClick = { PermissionStatusHelper.openAccessibilitySettings(context) }
                )

                HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 8.dp))

                // Device Admin
                PermissionCheckRow(
                    title = "Device Admin (Prevent Uninstall)",
                    subtitle = "Blocks uninstalling the app while an alarm is ringing",
                    isGranted = isDeviceAdmin,
                    onGrantClick = { PermissionStatusHelper.requestDeviceAdmin(context) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. 2-Step Restricted Settings & Accessibility Guide (Android 13+)
        Text(
            text = "2-Step Restricted Settings & Accessibility Guide",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = VividCyan,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isAccessibilityEnabled) EmeraldGreen.copy(alpha = 0.5f) else CardBackground
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Android 13/14/15 Sideload Protection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    if (isAccessibilityEnabled) {
                        Surface(
                            color = Color(0xFF00E676).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SHIELD ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAccessibilityEnabled) {
                        "Alarm Shield Accessibility Service is enabled and protecting your alarms!"
                    } else {
                        "If Android shows 'Restricted setting' when enabling the Accessibility Service, follow these two simple steps:"
                    },
                    fontSize = 12.sp,
                    color = if (isAccessibilityEnabled) EmeraldGreen else TextMuted
                )

                if (!isAccessibilityEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Step 1
                    OutlinedButton(
                        onClick = { PermissionStatusHelper.openAppDetails(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VividCyan)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Step 1: Open App Info (Tap 3 dots → Allow restricted settings)", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Step 2
                    OutlinedButton(
                        onClick = { PermissionStatusHelper.openAccessibilitySettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VividCyan)
                    ) {
                        Icon(Icons.Default.Accessibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Step 2: Open Accessibility Settings (Turn On Alarm Shield)", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onTestShield,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextWhite)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Alarm Shield Protection Now", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showSoundPicker) {
        SoundPickerSheet(
            currentSound = currentTestSound,
            onSoundSelected = { sound ->
                currentTestSound = sound
                showSoundPicker = false
            },
            onDismiss = { showSoundPicker = false }
        )
    }

    updateDialogManifest?.let { manifest ->
        GlassmorphicUpdateDialog(
            manifest = manifest,
            onDismiss = { updateDialogManifest = null }
        )
    }
}

@Composable
fun PermissionCheckRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF00E676) else Color(0xFFFFB300),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        if (!isGranted) {
            TextButton(
                onClick = onGrantClick,
                colors = ButtonDefaults.textButtonColors(contentColor = VividCyan)
            ) {
                Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Surface(
                color = Color(0xFF00E676).copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

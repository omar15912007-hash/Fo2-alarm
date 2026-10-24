package com.fajralarm.app.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.fajralarm.app.BuildConfig
import com.fajralarm.app.ui.theme.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Data Model for Remote GitHub OTA Update Manifest.
 */
data class UpdateManifest(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val releaseNotes: String = "Initial Flagship Release",
    val isForceUpdate: Boolean = false,
    val apkDownloadUrl: String = ""
)

/**
 * High-Level State of the OTA Updater Engine.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val manifest: UpdateManifest) : UpdateState()
    object UpToDate : UpdateState()
    data class Downloading(val progress: Float, val downloadedMb: Float, val totalMb: Float) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * Flagship GitHub OTA Auto-Updater Engine:
 * Fetches remote JSON manifest, compares version code against BuildConfig.VERSION_CODE,
 * verifies install permissions, streams APK with live progress, and triggers in-place package installer.
 */
object AppUpdateManager {

    /**
     * Configurable raw URL pointing to your repository's update_manifest.json on GitHub.
     */
    const val GITHUB_MANIFEST_RAW_URL = "https://raw.githubusercontent.com/omar15912007-hash/Fo2-alarm/main/update_manifest.json"

    /**
     * Checks whether a newer version is available on GitHub.
     */
    suspend fun checkUpdate(rawUrl: String = GITHUB_MANIFEST_RAW_URL): UpdateState = withContext(Dispatchers.IO) {
        try {
            val cacheBustedUrl = if (rawUrl.contains("?")) {
                "$rawUrl&t=${System.currentTimeMillis()}"
            } else {
                "$rawUrl?t=${System.currentTimeMillis()}"
            }
            val url = URL(cacheBustedUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }

            if (connection.responseCode in 200..299) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val manifest = Gson().fromJson(jsonString, UpdateManifest::class.java)

                if (manifest != null && manifest.latestVersionCode > BuildConfig.VERSION_CODE) {
                    UpdateState.UpdateAvailable(manifest)
                } else {
                    UpdateState.UpToDate
                }
            } else {
                UpdateState.Error("HTTP Error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            UpdateState.Error(e.localizedMessage ?: "Failed to connect to update server")
        }
    }

    /**
     * Streams the APK file from GitHub Releases with live 0%–100% progress reporting.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (progress: Float, downloadedMb: Float, totalMb: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 30000
                requestMethod = "GET"
            }

            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}"))
            }

            val totalBytes = connection.contentLengthLong
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val targetFile = File(targetDir, "alarmy_update.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (totalBytes > 0) (totalRead.toFloat() / totalBytes.toFloat()) else 0f
                        val downloadedMb = totalRead / (1024f * 1024f)
                        val totalMb = if (totalBytes > 0) (totalBytes / (1024f * 1024f)) else 0f

                        withContext(Dispatchers.Main) {
                            onProgress(progress, downloadedMb, totalMb)
                        }
                    }
                }
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates unknown source install permissions and opens the Android Package Installer.
     */
    fun triggerInstall(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(manageIntent)
                return
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}

/**
 * Sleek Glassmorphic Update Dialog with Spring Micro-Interactions,
 * Live 0%–100% Glowing Gradient Progress Bar, and In-Place Installation.
 */
@Composable
fun GlassmorphicUpdateDialog(
    manifest: UpdateManifest,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedMb by remember { mutableFloatStateOf(0f) }
    var totalMb by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    Dialog(onDismissRequest = {
        if (!isDownloading && !manifest.isForceUpdate) {
            onDismiss()
        }
    }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .glassDialog()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header with Glowing Update Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.2f))
                        .border(1.dp, CyanGlow.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Flagship Update Available",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricBlue.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "v${manifest.latestVersionName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Current: v${BuildConfig.VERSION_NAME}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Release Notes Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PitchBlack)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "What's New:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = manifest.releaseNotes.ifBlank { "Performance improvements, UI enhancements, and stability updates." },
                            fontSize = 12.sp,
                            color = TextWhite.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Download Progress Bar (0% - 100%)
                if (isDownloading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading update...",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanGlow
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GlowingGradientProgressBar(progress = downloadProgress, height = 8.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (totalMb > 0f) {
                            Text(
                                text = String.format("%.1f MB / %.1f MB", downloadedMb, totalMb),
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Error Notification
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = DangerRed,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Bottom Action Buttons with Spring Bouncy Micro-Interactions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!manifest.isForceUpdate && !isDownloading) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBackground)
                        ) {
                            Text("Later", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    val updateBtnInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            if (downloadedFile != null) {
                                AppUpdateManager.triggerInstall(context, downloadedFile!!)
                            } else {
                                isDownloading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val result = AppUpdateManager.downloadApk(
                                        context = context,
                                        downloadUrl = manifest.apkDownloadUrl,
                                        onProgress = { p, dMb, tMb ->
                                            downloadProgress = p
                                            downloadedMb = dMb
                                            totalMb = tMb
                                        }
                                    )
                                    isDownloading = false
                                    result.onSuccess { apk ->
                                        downloadedFile = apk
                                        AppUpdateManager.triggerInstall(context, apk)
                                    }.onFailure { err ->
                                        errorMessage = err.localizedMessage ?: "Download failed"
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading,
                        interactionSource = updateBtnInteraction,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .bouncyPress(updateBtnInteraction),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(
                            imageVector = if (downloadedFile != null) Icons.Default.InstallMobile else Icons.Default.Download,
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                downloadedFile != null -> "Install Now"
                                isDownloading -> "Downloading..."
                                else -> "Update Now"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                }
            }
        }
    }
}

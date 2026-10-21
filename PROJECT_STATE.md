# ⏰ Fo2 (Alarm App) - Project Architecture & State Ledger

> **Last Updated:** October 22, 2026  
> **Current Version:** `1.0.2` (`versionCode = 3`, `versionName = "1.0.2"`)  
> **Target OS:** Android 16 (API 36) | **Min SDK:** Android 12 (API 31)  
> **GitHub Repository:** `https://github.com/omar15912007-hash/Fo2-alarm`  
> **Developer:** Egyptian Developer 🇪🇬

---

## 🛠️ Local Environment & Build Paths

- **Workspace Directory:** `F:\alarmy`
- **Compiled Output APK:** `F:\alarmy\Alarm.apk` (~105.6 MB)
- **Local MinGit Tool:** `F:\mingit\cmd\git.exe`
- **Java Home:** `F:\android studio\jbr`
- **Android SDK Path:** `F:\android studio2` (Configured in `F:\alarmy\local.properties`)
- **Build Command:**
  ```cmd
  set JAVA_HOME=F:\android studio\jbr
  gradlew.bat assembleDebug
  ```

---

## 🚀 Key Modules & Architecture

1. **Service Layer (`com.fajralarm.app.service`):**
   - [`AlarmService.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/service/AlarmService.kt): Strict 3-step lifecycle teardown:
     1. Stop audio playback & release `MediaPlayer`.
     2. Release CPU `WakeLock` safely.
     3. Call `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf()`.

2. **Receiver Layer (`com.fajralarm.app.receiver`):**
   - [`AlarmReceiver.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/receiver/AlarmReceiver.kt): Exact alarm delivery via `AlarmManager.setAlarmClock`.
   - [`WakeUpCheckReceiver.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/receiver/WakeUpCheckReceiver.kt): Follow-up wake-up check defaulted to `OFF`. Shows a high-priority heads-up notification with interactive `"I'm Awake"` button before sounding the siren. Features distinct, cancellable request codes for check, confirm, and timeout.
   - [`AlarmScheduler.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/receiver/AlarmScheduler.kt): Schedules and cancels alarms with `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`.

3. **Data Layer (`com.fajralarm.app.data`):**
   - [`AlarmModel.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/data/AlarmModel.kt): Data models for alarms, missions, and preferences.
   - [`AlarmRepository.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/data/AlarmRepository.kt): Local persistence via SharedPreferences/JSON. No mock seed alarms (clean `emptyList()` for new users). Tracks `hasCompletedOnboarding`.

4. **UI Layer (`com.fajralarm.app.ui`):**
   - [`MainActivity.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/MainActivity.kt): Navigation, FTUX onboarding bottom sheet/modal, and interactive empty state (`+ Create Your First Alarm`).
   - [`AlarmEditorScreen.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/ui/screens/AlarmEditorScreen.kt): Decoupled 3D haptic wheel picker, single-line stabilized label input without typing vibrations, repeat day chips, and presets.
   - [`SoundPickerSheet.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/ui/screens/SoundPickerSheet.kt): Rigid 64dp card height with unified horizontal PLAYING pill and stop icon. Concurrency control and audio release on dismiss.
   - [`MissionsPreviewTab.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/ui/screens/MissionsPreviewTab.kt): Flexbox weights, 76dp action buttons, balanced typography, and 120dp bottom scrolling clearance.
   - [`GlobalSettingsTab.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/ui/screens/GlobalSettingsTab.kt): Anti-cheat switches, quick mutes, mission inactivity limits, and permission deep links.

5. **Updater Engine (`com.fajralarm.app.updater`):**
   - [`AppUpdateManager.kt`](file:///f:/alarmy/app/src/main/java/com/fajralarm/app/updater/AppUpdateManager.kt): GitHub OTA auto-updater checking `update_manifest.json`.
   - `update_manifest.json`: Configured for v1.0.1 download from GitHub releases.

---

## 📦 GitHub Release History

- **v1.0.0 (Archived Base):** Initial release.
- **v1.0.1 (Current Flagship):** Comprehensive QA overhaul, phantom ringing fix, FTUX, empty state UI, ringtone 64dp pill, and Android 16 optimizations.

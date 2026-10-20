# ⏰ Fo2 (Fo2 Men El Noom!) - Mission-Based Alarm App

[![Platform](https://img.shields.io/badge/Platform-Android%2016%20(API%2031--36%2B)-2979FF?logo=android&logoColor=white)](https://github.com/omar15912007-hash/Fo2-alarm)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI/UX](https://img.shields.io/badge/UI%2FUX-Deep%20Glassmorphism-00E5FF)](https://github.com/omar15912007-hash/Fo2-alarm)
[![Release](https://img.shields.io/badge/Release-v1.0.1-2979FF)](https://github.com/omar15912007-hash/Fo2-alarm/releases)
[![Made with Pride](https://img.shields.io/badge/Made%20in-Egypt%20%F0%9F%87%AA%F0%9F%87%AC-C8102E?logoColor=white)](https://github.com/omar15912007-hash/Fo2-alarm)

**Fo2** is a mission-driven, anti-cheat wake-up alarm system designed to guarantee waking up on time. Built clean-room from the ground up using **Kotlin** and modern **Jetpack Compose**.

> 🇪🇬 **Proudly Designed & Engineered by an Egyptian Developer.**

---

## ✨ Flagship Features

- 💎 **Deep Glassmorphic UI:** Deep Obsidian theme (`#07090E`), elevated glass cards, electric blue (`#2979FF`) accents, and cyan glow highlights.
- 🎛️ **3D Haptic Time Wheel Picker:** Ultra-smooth 3D perspective wheel with tabular numerals (`tnum`) and tactile haptic ticks.
- 🎯 **Wake-Up Missions:**
  - 🔢 **Math Problems:** Customizable difficulty levels and question counts.
  - 📱 **Shake to Wake:** Calibrated device shaking detection.
  - 🧩 **Memory Matrix:** Visual memory grid pattern recall.
  - 🚶 **Walk Steps:** Dedicated pedometer sensor (`TYPE_STEP_DETECTOR`) with cadence validation.
  - 🏋️ **Squat Exercises:** Accelerometer vertical phase detection with anti-cheat cooldown.
  - ✍️ **Typing Mission:** Type exact motivational wake-up affirmations.
- 🛡️ **Anti-Dismiss Protection:** Relentless foreground service, volume lock, wake-lock persistence, and device admin protection.
- 🚀 **GitHub OTA Auto-Updater:** Integrated self-updating engine that checks for new releases on GitHub, downloads APKs with live progress, and triggers installation via `FileProvider`.

---

## 📱 Tech Stack

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Target Platform:** Android 16 (Compatible with Android 12 up to Android 16+)
- **Min SDK:** 31 (Android 12.0+)
- **Architecture:** Clean Architecture + Unidirectional Data Flow (UDF)

---

## 🛠️ Open-Source Build Guide (Quickstart)

Follow these 4 simple steps to clone, build, and run the project locally on your machine:

### Step 1: Clone the Repository
Open your terminal and clone the repository:
```bash
git clone https://github.com/omar15912007-hash/Fo2-alarm.git
cd Fo2-alarm
```

### Step 2: Open in Android Studio
1. Launch **Android Studio** (Ladybug, Koala, or later).
2. Click **Open** and select the cloned `Fo2-alarm` directory.

### Step 3: Gradle Sync & JDK Configuration
1. Allow Android Studio to automatically sync project dependencies.
2. Ensure your Gradle JDK is set to **Java 11 or Java 17** via:
   `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`.

### Step 4: Build and Run
Connect your physical Android device (with USB Debugging enabled) or launch an Android Virtual Device (AVD), then:
- Click the **Run ▶️** button (or press `Shift + F10`).
- Or build the debug APK directly via command line:
  ```bash
  ./gradlew assembleDebug
  ```
  The compiled APK will be generated at:
  `app/build/outputs/apk/debug/app-debug.apk`

---

## 👨‍💻 Developer & Attribution

- **Developer:** Egyptian Developer 🇪🇬
- **Project:** Fo2 - Mission-Based Wake-Up System
- **License:** Open Source Community Edition

---

## 📥 Download APK

Get the latest release APK from [GitHub Releases](https://github.com/omar15912007-hash/Fo2-alarm/releases).

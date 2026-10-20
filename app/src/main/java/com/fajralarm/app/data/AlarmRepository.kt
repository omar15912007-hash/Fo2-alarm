package com.fajralarm.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("alarm_repo_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_ALARMS = "key_alarms_list"
        private const val KEY_HISTORY = "key_alarm_history"
        private const val KEY_SETTINGS = "key_global_settings"
        private const val KEY_ONBOARDING = "key_has_completed_onboarding"
    }

    // --- ALARMS MANAGEMENT ---

    fun getAllAlarms(): List<AlarmModel> {
        val json = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        val type = object : TypeToken<List<AlarmModel>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAlarmById(id: String): AlarmModel? {
        return getAllAlarms().firstOrNull { it.id == id }
    }

    fun saveOrUpdateAlarm(alarm: AlarmModel) {
        val current = getAllAlarms().toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            current[index] = alarm
        } else {
            current.add(alarm)
        }
        prefs.edit().putString(KEY_ALARMS, gson.toJson(current)).apply()
    }

    fun deleteAlarm(id: String) {
        val current = getAllAlarms().filter { it.id != id }
        prefs.edit().putString(KEY_ALARMS, gson.toJson(current)).apply()
    }

    // --- ONBOARDING (FTUX) ---

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING, false)
    }

    fun setCompletedOnboarding(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING, completed).apply()
    }

    // --- HISTORY & STATS MANAGEMENT ---

    fun getAllHistory(): List<AlarmHistoryRecord> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<AlarmHistoryRecord>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistoryRecord(record: AlarmHistoryRecord) {
        val current = getAllHistory().toMutableList()
        current.add(0, record) // newest first
        if (current.size > 100) current.removeLast() // keep last 100
        prefs.edit().putString(KEY_HISTORY, gson.toJson(current)).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    // --- GLOBAL SETTINGS ---

    fun getGlobalSettings(): AppGlobalSettings {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return AppGlobalSettings()
        return try {
            gson.fromJson(json, AppGlobalSettings::class.java) ?: AppGlobalSettings()
        } catch (e: Exception) {
            AppGlobalSettings()
        }
    }

    fun saveGlobalSettings(settings: AppGlobalSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }
}

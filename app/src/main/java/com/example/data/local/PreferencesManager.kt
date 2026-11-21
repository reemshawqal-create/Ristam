package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentPreset
import com.example.data.model.AlgorithmWeights
import com.example.data.model.AppPreferencesState
import com.example.data.model.CornerDensity
import com.example.data.model.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ristam_preferences")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_preset")
        val ACCENT = stringPreferencesKey("accent_preset")
        val DENSITY = stringPreferencesKey("corner_density")
        val DISTRACTION_FREE = booleanPreferencesKey("distraction_free")
        val OFFLINE_MODE = booleanPreferencesKey("permanent_offline_mode")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("optional_cloud_sync")
        val CLOUD_SYNC_NOTE = stringPreferencesKey("cloud_sync_note")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val MASTER_PASS_ENABLED = booleanPreferencesKey("master_pass_enabled")
        val MASTER_PASS_SALT = stringPreferencesKey("master_pass_salt")
        val MASTER_PASS_VERIFIER = stringPreferencesKey("master_pass_verifier")
        val LOCK_ON_LAUNCH = booleanPreferencesKey("lock_on_launch")
        val DAILY_LIMIT_MIN = intPreferencesKey("daily_limit_min")
        val PRODUCTIVITY_ALERTS = booleanPreferencesKey("productivity_alerts")
        val SYSTEM_NOTIFS = booleanPreferencesKey("system_notifs")
        val W_RECENCY = floatPreferencesKey("w_recency")
        val W_TAGS = floatPreferencesKey("w_tags")
        val W_CREATOR = floatPreferencesKey("w_creator")
        val W_VIDEO = floatPreferencesKey("w_video")
        val BLOCKED_TAGS = stringPreferencesKey("blocked_tags_csv")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches_csv")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        AppPreferencesState(
            themePreset = enumValueOrDefault(prefs[Keys.THEME], ThemePreset.AMOLED_DARK),
            accentPreset = enumValueOrDefault(prefs[Keys.ACCENT], AccentPreset.ROSE_VIOLET),
            cornerDensity = enumValueOrDefault(prefs[Keys.DENSITY], CornerDensity.STANDARD),
            distractionFreeMode = prefs[Keys.DISTRACTION_FREE] ?: false,
            permanentOfflineMode = prefs[Keys.OFFLINE_MODE] ?: true,
            optionalCloudSyncEnabled = prefs[Keys.CLOUD_SYNC_ENABLED] ?: false,
            cloudSyncProfileNote = prefs[Keys.CLOUD_SYNC_NOTE] ?: "",
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TIME] ?: 0L,
            masterPasswordEnabled = prefs[Keys.MASTER_PASS_ENABLED] ?: false,
            lockOnLaunch = prefs[Keys.LOCK_ON_LAUNCH] ?: false,
            dailyUsageLimitMinutes = prefs[Keys.DAILY_LIMIT_MIN] ?: 45,
            productivityAlertsEnabled = prefs[Keys.PRODUCTIVITY_ALERTS] ?: true,
            systemNotificationsEnabled = prefs[Keys.SYSTEM_NOTIFS] ?: false,
            algorithmWeights = AlgorithmWeights(
                recencyWeight = prefs[Keys.W_RECENCY] ?: 0.35f,
                tagAffinityWeight = prefs[Keys.W_TAGS] ?: 0.40f,
                creatorPriorityWeight = prefs[Keys.W_CREATOR] ?: 0.25f,
                videoPreferenceBoost = prefs[Keys.W_VIDEO] ?: 0.15f
            ),
            blockedTags = (prefs[Keys.BLOCKED_TAGS] ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() },
            recentSearches = (prefs[Keys.RECENT_SEARCHES] ?: "").split("|").map { it.trim() }.filter { it.isNotEmpty() }
        )
    }

    suspend fun getMasterPasswordCredentials(): Pair<String, String> {
        val prefs = context.dataStore.data.first()
        return (prefs[Keys.MASTER_PASS_SALT] ?: "") to (prefs[Keys.MASTER_PASS_VERIFIER] ?: "")
    }

    suspend fun setMasterPassword(salt: String, verifier: String, lockOnLaunch: Boolean = true) {
        context.dataStore.edit {
            it[Keys.MASTER_PASS_ENABLED] = true
            it[Keys.MASTER_PASS_SALT] = salt
            it[Keys.MASTER_PASS_VERIFIER] = verifier
            it[Keys.LOCK_ON_LAUNCH] = lockOnLaunch
        }
    }

    suspend fun disableMasterPassword() {
        context.dataStore.edit {
            it[Keys.MASTER_PASS_ENABLED] = false
            it[Keys.MASTER_PASS_SALT] = ""
            it[Keys.MASTER_PASS_VERIFIER] = ""
            it[Keys.LOCK_ON_LAUNCH] = false
        }
    }

    suspend fun setLockOnLaunch(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCK_ON_LAUNCH] = enabled }
    }

    suspend fun setTheme(preset: ThemePreset) {
        context.dataStore.edit { it[Keys.THEME] = preset.name }
    }

    suspend fun setAccent(preset: AccentPreset) {
        context.dataStore.edit { it[Keys.ACCENT] = preset.name }
    }

    suspend fun setCornerDensity(density: CornerDensity) {
        context.dataStore.edit { it[Keys.DENSITY] = density.name }
    }

    suspend fun setDistractionFree(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DISTRACTION_FREE] = enabled }
    }

    suspend fun setOfflineAndSync(permanentOffline: Boolean, optionalSync: Boolean, note: String) {
        context.dataStore.edit {
            it[Keys.OFFLINE_MODE] = permanentOffline
            it[Keys.CLOUD_SYNC_ENABLED] = optionalSync
            it[Keys.CLOUD_SYNC_NOTE] = note
            if (optionalSync) {
                it[Keys.LAST_SYNC_TIME] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setProductivitySettings(limitMinutes: Int, alertsEnabled: Boolean, systemNotifs: Boolean) {
        context.dataStore.edit {
            it[Keys.DAILY_LIMIT_MIN] = limitMinutes
            it[Keys.PRODUCTIVITY_ALERTS] = alertsEnabled
            it[Keys.SYSTEM_NOTIFS] = systemNotifs
        }
    }

    suspend fun setAlgorithmWeights(weights: AlgorithmWeights) {
        context.dataStore.edit {
            it[Keys.W_RECENCY] = weights.recencyWeight
            it[Keys.W_TAGS] = weights.tagAffinityWeight
            it[Keys.W_CREATOR] = weights.creatorPriorityWeight
            it[Keys.W_VIDEO] = weights.videoPreferenceBoost
        }
    }

    suspend fun updateBlockedTags(tags: List<String>) {
        context.dataStore.edit {
            it[Keys.BLOCKED_TAGS] = tags.distinct().joinToString(",")
        }
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.RECENT_SEARCHES] ?: "")
                .split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals(clean, ignoreCase = true) }
            val updated = (listOf(clean) + current).take(8)
            prefs[Keys.RECENT_SEARCHES] = updated.joinToString("|")
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it[Keys.RECENT_SEARCHES] = "" }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T {
        return enumValues<T>().firstOrNull { it.name == raw } ?: default
    }
}

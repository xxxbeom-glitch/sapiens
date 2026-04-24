package com.sapiens.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sapiens.app.data.model.AiSelectedModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val USER_PREFS_NAME = "sapiens_user_prefs"
private val Context.dataStore by preferencesDataStore(name = USER_PREFS_NAME)

class UserPreferencesRepository(
    private val context: Context
) {
    companion object {
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
    }

    private val themeKey = stringPreferencesKey("theme")
    private val apiSelectedModelKey = stringPreferencesKey("api_selected_model")
    private val lastRestoredCloudBackupMsKey = longPreferencesKey("last_restored_cloud_backup_ms")
    private val pushNotificationsEnabledKey = booleanPreferencesKey("push_notifications_enabled")

    val themeModeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[themeKey] ?: THEME_DARK }

    val apiSelectedModelFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[apiSelectedModelKey]?.let { AiSelectedModel.normalize(it) }
                ?: AiSelectedModel.CLAUDE
        }

    /** 푸시 알림 수신 여부(마이 탭 토글). 기본값 true — 기존 동작과 맞춤. */
    val pushNotificationsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[pushNotificationsEnabledKey] ?: true }

    suspend fun setPushNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[pushNotificationsEnabledKey] = enabled
        }
    }

    suspend fun setApiSelectedModel(model: String) {
        val normalized = AiSelectedModel.normalize(model)
        context.dataStore.edit { preferences ->
            preferences[apiSelectedModelKey] = normalized
        }
    }

    suspend fun setThemeMode(themeMode: String) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = themeMode
        }
    }

    suspend fun getLastRestoredCloudBackupMs(): Long =
        context.dataStore.data.first()[lastRestoredCloudBackupMsKey] ?: 0L

    suspend fun setLastRestoredCloudBackupMs(epochMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[lastRestoredCloudBackupMsKey] = epochMs
        }
    }

    suspend fun exportSettingsSnapshot(): Map<String, Any?> {
        val prefs = context.dataStore.data.first()
        return mapOf(
            "theme" to (prefs[themeKey] ?: THEME_DARK),
            "apiSelectedModel" to AiSelectedModel.normalize(prefs[apiSelectedModelKey]),
            "pushNotificationsEnabled" to (prefs[pushNotificationsEnabledKey] ?: true),
        )
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun applyCloudSettingsSnapshot(data: Map<String, Any?>) {
        context.dataStore.edit { preferences ->
            (data["theme"] as? String)?.let { preferences[themeKey] = it }
            (data["apiSelectedModel"] as? String)?.let {
                preferences[apiSelectedModelKey] = AiSelectedModel.normalize(it)
            }
            if (data["apiSelectedModel"] == null) {
                val legacyClaude = data["apiClaudeEnabled"] as? Boolean
                val legacyGemini = data["apiGeminiEnabled"] as? Boolean
                if (legacyClaude != null || legacyGemini != null) {
                    preferences[apiSelectedModelKey] = AiSelectedModel.CLAUDE
                }
            }
            (data["pushNotificationsEnabled"] as? Boolean)?.let {
                preferences[pushNotificationsEnabledKey] = it
            }
        }
    }
}

package com.sapiens.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    private val domesticNewsCategoryKey = stringSetPreferencesKey("domestic_news_receive_categories")
    private val overseasNewsCategoryKey = stringSetPreferencesKey("overseas_news_receive_categories")
    private val themeKey = stringPreferencesKey("theme")
    private val apiClaudeEnabledKey = booleanPreferencesKey("api_claude_enabled")
    private val apiGeminiEnabledKey = booleanPreferencesKey("api_gemini_enabled")
    private val lastRestoredCloudBackupMsKey = longPreferencesKey("last_restored_cloud_backup_ms")

    val selectedDomesticNewsCategoriesFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[domesticNewsCategoryKey] ?: emptySet() }

    val selectedOverseasNewsCategoriesFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[overseasNewsCategoryKey] ?: emptySet() }

    val themeModeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[themeKey] ?: THEME_DARK }

    val apiClaudeEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[apiClaudeEnabledKey] ?: true }

    val apiGeminiEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[apiGeminiEnabledKey] ?: true }

    suspend fun toggleDomesticNewsCategory(value: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[domesticNewsCategoryKey].orEmpty().toMutableSet()
            if (!current.add(value)) {
                current.remove(value)
            }
            preferences[domesticNewsCategoryKey] = current
        }
    }

    suspend fun toggleOverseasNewsCategory(value: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[overseasNewsCategoryKey].orEmpty().toMutableSet()
            if (!current.add(value)) {
                current.remove(value)
            }
            preferences[overseasNewsCategoryKey] = current
        }
    }

    suspend fun setApiClaudeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[apiClaudeEnabledKey] = enabled
        }
    }

    suspend fun setApiGeminiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[apiGeminiEnabledKey] = enabled
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

    /** 북마크·뉴스 분야가 비었을 때만 클라우드 복원 허용하는 판단용. */
    suspend fun isLocalNewsAndPrefsEmptyForRestore(): Boolean {
        val prefs = context.dataStore.data.first()
        val domestic = prefs[domesticNewsCategoryKey].orEmpty()
        val overseas = prefs[overseasNewsCategoryKey].orEmpty()
        return domestic.isEmpty() && overseas.isEmpty()
    }

    suspend fun exportSettingsSnapshot(): Map<String, Any?> {
        val prefs = context.dataStore.data.first()
        return mapOf(
            "domesticCategories" to prefs[domesticNewsCategoryKey].orEmpty().toList(),
            "overseasCategories" to prefs[overseasNewsCategoryKey].orEmpty().toList(),
            "theme" to (prefs[themeKey] ?: THEME_DARK),
            "apiClaudeEnabled" to (prefs[apiClaudeEnabledKey] ?: true),
            "apiGeminiEnabled" to (prefs[apiGeminiEnabledKey] ?: true),
        )
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun applyCloudSettingsSnapshot(data: Map<String, Any?>) {
        context.dataStore.edit { preferences ->
            (data["domesticCategories"] as? List<*>)?.mapNotNull { it as? String }?.toSet()?.let {
                preferences[domesticNewsCategoryKey] = it
            }
            (data["overseasCategories"] as? List<*>)?.mapNotNull { it as? String }?.toSet()?.let {
                preferences[overseasNewsCategoryKey] = it
            }
            (data["theme"] as? String)?.let { preferences[themeKey] = it }
            (data["apiClaudeEnabled"] as? Boolean)?.let { preferences[apiClaudeEnabledKey] = it }
            (data["apiGeminiEnabled"] as? Boolean)?.let { preferences[apiGeminiEnabledKey] = it }
        }
    }
}

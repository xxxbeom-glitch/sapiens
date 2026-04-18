package com.sapiens.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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

    private val sectorKey = stringSetPreferencesKey("selected_sectors")
    private val morningNewsCategoryKey = stringSetPreferencesKey("selected_morning_news_categories")
    private val themeKey = stringPreferencesKey("theme")

    val selectedSectorsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[sectorKey] ?: emptySet() }

    val selectedMorningCategoriesFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[morningNewsCategoryKey] ?: emptySet() }

    val themeModeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[themeKey] ?: THEME_DARK }

    suspend fun toggleSector(value: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[sectorKey].orEmpty().toMutableSet()
            if (!current.add(value)) {
                current.remove(value)
            }
            preferences[sectorKey] = current
        }
    }

    suspend fun toggleMorningCategory(value: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[morningNewsCategoryKey].orEmpty().toMutableSet()
            if (!current.add(value)) {
                current.remove(value)
            }
            preferences[morningNewsCategoryKey] = current
        }
    }

    suspend fun setThemeMode(themeMode: String) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = themeMode
        }
    }
}

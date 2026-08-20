package com.awd.r2cloud.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.awd.r2cloud.domain.repository.AppLanguage
import com.awd.r2cloud.domain.repository.AppTheme
import com.awd.r2cloud.domain.repository.PreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class PreferenceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferenceRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _theme = MutableStateFlow(AppTheme.SYSTEM)
    override val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    override val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _isWiFiOnly = MutableStateFlow(true)
    override val isWiFiOnly: StateFlow<Boolean> = _isWiFiOnly.asStateFlow()

    private val _backupIntervalHours = MutableStateFlow(24)
    override val backupIntervalHours: StateFlow<Int> = _backupIntervalHours.asStateFlow()

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val BACKUP_INTERVAL = intPreferencesKey("backup_interval")
    }

    init {
        scope.launch {
            context.dataStore.data.collect { prefs ->
                _theme.value = AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
                _language.value = AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.name)
                _isWiFiOnly.value = prefs[Keys.WIFI_ONLY] ?: true
                _backupIntervalHours.value = prefs[Keys.BACKUP_INTERVAL] ?: 24
            }
        }
    }

    override fun setTheme(theme: AppTheme) {
        scope.launch {
            context.dataStore.edit { it[Keys.THEME] = theme.name }
        }
    }

    override fun setLanguage(language: AppLanguage) {
        scope.launch {
            context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
        }
    }

    override fun setWiFiOnly(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
        }
    }

    override fun setBackupInterval(hours: Int) {
        scope.launch {
            context.dataStore.edit { it[Keys.BACKUP_INTERVAL] = hours }
        }
    }
}

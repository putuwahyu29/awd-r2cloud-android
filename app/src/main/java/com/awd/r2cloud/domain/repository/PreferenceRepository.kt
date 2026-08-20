package com.awd.r2cloud.domain.repository

import kotlinx.coroutines.flow.StateFlow

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class AppLanguage(val code: String) {
    ENGLISH("en"), INDONESIAN("in")
}

interface PreferenceRepository {
    val theme: StateFlow<AppTheme>
    val language: StateFlow<AppLanguage>
    val isWiFiOnly: StateFlow<Boolean>
    val backupIntervalHours: StateFlow<Int>

    fun setTheme(theme: AppTheme)
    fun setLanguage(language: AppLanguage)
    fun setWiFiOnly(enabled: Boolean)
    fun setBackupInterval(hours: Int)
}

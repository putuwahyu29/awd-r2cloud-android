package com.awd.r2cloud.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.awd.r2cloud.domain.model.BackupFolder
import com.awd.r2cloud.domain.model.R2Credentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "r2_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun saveAccount(credentials: R2Credentials) {
        val accounts = getAllAccounts().toMutableList()
        val existingIndex = accounts.indexOfFirst { it.accountId == credentials.accountId }
        
        if (existingIndex != -1) {
            accounts[existingIndex] = credentials
        } else {
            accounts.add(credentials)
        }
        
        sharedPreferences.edit().apply {
            putString("accounts_json", json.encodeToString(accounts))
            if (getActiveAccountId() == null) {
                putString("active_account_id", credentials.accountId)
            }
            apply()
        }
    }

    fun getAllAccounts(): List<R2Credentials> {
        val jsonString = sharedPreferences.getString("accounts_json", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<R2Credentials>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getActiveAccount(): R2Credentials? {
        val activeId = getActiveAccountId() ?: return null
        return getAllAccounts().find { it.accountId == activeId }
    }

    fun getActiveAccountId(): String? {
        return sharedPreferences.getString("active_account_id", null)
    }

    fun setActiveAccount(accountId: String) {
        sharedPreferences.edit().putString("active_account_id", accountId).apply()
    }

    fun removeAccount(accountId: String) {
        val accounts = getAllAccounts().filterNot { it.accountId == accountId }
        sharedPreferences.edit().apply {
            putString("accounts_json", json.encodeToString(accounts))
            if (getActiveAccountId() == accountId) {
                putString("active_account_id", accounts.firstOrNull()?.accountId)
            }
            apply()
        }
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun isAppLockEnabled(): Boolean {
        return sharedPreferences.getBoolean("app_lock_enabled", false)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("app_lock_enabled", enabled).apply()
    }

    fun getBackupFolders(): List<BackupFolder> {
        val jsonString = sharedPreferences.getString("backup_folders", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<BackupFolder>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveBackupFolder(folder: BackupFolder) {
        val folders = getBackupFolders().toMutableList()
        val index = folders.indexOfFirst { it.uri == folder.uri }
        if (index != -1) {
            folders[index] = folder
        } else {
            folders.add(folder)
        }
        sharedPreferences.edit().putString("backup_folders", json.encodeToString(folders)).apply()
    }

    fun removeBackupFolder(uri: String) {
        val folders = getBackupFolders().filter { it.uri != uri }
        sharedPreferences.edit().putString("backup_folders", json.encodeToString(folders)).apply()
    }

    fun getStarredFiles(): Set<String> {
        val jsonString = sharedPreferences.getString("starred_files", null) ?: return emptySet()
        return try {
            json.decodeFromString<Set<String>>(jsonString)
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun toggleStarred(bucketName: String, fileKey: String) {
        val starred = getStarredFiles().toMutableSet()
        val uniqueKey = "$bucketName|$fileKey"
        if (starred.contains(uniqueKey)) starred.remove(uniqueKey) else starred.add(uniqueKey)
        sharedPreferences.edit().putString("starred_files", json.encodeToString(starred)).apply()
    }
}

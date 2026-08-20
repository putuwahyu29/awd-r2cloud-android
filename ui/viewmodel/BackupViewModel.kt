package com.awd.r2cloud.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awd.r2cloud.domain.model.BackupFolder
import com.awd.r2cloud.domain.repository.PreferenceRepository
import com.awd.r2cloud.domain.repository.R2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: R2Repository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    var backupFolders by mutableStateOf(repository.getBackupFolders())
        private set

    val isWiFiOnly = preferenceRepository.isWiFiOnly.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    
    val backupInterval = preferenceRepository.backupIntervalHours.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 24
    )

    fun refreshFolders() {
        backupFolders = repository.getBackupFolders()
    }

    fun addFolder(uri: String, name: String, bucketName: String) {
        val folder = BackupFolder(uri = uri, name = name, bucketName = bucketName)
        repository.saveBackupFolder(folder)
        refreshFolders()
    }

    fun toggleFolder(uri: String, isEnabled: Boolean) {
        val folder = backupFolders.find { it.uri == uri } ?: return
        repository.saveBackupFolder(folder.copy(isEnabled = isEnabled))
        refreshFolders()
    }

    fun removeFolder(uri: String) {
        repository.removeBackupFolder(uri)
        refreshFolders()
    }

    fun setWiFiOnly(enabled: Boolean) {
        preferenceRepository.setWiFiOnly(enabled)
    }

    fun setBackupInterval(hours: Int) {
        preferenceRepository.setBackupInterval(hours)
    }
}

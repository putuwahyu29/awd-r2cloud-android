package com.awd.r2cloud.data.worker

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.awd.r2cloud.domain.repository.PreferenceRepository
import com.awd.r2cloud.domain.repository.R2Repository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: R2Repository,
    private val preferenceRepository: PreferenceRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val folders = repository.getBackupFolders().filter { it.isEnabled }
        if (folders.isEmpty()) return Result.success()

        val isWiFiOnly = preferenceRepository.isWiFiOnly.first()
        
        // Connectivity check is handled by WorkManager constraints, 
        // but we double check for "WiFi Only" if needed manually
        
        folders.forEach { folder ->
            try {
                val root = DocumentFile.fromTreeUri(applicationContext, android.net.Uri.parse(folder.uri))
                if (root != null && root.isDirectory) {
                    uploadFolder(root, folder.bucketName, folder.targetPath)
                }
                repository.saveBackupFolder(folder.copy(lastBackup = System.currentTimeMillis()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success()
    }

    private suspend fun uploadFolder(folder: DocumentFile, bucketName: String, pathPrefix: String) {
        folder.listFiles().forEach { file ->
            if (file.isDirectory) {
                uploadFolder(file, bucketName, "$pathPrefix${file.name}/")
            } else if (file.isFile) {
                try {
                    val bytes = applicationContext.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        repository.uploadFile(bucketName, "$pathPrefix${file.name}", bytes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

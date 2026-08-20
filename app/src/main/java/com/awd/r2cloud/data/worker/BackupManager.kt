package com.awd.r2cloud.data.worker

import android.content.Context
import androidx.work.*
import com.awd.r2cloud.domain.repository.PreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceRepository: PreferenceRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startMonitoring() {
        scope.launch {
            combine(
                preferenceRepository.isWiFiOnly,
                preferenceRepository.backupIntervalHours
            ) { wifi, interval ->
                scheduleBackup(wifi, interval)
            }.collect {}
        }
    }

    private fun scheduleBackup(wifiOnly: Boolean, intervalHours: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(intervalHours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "r2_auto_backup",
            ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )
    }

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<BackupWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun cancelBackup() {
        WorkManager.getInstance(context).cancelUniqueWork("r2_auto_backup")
    }
}

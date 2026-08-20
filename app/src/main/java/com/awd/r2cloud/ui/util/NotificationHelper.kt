package com.awd.r2cloud.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.awd.r2cloud.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "transfer_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.transfers),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.transfer_notif_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgressNotification(id: String, fileName: String, progress: Int, isUpload: Boolean) {
        val title = if (isUpload) {
            context.getString(R.string.notif_uploading, fileName)
        } else {
            context.getString(R.string.notif_downloading, fileName)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (isUpload) android.R.drawable.stat_sys_upload else android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        try {
            notificationManager.notify(id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun showSuccessNotification(id: String, fileName: String, isUpload: Boolean) {
        val title = if (isUpload) {
            context.getString(R.string.notif_success_upload)
        } else {
            context.getString(R.string.notif_success_download)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (isUpload) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(fileName)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun showErrorNotification(id: String, fileName: String, error: String?) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.notif_failed))
            .setContentText("$fileName: ${error ?: context.getString(R.string.unknown_error)}")
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun cancelNotification(id: String) {
        notificationManager.cancel(id.hashCode())
    }
}

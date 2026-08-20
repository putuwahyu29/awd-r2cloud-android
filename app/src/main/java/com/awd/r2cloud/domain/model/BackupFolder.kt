package com.awd.r2cloud.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupFolder(
    val uri: String,
    val name: String,
    val bucketName: String,
    val targetPath: String = "backups/",
    val isEnabled: Boolean = true,
    val lastBackup: Long? = null
)

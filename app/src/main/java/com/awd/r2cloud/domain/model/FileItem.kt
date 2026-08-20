package com.awd.r2cloud.domain.model

data class FileItem(
    val key: String,
    val name: String,
    val size: Long? = null,
    val lastModified: String? = null,
    val isFolder: Boolean = false,
    val thumbnailUrl: String? = null,
    val bucketName: String? = null
)

package com.awd.r2cloud.domain.model

enum class TransferType {
    UPLOAD, DOWNLOAD
}

enum class TransferStatus {
    IN_PROGRESS, SUCCESS, FAILED
}

data class TransferItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val type: TransferType,
    val status: TransferStatus,
    val progress: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

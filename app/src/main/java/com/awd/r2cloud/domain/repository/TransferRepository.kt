package com.awd.r2cloud.domain.repository

import com.awd.r2cloud.domain.model.TransferItem
import kotlinx.coroutines.flow.StateFlow

interface TransferRepository {
    val transfers: StateFlow<List<TransferItem>>
    fun addTransfer(item: TransferItem)
    fun updateTransferStatus(id: String, status: com.awd.r2cloud.domain.model.TransferStatus, errorMessage: String? = null)
    fun updateTransferProgress(id: String, progress: Int)
    fun clearHistory()
}

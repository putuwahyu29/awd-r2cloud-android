package com.awd.r2cloud.data.repository

import com.awd.r2cloud.domain.model.TransferItem
import com.awd.r2cloud.domain.model.TransferStatus
import com.awd.r2cloud.domain.model.TransferType
import com.awd.r2cloud.domain.repository.TransferRepository
import com.awd.r2cloud.ui.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val notificationHelper: NotificationHelper
) : TransferRepository {
    private val _transfers = MutableStateFlow<List<TransferItem>>(emptyList())
    override val transfers: StateFlow<List<TransferItem>> = _transfers.asStateFlow()

    override fun addTransfer(item: TransferItem) {
        _transfers.value = listOf(item) + _transfers.value
        updateNotification(item)
    }

    override fun updateTransferStatus(id: String, status: TransferStatus, errorMessage: String?) {
        _transfers.value = _transfers.value.map {
            if (it.id == id) {
                val updated = it.copy(status = status, errorMessage = errorMessage)
                updateNotification(updated)
                updated
            } else {
                it
            }
        }
    }

    override fun updateTransferProgress(id: String, progress: Int) {
        _transfers.value = _transfers.value.map {
            if (it.id == id) {
                val updated = it.copy(progress = progress)
                notificationHelper.showProgressNotification(
                    updated.id,
                    updated.fileName,
                    progress,
                    updated.type == TransferType.UPLOAD
                )
                updated
            } else {
                it
            }
        }
    }

    private fun updateNotification(item: TransferItem) {
        when (item.status) {
            TransferStatus.IN_PROGRESS -> {
                notificationHelper.showProgressNotification(
                    item.id, 
                    item.fileName, 
                    item.progress,
                    item.type == TransferType.UPLOAD
                )
            }
            TransferStatus.SUCCESS -> {
                notificationHelper.showSuccessNotification(
                    item.id, 
                    item.fileName, 
                    item.type == TransferType.UPLOAD
                )
            }
            TransferStatus.FAILED -> {
                notificationHelper.showErrorNotification(
                    item.id, 
                    item.fileName, 
                    item.errorMessage
                )
            }
        }
    }

    override fun clearHistory() {
        _transfers.value = emptyList()
    }
}

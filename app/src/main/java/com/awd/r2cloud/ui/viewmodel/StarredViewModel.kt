package com.awd.r2cloud.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awd.r2cloud.R
import com.awd.r2cloud.domain.model.FileItem
import com.awd.r2cloud.domain.model.TransferItem
import com.awd.r2cloud.domain.model.TransferStatus
import com.awd.r2cloud.domain.model.TransferType
import com.awd.r2cloud.domain.repository.R2Repository
import com.awd.r2cloud.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

sealed interface StarredUiState {
    object Loading : StarredUiState
    data class Success(val files: List<FileItem>) : StarredUiState
    data class Error(val message: String) : StarredUiState
}

@HiltViewModel
class StarredViewModel @Inject constructor(
    private val repository: R2Repository,
    private val transferRepository: TransferRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StarredUiState>(StarredUiState.Loading)
    val uiState: StateFlow<StarredUiState> = _uiState.asStateFlow()

    var previewFileItem by mutableStateOf<FileItem?>(null)
    var previewUrl by mutableStateOf<String?>(null)
    var isOperationLoading by mutableStateOf(false)

    init {
        loadStarredFiles()
    }

    fun loadStarredFiles() {
        viewModelScope.launch {
            _uiState.value = StarredUiState.Loading
            repository.getAllStarredFiles()
                .onSuccess { files ->
                    _uiState.value = StarredUiState.Success(files)
                }
                .onFailure { error ->
                    _uiState.value = StarredUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun toggleStarred(fileItem: FileItem, context: Context) {
        val bucket = fileItem.bucketName ?: return
        repository.toggleStarred(bucket, fileItem.key)
        Toast.makeText(context, context.getString(R.string.star_removed), Toast.LENGTH_SHORT).show()
        loadStarredFiles()
    }

    fun previewFile(fileItem: FileItem) {
        val bucket = fileItem.bucketName ?: return
        viewModelScope.launch {
            isOperationLoading = true
            repository.generatePresignedUrl(bucket, fileItem.key)
                .onSuccess { url ->
                    previewFileItem = fileItem
                    previewUrl = url
                }
            isOperationLoading = false
        }
    }

    fun downloadFile(fileItem: FileItem, context: Context) {
        val bucket = fileItem.bucketName ?: return
        viewModelScope.launch {
            val transferId = UUID.randomUUID().toString()
            transferRepository.addTransfer(
                TransferItem(
                    id = transferId,
                    fileName = fileItem.name,
                    type = TransferType.DOWNLOAD,
                    status = TransferStatus.IN_PROGRESS
                )
            )

            repository.getObject(
                bucketName = bucket,
                key = fileItem.key,
                onProgress = { progress ->
                    transferRepository.updateTransferProgress(transferId, progress)
                }
            ).onSuccess { bytes ->
                saveFileToDownloads(fileItem.name, bytes, context)
                transferRepository.updateTransferStatus(transferId, TransferStatus.SUCCESS)
                Toast.makeText(context, "Unduhan selesai: ${fileItem.name}", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                transferRepository.updateTransferStatus(transferId, TransferStatus.FAILED, error.message)
                Toast.makeText(context, "Unduhan gagal: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveFileToDownloads(fileName: String, bytes: ByteArray, context: Context) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            file.writeBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateShareLink(file: FileItem, onResult: (String) -> Unit) {
        val bucket = file.bucketName ?: return
        viewModelScope.launch {
            repository.generatePresignedUrl(bucket, file.key)
                .onSuccess { onResult(it) }
        }
    }

    fun deleteFile(fileItem: FileItem, context: Context) {
        val bucket = fileItem.bucketName ?: return
        viewModelScope.launch {
            isOperationLoading = true
            repository.deleteObject(bucket, fileItem.key)
                .onSuccess {
                    Toast.makeText(context, context.getString(R.string.file_deleted), Toast.LENGTH_SHORT).show()
                    loadStarredFiles()
                }
            isOperationLoading = false
        }
    }

    fun dismissPreview() {
        previewFileItem = null
        previewUrl = null
    }
}

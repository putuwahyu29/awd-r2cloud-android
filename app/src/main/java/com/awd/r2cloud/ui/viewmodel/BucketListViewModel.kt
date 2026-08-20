package com.awd.r2cloud.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awd.r2cloud.R
import com.awd.r2cloud.domain.model.Bucket
import com.awd.r2cloud.domain.model.FileItem
import com.awd.r2cloud.domain.model.TransferItem
import com.awd.r2cloud.domain.model.TransferStatus
import com.awd.r2cloud.domain.model.TransferType
import com.awd.r2cloud.domain.repository.R2Repository
import com.awd.r2cloud.domain.repository.TransferRepository
import com.awd.r2cloud.ui.screen.dashboard.PieChartData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Stack
import javax.inject.Inject

sealed interface BucketListUiState {
    object Loading : BucketListUiState
    data class Success(val buckets: List<Bucket>) : BucketListUiState
    data class Error(val message: String) : BucketListUiState
}

@HiltViewModel
class BucketListViewModel @Inject constructor(
    private val repository: R2Repository,
    private val transferRepository: TransferRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var uiState: BucketListUiState by mutableStateOf(BucketListUiState.Loading)
        private set

    var isCreatingBucket by mutableStateOf(false)
        private set

    var totalStorageSize by mutableLongStateOf(0L)
        private set

    var classAOperations by mutableLongStateOf(0L)
        private set

    var classBOperations by mutableLongStateOf(0L)
        private set

    var storageDistribution by mutableStateOf(emptyList<PieChartData>())
        private set

    val storageLimit = 10L * 1024 * 1024 * 1024 // 10 GB Account-Wide Limit
    val classALimit = 1_000_000L
    val classBLimit = 10_000_000L

    val storageUsagePercentage: Float
        get() = if (totalStorageSize > 0) (totalStorageSize.toFloat() / storageLimit.toFloat()).coerceIn(0f, 1f) else 0f

    val classAUsagePercentage: Float
        get() = (classAOperations.toFloat() / classALimit.toFloat()).coerceIn(0f, 1f)

    val classBUsagePercentage: Float
        get() = (classBOperations.toFloat() / classBLimit.toFloat()).coerceIn(0f, 1f)

    var isPoolMode by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    // Global Search & Filtering
    var selectedCategory by mutableStateOf<String?>(null)
    var globalSearchResults by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var isSearchingGlobal by mutableStateOf(false)
        private set

    // Recents & Starred
    var recentFiles by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var starredFileKeys by mutableStateOf(repository.getStarredFiles())
        private set

    init {
        getBuckets()
        loadRecentFiles()
    }

    fun loadRecentFiles() {
        viewModelScope.launch {
            repository.getRecentFiles(15).onSuccess {
                recentFiles = it
            }
        }
    }

    fun getBuckets() {
        viewModelScope.launch {
            uiState = BucketListUiState.Loading
            
            repository.getAccountUsage().onSuccess { usage ->
                classAOperations = usage.classAOperations
                classBOperations = usage.classBOperations
            }

            repository.getBuckets()
                .onSuccess { baseBuckets ->
                    val enrichedBuckets = baseBuckets.map { bucket ->
                        async {
                            val health = repository.checkBucketHealth(bucket.name).getOrDefault(true)
                            val nativeUsage = repository.getNativeUsage(bucket.name)
                            val managedDomain = repository.getManagedDomain(bucket.name).getOrNull()
                            
                            val updatedBucket = if (nativeUsage.isSuccess) {
                                bucket.copy(
                                    size = nativeUsage.getOrThrow(),
                                    isOnline = health,
                                    verifiedViaApi = true
                                )
                            } else {
                                // Recursive scan if API token fails or not provided
                                val allFiles = repository.listObjectsRecursive(bucket.name).getOrDefault(emptyList())
                                bucket.copy(
                                    size = allFiles.sumOf { it.size ?: 0L },
                                    isOnline = health,
                                    verifiedViaApi = false
                                )
                            }
                            
                            updatedBucket.copy(
                                isPublic = managedDomain?.first ?: false,
                                publicDomain = managedDomain?.second
                            )
                        }
                    }.awaitAll()
                    
                    uiState = BucketListUiState.Success(enrichedBuckets)
                    updateTotalsRecursive(enrichedBuckets)
                }
                .onFailure { error ->
                    val message = when (error) {
                        is java.net.UnknownHostException -> context.getString(R.string.error_no_internet)
                        is java.net.SocketTimeoutException -> context.getString(R.string.error_timeout)
                        else -> error.message ?: context.getString(R.string.unknown_error)
                    }
                    uiState = BucketListUiState.Error(message)
                }
            starredFileKeys = repository.getStarredFiles()
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        if (query.length >= 2 || selectedCategory != null) {
            performGlobalSearch()
        } else {
            isSearchingGlobal = false
            globalSearchResults = emptyList()
        }
    }

    fun onCategorySelect(category: String?) {
        selectedCategory = if (selectedCategory == category) null else category
        performGlobalSearch()
    }

    val repositoryForDialog: R2Repository get() = repository

    fun toggleStarred(file: FileItem) {
        val bucket = file.bucketName ?: return
        repository.toggleStarred(bucket, file.key)
        starredFileKeys = repository.getStarredFiles()
    }

    fun deleteFile(file: FileItem) {
        viewModelScope.launch {
            val bucket = file.bucketName ?: return@launch
            repository.deleteObject(bucket, file.key).onSuccess {
                // Locally remove from lists to be responsive
                globalSearchResults = globalSearchResults.filter { it.key != file.key }
                recentFiles = recentFiles.filter { it.key != file.key }
                getBuckets() // Refresh totals
            }
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
            }.onFailure { error ->
                transferRepository.updateTransferStatus(transferId, TransferStatus.FAILED, error.message)
            }
        }
    }

    private fun saveFileToDownloads(fileName: String, bytes: ByteArray, context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(bytes)
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                file.writeBytes(bytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyFile(fileItem: FileItem, destBucket: String, destKey: String) {
        viewModelScope.launch {
            val sourceBucket = fileItem.bucketName ?: return@launch
            repository.copyObject(sourceBucket, fileItem.key, destBucket, destKey)
                .onSuccess {
                    loadRecentFiles()
                    getBuckets()
                }
        }
    }

    fun moveFile(fileItem: FileItem, destBucket: String, destKey: String) {
        viewModelScope.launch {
            val sourceBucket = fileItem.bucketName ?: return@launch
            repository.moveObject(sourceBucket, fileItem.key, destBucket, destKey)
                .onSuccess {
                    loadRecentFiles()
                    getBuckets()
                    // Remove from local search if it was there
                    globalSearchResults = globalSearchResults.filter { it.key != fileItem.key }
                }
        }
    }

    private fun performGlobalSearch() {
        if (searchQuery.isEmpty() && selectedCategory == null) {
            isSearchingGlobal = false
            return
        }

        viewModelScope.launch {
            isSearchingGlobal = true
            val currentState = uiState
            if (currentState is BucketListUiState.Success) {
                val results = mutableListOf<FileItem>()
                currentState.buckets.forEach { bucket ->
                    repository.listObjectsRecursive(bucket.name).onSuccess { files ->
                        val filtered = files.filter { file ->
                            val matchesQuery = if (searchQuery.isNotBlank()) {
                                file.name.contains(searchQuery, ignoreCase = true)
                            } else true
                            
                            val matchesCategory = if (selectedCategory != null) {
                                getFileCategory(file.name) == selectedCategory
                            } else true
                            
                            matchesQuery && matchesCategory
                        }
                        results.addAll(filtered)
                    }
                }
                globalSearchResults = results
            }
        }
    }

    private fun getFileCategory(fileName: String): String {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif" -> "images"
            "mp4", "mkv", "mov", "webm", "avi", "flv", "3gp" -> "videos"
            "pdf", "txt", "log", "md", "json", "docx", "doc", "xls", "xlsx", "ppt", "pptx" -> "documents"
            else -> "others"
        }
    }

    private fun updateTotalsRecursive(buckets: List<Bucket>) {
        viewModelScope.launch {
            totalStorageSize = buckets.sumOf { it.size }
            
            var imgSize = 0L
            var vidSize = 0L
            var docSize = 0L
            var otherSize = 0L

            buckets.forEach { bucket ->
                repository.listObjectsRecursive(bucket.name).onSuccess { files ->
                    files.forEach { file ->
                        val cat = getFileCategory(file.name)
                        when (cat) {
                            "images" -> imgSize += file.size ?: 0
                            "videos" -> vidSize += file.size ?: 0
                            "documents" -> docSize += file.size ?: 0
                            else -> otherSize += file.size ?: 0
                        }
                    }
                }
            }

            storageDistribution = listOf(
                PieChartData(imgSize.toFloat(), Color(0xFF42A5F5), "images"),
                PieChartData(vidSize.toFloat(), Color(0xFFAB47BC), "videos"),
                PieChartData(docSize.toFloat(), Color(0xFF66BB6A), "documents"),
                PieChartData(otherSize.toFloat(), Color(0xFFFFA726), "others")
            )
        }
    }

    fun generatePreviewUrl(file: FileItem, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val bucket = file.bucketName ?: return@launch
            repository.generatePresignedUrl(bucket, file.key)
                .onSuccess { onResult(it) }
        }
    }

    fun togglePublicAccess(bucketName: String, enabled: Boolean, onComplete: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val result = repository.togglePublicAccess(bucketName, enabled)
            if (result.isSuccess) {
                getBuckets()
            }
            onComplete(result)
        }
    }

    fun createBucket(name: String, region: String?, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            isCreatingBucket = true
            val result = repository.createBucket(name, region)
            if (result.isSuccess) {
                getBuckets()
            }
            onComplete(result)
            isCreatingBucket = false
        }
    }

    fun deleteBucket(name: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            isCreatingBucket = true
            val result = repository.deleteBucket(name)
            if (result.isSuccess) {
                getBuckets()
            }
            onComplete(result)
            isCreatingBucket = false
        }
    }
}

package com.awd.r2cloud.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

sealed interface FileBrowserUiState {
    object Loading : FileBrowserUiState
    data class Success(val files: List<FileItem>) : FileBrowserUiState
    data class Error(val message: String) : FileBrowserUiState
}

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val repository: R2Repository,
    private val transferRepository: TransferRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val repositoryForDialog: R2Repository get() = repository

    val bucketName: String = checkNotNull(savedStateHandle["bucketName"])
    
    var uiState: FileBrowserUiState by mutableStateOf(FileBrowserUiState.Loading)
        private set

    private val prefixStack = Stack<String?>()
    var currentPrefix by mutableStateOf<String?>(null)
        private set

    var isOperationLoading by mutableStateOf(false)
        private set

    var isGridView by mutableStateOf(false)
        private set

    var searchQuery by mutableStateOf("")

    // Preview state
    var previewFileItem by mutableStateOf<FileItem?>(null)
    var previewUrl by mutableStateOf<String?>(null)
    var previewFiles by mutableStateOf<List<FileItem>>(emptyList())
    var previewFileIndex by mutableIntStateOf(0)

    // Public link support
    var publicDomain by mutableStateOf<String?>(null)
        private set

    // Multi-select state
    var selectedFiles by mutableStateOf(setOf<FileItem>())
        private set
    
    var isMultiSelectMode by mutableStateOf(false)
        private set

    // Starred support
    var starredFileKeys by mutableStateOf(repository.getStarredFiles())
        private set

    // Sorting state
    var sortOrder by mutableStateOf(SortOrder.NAME_ASC)
        private set

    enum class SortOrder {
        NAME_ASC, NAME_DESC, SIZE_ASC, SIZE_DESC, DATE_ASC, DATE_DESC
    }

    fun updateSortOrder(order: SortOrder) {
        sortOrder = order
        val currentState = uiState
        if (currentState is FileBrowserUiState.Success) {
            uiState = FileBrowserUiState.Success(sortFiles(currentState.files))
        }
    }

    init {
        loadFiles()
        checkPublicStatus()
    }

    private fun checkPublicStatus() {
        viewModelScope.launch {
            repository.getManagedDomain(bucketName).onSuccess { (enabled, domain) ->
                if (enabled) publicDomain = domain
            }
        }
    }

    fun loadFiles() {
        viewModelScope.launch {
            uiState = FileBrowserUiState.Loading
            repository.listObjects(bucketName, currentPrefix)
                .onSuccess { files ->
                    uiState = FileBrowserUiState.Success(sortFiles(files))
                    starredFileKeys = repository.getStarredFiles()
                }
                .onFailure { error ->
                    uiState = FileBrowserUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    private fun sortFiles(files: List<FileItem>): List<FileItem> {
        return when (sortOrder) {
            SortOrder.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            SortOrder.SIZE_ASC -> files.sortedBy { it.size ?: 0L }
            SortOrder.SIZE_DESC -> files.sortedByDescending { it.size ?: 0L }
            SortOrder.DATE_ASC -> files.sortedBy { it.lastModified ?: "" }
            SortOrder.DATE_DESC -> files.sortedByDescending { it.lastModified ?: "" }
        }
    }

    fun toggleStarred(fileItem: FileItem, context: Context) {
        repository.toggleStarred(bucketName, fileItem.key)
        starredFileKeys = repository.getStarredFiles()
        val isNowStarred = starredFileKeys.contains("${bucketName}|${fileItem.key}")
        val msgId = if (isNowStarred) R.string.star_added else R.string.star_removed
        Toast.makeText(context, context.getString(msgId), Toast.LENGTH_SHORT).show()
    }

    fun navigateToFolder(prefix: String) {
        if (isMultiSelectMode) return
        prefixStack.push(currentPrefix)
        currentPrefix = prefix
        loadFiles()
    }

    fun navigateBack(): Boolean {
        if (isMultiSelectMode) {
            exitMultiSelect()
            return true
        }
        return if (!prefixStack.isEmpty()) {
            currentPrefix = prefixStack.pop()
            loadFiles()
            true
        } else {
            false
        }
    }

    fun toggleLayout() {
        isGridView = !isGridView
    }

    fun toggleSelection(file: FileItem) {
        if (!isMultiSelectMode) {
            isMultiSelectMode = true
        }
        selectedFiles = if (selectedFiles.contains(file)) {
            selectedFiles - file
        } else {
            selectedFiles + file
        }
        if (selectedFiles.isEmpty()) {
            isMultiSelectMode = false
        }
    }

    fun exitMultiSelect() {
        isMultiSelectMode = false
        selectedFiles = emptySet()
    }

    fun createFolder(name: String, context: Context) {
        viewModelScope.launch {
            isOperationLoading = true
            val folderKey = if (currentPrefix != null) "${currentPrefix}$name/" else "$name/"
            repository.uploadFile(bucketName, folderKey, ByteArray(0))
                .onSuccess { 
                    loadFiles()
                    Toast.makeText(context, context.getString(R.string.folder_created), Toast.LENGTH_SHORT).show()
                }
            isOperationLoading = false
        }
    }

    fun bulkDelete(context: Context) {
        viewModelScope.launch {
            isOperationLoading = true
            val count = selectedFiles.size
            selectedFiles.forEach { file ->
                repository.deleteObject(bucketName, file.key)
            }
            exitMultiSelect()
            loadFiles()
            Toast.makeText(context, "$count item berhasil dihapus", Toast.LENGTH_SHORT).show()
            isOperationLoading = false
        }
    }

    fun bulkDownload(context: Context) {
        selectedFiles.forEach { file ->
            if (!file.isFolder) {
                downloadFile(file, context)
            }
        }
        exitMultiSelect()
        Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_SHORT).show()
    }

    fun uploadFiles(uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            uris.forEach { uri ->
                uploadSingleFile(uri, context)
            }
            loadFiles()
        }
    }

    fun uploadFolder(treeUri: Uri, context: Context) {
        viewModelScope.launch {
            val root = DocumentFile.fromTreeUri(context, treeUri)
            if (root != null && root.isDirectory) {
                uploadDocumentFolder(root, "", context)
                loadFiles()
            }
        }
    }

    private suspend fun uploadDocumentFolder(folder: DocumentFile, relativePath: String, context: Context) {
        val currentPath = if (relativePath.isEmpty()) folder.name ?: "folder" else "$relativePath/${folder.name}"
        
        val folderKey = if (currentPrefix != null) "${currentPrefix}$currentPath/" else "$currentPath/"
        repository.uploadFile(bucketName, folderKey, ByteArray(0))

        folder.listFiles().forEach { file ->
            if (file.isDirectory) {
                uploadDocumentFolder(file, currentPath, context)
            } else if (file.isFile) {
                uploadSingleFile(file.uri, context, "$currentPath/")
            }
        }
    }

    private suspend fun uploadSingleFile(uri: Uri, context: Context, pathPrefix: String = "") {
        val fileName = getFileName(uri, context)
        val transferId = UUID.randomUUID().toString()
        
        transferRepository.addTransfer(
            TransferItem(
                id = transferId,
                fileName = fileName,
                type = TransferType.UPLOAD,
                status = TransferStatus.IN_PROGRESS
            )
        )

        try {
            val fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (fileBytes != null) {
                val basePrefix = currentPrefix ?: ""
                val key = "$basePrefix$pathPrefix$fileName"
                repository.uploadFile(
                    bucketName = bucketName, 
                    key = key, 
                    fileBytes = fileBytes,
                    onProgress = { progress ->
                        transferRepository.updateTransferProgress(transferId, progress)
                    }
                ).onSuccess {
                    transferRepository.updateTransferStatus(transferId, TransferStatus.SUCCESS)
                }.onFailure { error ->
                    transferRepository.updateTransferStatus(transferId, TransferStatus.FAILED, error.message)
                }
            }
        } catch (e: Exception) {
            transferRepository.updateTransferStatus(transferId, TransferStatus.FAILED, e.message)
            e.printStackTrace()
        }
    }

    fun deleteFile(fileItem: FileItem, context: Context) {
        viewModelScope.launch {
            isOperationLoading = true
            repository.deleteObject(bucketName, fileItem.key)
                .onSuccess {
                    loadFiles()
                    Toast.makeText(context, context.getString(R.string.file_deleted), Toast.LENGTH_SHORT).show()
                }
            isOperationLoading = false
        }
    }

    fun renameFile(fileItem: FileItem, newName: String, context: Context) {
        viewModelScope.launch {
            isOperationLoading = true
            val oldKey = fileItem.key
            val newKey = if (currentPrefix != null) "${currentPrefix}$newName" else newName
            
            repository.renameObject(bucketName, oldKey, newKey)
                .onSuccess {
                    loadFiles()
                    Toast.makeText(context, context.getString(R.string.renamed_success), Toast.LENGTH_SHORT).show()
                }
            isOperationLoading = false
        }
    }

    fun copyFile(fileItem: FileItem, destBucket: String, destKey: String, context: Context) {
        viewModelScope.launch {
            isOperationLoading = true
            repository.copyObject(bucketName, fileItem.key, destBucket, destKey)
                .onSuccess {
                    loadFiles()
                    Toast.makeText(context, context.getString(R.string.copied_success), Toast.LENGTH_SHORT).show()
                }
            isOperationLoading = false
        }
    }

    fun moveFile(fileItem: FileItem, destBucket: String, destKey: String, context: Context) {
        viewModelScope.launch {
            isOperationLoading = true
            repository.moveObject(bucketName, fileItem.key, destBucket, destKey)
                .onSuccess {
                    loadFiles()
                    Toast.makeText(context, context.getString(R.string.moved_success), Toast.LENGTH_SHORT).show()
                }
            isOperationLoading = false
        }
    }

    fun previewFile(fileItem: FileItem, allFiles: List<FileItem>) {
        viewModelScope.launch {
            isOperationLoading = true
            repository.generatePresignedUrl(bucketName, fileItem.key)
                .onSuccess { url ->
                    previewFiles = allFiles.filter { !it.isFolder }
                    previewFileIndex = previewFiles.indexOfFirst { it.key == fileItem.key }.coerceAtLeast(0)
                    previewUrl = url
                    previewFileItem = previewFiles.getOrNull(previewFileIndex)
                }
            isOperationLoading = false
        }
    }

    fun updatePreviewIndex(index: Int) {
        val file = previewFiles.getOrNull(index) ?: return
        previewFileIndex = index
        previewFileItem = file
        viewModelScope.launch {
            repository.generatePresignedUrl(bucketName, file.key)
                .onSuccess { url ->
                    previewUrl = url
                }
        }
    }

    fun generateShareLink(file: FileItem, onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.generatePresignedUrl(bucketName, file.key)
                .onSuccess { onResult(it) }
        }
    }

    fun getPublicLink(file: FileItem): String? {
        val domain = publicDomain ?: return null
        return "https://$domain/${file.key}"
    }

    fun dismissPreview() {
        previewFileItem = null
        previewUrl = null
        previewFiles = emptyList()
    }

    fun downloadFile(fileItem: FileItem, context: Context) {
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

            isOperationLoading = true
            repository.getObject(
                bucketName = bucketName, 
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
            isOperationLoading = false
        }
    }

    private fun getFileName(uri: Uri, context: Context): String {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "uploaded_file_${UUID.randomUUID()}"
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
}

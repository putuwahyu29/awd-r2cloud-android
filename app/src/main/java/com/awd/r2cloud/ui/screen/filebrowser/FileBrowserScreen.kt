package com.awd.r2cloud.ui.screen.filebrowser

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.awd.r2cloud.R
import com.awd.r2cloud.domain.model.FileItem
import com.awd.r2cloud.ui.component.FolderSelectorDialog
import com.awd.r2cloud.ui.util.FileUtils
import com.awd.r2cloud.ui.viewmodel.FileBrowserUiState
import com.awd.r2cloud.ui.viewmodel.FileBrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    bucketName: String,
    onBack: () -> Unit,
    viewModel: FileBrowserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    var fileToRename by remember { mutableStateOf<FileItem?>(null) }
    var fileToMove by remember { mutableStateOf<FileItem?>(null) }
    var fileToCopy by remember { mutableStateOf<FileItem?>(null) }
    
    var isSearching by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sharingUrl by remember { mutableStateOf<String?>(null) }
    
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadFiles(uris, context)
            Toast.makeText(context, context.getString(R.string.uploading_n_files, uris.size), Toast.LENGTH_SHORT).show()
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.uploadFolder(it, context)
            Toast.makeText(context, context.getString(R.string.uploading_folder), Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        if (viewModel.isMultiSelectMode) {
            viewModel.exitMultiSelect()
        } else if (isSearching) {
            isSearching = false
            viewModel.searchQuery = ""
        } else if (!viewModel.navigateBack()) {
            onBack()
        }
    }

    if (sharingUrl != null) {
        ShareSheet(
            url = sharingUrl!!,
            fileName = viewModel.previewFileItem?.name ?: "File",
            onDismiss = { sharingUrl = null }
        )
    }

    // Move/Copy Dialog
    if (fileToMove != null) {
        FolderSelectorDialog(
            repository = viewModel.repositoryForDialog,
            onDismiss = { fileToMove = null },
            onSelected = { destBucket, destPrefix ->
                val destKey = "${destPrefix}${fileToMove!!.name}"
                viewModel.moveFile(fileToMove!!, destBucket, destKey, context)
                fileToMove = null
            }
        )
    }

    if (fileToCopy != null) {
        FolderSelectorDialog(
            repository = viewModel.repositoryForDialog,
            onDismiss = { fileToCopy = null },
            onSelected = { destBucket, destPrefix ->
                val destKey = "${destPrefix}${fileToCopy!!.name}"
                viewModel.copyFile(fileToCopy!!, destBucket, destKey, context)
                fileToCopy = null
            }
        )
    }

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    stringResource(R.string.create_new),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CreateOptionItem(
                        icon = Icons.Default.CreateNewFolder,
                        label = stringResource(R.string.create_folder),
                        onClick = {
                            showCreateSheet = false
                            showCreateFolderDialog = true
                        }
                    )
                    CreateOptionItem(
                        icon = Icons.Default.UploadFile,
                        label = stringResource(R.string.upload_file),
                        onClick = {
                            showCreateSheet = false
                            fileLauncher.launch("*/*")
                        }
                    )
                    CreateOptionItem(
                        icon = Icons.Default.DriveFolderUpload,
                        label = stringResource(R.string.upload_folder),
                        onClick = {
                            showCreateSheet = false
                            folderLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text(stringResource(R.string.create_folder)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.create_folder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (folderName.isNotBlank()) {
                        viewModel.createFolder(folderName, context)
                        showCreateFolderDialog = false
                    }
                }) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.bulk_delete_confirm, viewModel.selectedFiles.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bulkDelete(context)
                        showBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete) + " All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.file_delete_confirm, fileToDelete!!.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(fileToDelete!!, context)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (fileToRename != null) {
        RenameDialog(
            initialName = fileToRename!!.name,
            onDismiss = { fileToRename = null },
            onConfirm = { newName ->
                viewModel.renameFile(fileToRename!!, newName, context)
                fileToRename = null
            }
        )
    }

    if (viewModel.previewFileItem != null && viewModel.previewUrl != null) {
        FilePreviewDialog(
            fileItem = viewModel.previewFileItem!!,
            url = viewModel.previewUrl!!,
            allFiles = viewModel.previewFiles,
            initialIndex = viewModel.previewFileIndex,
            onDismiss = { viewModel.dismissPreview() },
            onDownload = { file -> viewModel.downloadFile(file, context) },
            onPageChanged = { index -> viewModel.updatePreviewIndex(index) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (viewModel.isMultiSelectMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.selected_count, viewModel.selectedFiles.size)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitMultiSelect() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.bulkDownload(context) }) {
                            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.bulk_download))
                        }
                        IconButton(onClick = { showBulkDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.bulk_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            } else {
                TopAppBar(
                    title = { 
                        if (isSearching) {
                            OutlinedTextField(
                                value = viewModel.searchQuery,
                                onValueChange = { viewModel.searchQuery = it },
                                placeholder = { Text(stringResource(R.string.search_hint)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        isSearching = false
                                        viewModel.searchQuery = ""
                                    }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        } else {
                            Text(
                                text = if (viewModel.currentPrefix == null) bucketName else viewModel.currentPrefix!!.removeSuffix("/").substringAfterLast("/"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        if (!isSearching) {
                            IconButton(onClick = {
                                if (!viewModel.navigateBack()) onBack()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (!isSearching) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_files))
                            }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_name_asc)) },
                                        onClick = { viewModel.updateSortOrder(FileBrowserViewModel.SortOrder.NAME_ASC); showSortMenu = false },
                                        leadingIcon = { if (viewModel.sortOrder == FileBrowserViewModel.SortOrder.NAME_ASC) Icon(Icons.Default.Check, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_size_desc)) },
                                        onClick = { viewModel.updateSortOrder(FileBrowserViewModel.SortOrder.SIZE_DESC); showSortMenu = false },
                                        leadingIcon = { if (viewModel.sortOrder == FileBrowserViewModel.SortOrder.SIZE_DESC) Icon(Icons.Default.Check, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_date_desc)) },
                                        onClick = { viewModel.updateSortOrder(FileBrowserViewModel.SortOrder.DATE_DESC); showSortMenu = false },
                                        leadingIcon = { if (viewModel.sortOrder == FileBrowserViewModel.SortOrder.DATE_DESC) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.toggleLayout() }) {
                                Icon(if (viewModel.isGridView) Icons.Default.ViewList else Icons.Default.GridView, null)
                            }
                            IconButton(onClick = { viewModel.loadFiles() }) {
                                Icon(Icons.Default.Refresh, null)
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!viewModel.isMultiSelectMode) {
                FloatingActionButton(onClick = { showCreateSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val state = viewModel.uiState
            when (state) {
                is FileBrowserUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FileBrowserUiState.Success -> {
                    val filteredFiles = state.files.filter { it.name.contains(viewModel.searchQuery, ignoreCase = true) }
                    if (filteredFiles.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.empty_folder_search), color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        if (viewModel.isGridView) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(120.dp),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredFiles) { file ->
                                    FileGridItem(
                                        file = file,
                                        isSelected = viewModel.selectedFiles.contains(file),
                                        isStarred = viewModel.starredFileKeys.contains("${bucketName}|${file.key}"),
                                        publicLink = viewModel.getPublicLink(file),
                                        onFolderClick = { viewModel.navigateToFolder(file.key) },
                                        onDeleteClick = { fileToDelete = file },
                                        onDownloadClick = { viewModel.downloadFile(file, context) },
                                        onFileClick = { viewModel.previewFile(file, state.files) },
                                        onRenameClick = { fileToRename = file },
                                        onLongClick = { viewModel.toggleSelection(file) },
                                        onStarredClick = { viewModel.toggleStarred(file, context) },
                                        onShareClick = { viewModel.generateShareLink(file) { sharingUrl = it } },
                                        onCopyPublicLink = { 
                                            clipboardManager.setText(AnnotatedString(it))
                                            Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
                                        },
                                        onMoveClick = { fileToMove = file },
                                        onCopyClick = { fileToCopy = file }
                                    )
                                }
                            }
                        } else {
                            LazyColumn {
                                items(filteredFiles) { file ->
                                    FileListItem(
                                        file = file,
                                        isSelected = viewModel.selectedFiles.contains(file),
                                        isStarred = viewModel.starredFileKeys.contains("${bucketName}|${file.key}"),
                                        publicLink = viewModel.getPublicLink(file),
                                        onFolderClick = { viewModel.navigateToFolder(file.key) },
                                        onDeleteClick = { fileToDelete = file },
                                        onDownloadClick = { viewModel.downloadFile(file, context) },
                                        onFileClick = { viewModel.previewFile(file, state.files) },
                                        onRenameClick = { fileToRename = file },
                                        onLongClick = { viewModel.toggleSelection(file) },
                                        onStarredClick = { viewModel.toggleStarred(file, context) },
                                        onShareClick = { viewModel.generateShareLink(file) { sharingUrl = it } },
                                        onCopyPublicLink = { 
                                            clipboardManager.setText(AnnotatedString(it))
                                            Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
                                        },
                                        onMoveClick = { fileToMove = file },
                                        onCopyClick = { fileToCopy = file }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
                is FileBrowserUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
            }
            if (viewModel.isOperationLoading) {
                Surface(Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.3f)) {
                    CircularProgressIndicator(modifier = Modifier.wrapContentSize(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun CreateOptionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp).width(80.dp)
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null) }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: FileItem,
    isSelected: Boolean,
    isStarred: Boolean,
    publicLink: String?,
    onFolderClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onFileClick: () -> Unit,
    onRenameClick: () -> Unit,
    onLongClick: () -> Unit,
    onStarredClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyPublicLink: (String) -> Unit,
    onMoveClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, color) = getFileIconAndColor(file.name, file.isFolder)

    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(file.name, fontWeight = if (file.isFolder) FontWeight.Medium else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isStarred) Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp).padding(start = 4.dp))
            }
        },
        supportingContent = { if (!file.isFolder) Text("${FileUtils.formatSize(file.size)} • ${file.lastModified?.take(16) ?: ""}") },
        leadingContent = {
            Surface(shape = MaterialTheme.shapes.small, color = if (file.isFolder) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    else if (file.thumbnailUrl != null) Image(rememberAsyncImagePainter(file.thumbnailUrl), null, contentScale = ContentScale.Crop)
                    else Icon(icon, null, tint = color)
                }
            }
        },
        trailingContent = {
            if (!file.isFolder) {
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.download)) }, onClick = { onDownloadClick(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Download, null) })
                        DropdownMenuItem(text = { Text(if (isStarred) stringResource(R.string.remove_star) else stringResource(R.string.add_star)) }, onClick = { onStarredClick(); showMenu = false }, leadingIcon = { Icon(if (isStarred) Icons.Default.StarOutline else Icons.Default.Star, null) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.copy_action)) }, onClick = { onCopyClick(); showMenu = false }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.move)) }, onClick = { onMoveClick(); showMenu = false }, leadingIcon = { Icon(Icons.Default.DriveFileMove, null) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = { onRenameClick(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { onDeleteClick(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) })
                    }
                }
            }
        },
        modifier = Modifier.combinedClickable(onClick = { if (file.isFolder) onFolderClick() else onFileClick() }, onLongClick = onLongClick)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FileItem,
    isSelected: Boolean,
    isStarred: Boolean,
    publicLink: String?,
    onFolderClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onFileClick: () -> Unit,
    onRenameClick: () -> Unit,
    onLongClick: () -> Unit,
    onStarredClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyPublicLink: (String) -> Unit,
    onMoveClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, color) = getFileIconAndColor(file.name, file.isFolder)

    ElevatedCard(modifier = Modifier.aspectRatio(1f), onClick = { if (file.isFolder) onFolderClick() else onFileClick() }) {
        Box(modifier = Modifier.fillMaxSize().combinedClickable(onClick = { if (file.isFolder) onFolderClick() else onFileClick() }, onLongClick = onLongClick)) {
            Column(modifier = Modifier.padding(8.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (file.thumbnailUrl != null) Image(rememberAsyncImagePainter(file.thumbnailUrl), null, modifier = Modifier.size(64.dp), contentScale = ContentScale.Crop)
                else Icon(icon, null, modifier = Modifier.size(48.dp), tint = color)
                Spacer(Modifier.height(8.dp))
                Text(file.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!file.isFolder) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp)) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.download)) }, onClick = { onDownloadClick(); showMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.copy_action)) }, onClick = { onCopyClick(); showMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.move)) }, onClick = { onMoveClick(); showMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { onDeleteClick(); showMenu = false })
                    }
                }
            }
            if (isSelected) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

private fun getFileIconAndColor(name: String, isFolder: Boolean): Pair<ImageVector, Color> {
    if (isFolder) return Icons.Default.Folder to Color(0xFFFBC02D)
    val ext = name.substringAfterLast(".", "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "webp", "gif" -> Icons.Default.Image to Color(0xFF42A5F5)
        "mp4", "mkv", "mov" -> Icons.Default.VideoFile to Color(0xFFAB47BC)
        "mp3", "wav", "flac" -> Icons.Default.AudioFile to Color(0xFF26A69A)
        "pdf" -> Icons.Default.PictureAsPdf to Color(0xFFEF5350)
        else -> Icons.Default.InsertDriveFile to Color(0xFF78909C)
    }
}

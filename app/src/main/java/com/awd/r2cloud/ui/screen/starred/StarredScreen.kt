package com.awd.r2cloud.ui.screen.starred

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awd.r2cloud.R
import com.awd.r2cloud.domain.model.FileItem
import com.awd.r2cloud.ui.screen.filebrowser.FileListItem
import com.awd.r2cloud.ui.screen.filebrowser.FilePreviewDialog
import com.awd.r2cloud.ui.screen.filebrowser.ShareSheet
import com.awd.r2cloud.ui.viewmodel.StarredUiState
import com.awd.r2cloud.ui.viewmodel.StarredViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredScreen(
    viewModel: StarredViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()
    
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    var sharingUrl by remember { mutableStateOf<String?>(null) }

    if (sharingUrl != null) {
        ShareSheet(
            url = sharingUrl!!,
            fileName = viewModel.previewFileItem?.name ?: "File",
            onDismiss = { sharingUrl = null }
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

    if (viewModel.previewFileItem != null && viewModel.previewUrl != null) {
        val allFiles = (uiState as? StarredUiState.Success)?.files ?: emptyList()
        FilePreviewDialog(
            fileItem = viewModel.previewFileItem!!,
            url = viewModel.previewUrl!!,
            allFiles = allFiles,
            initialIndex = allFiles.indexOf(viewModel.previewFileItem!!).coerceAtLeast(0),
            onDismiss = { viewModel.dismissPreview() },
            onDownload = { viewModel.downloadFile(viewModel.previewFileItem!!, context) },
            onPageChanged = { index ->
                val nextFile = allFiles.getOrNull(index)
                if (nextFile != null) viewModel.previewFile(nextFile)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.starred), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            when (val state = uiState) {
                is StarredUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is StarredUiState.Success -> {
                    if (state.files.isEmpty()) {
                        EmptyStarred()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(state.files) { file ->
                                FileListItem(
                                    file = file,
                                    isSelected = false,
                                    isStarred = true,
                                    publicLink = null,
                                    onFolderClick = {},
                                    onDeleteClick = { fileToDelete = file },
                                    onDownloadClick = { viewModel.downloadFile(file, context) },
                                    onFileClick = { viewModel.previewFile(file) },
                                    onRenameClick = {},
                                    onLongClick = {},
                                    onStarredClick = { viewModel.toggleStarred(file, context) },
                                    onShareClick = {
                                        viewModel.generateShareLink(file) { url ->
                                            sharingUrl = url
                                        }
                                    },
                                    onCopyPublicLink = { link ->
                                        clipboardManager.setText(AnnotatedString(link))
                                    },
                                    onMoveClick = {},
                                    onCopyClick = {}
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
                is StarredUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
            }

            if (viewModel.isOperationLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStarred() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFFC107).copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.no_starred), color = MaterialTheme.colorScheme.outline)
    }
}

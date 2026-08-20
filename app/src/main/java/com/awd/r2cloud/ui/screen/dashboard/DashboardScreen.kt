package com.awd.r2cloud.ui.screen.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.awd.r2cloud.R
import com.awd.r2cloud.domain.model.FileItem
import com.awd.r2cloud.ui.component.FolderSelectorDialog
import com.awd.r2cloud.ui.screen.filebrowser.FileListItem
import com.awd.r2cloud.ui.screen.filebrowser.FilePreviewDialog
import com.awd.r2cloud.ui.screen.filebrowser.ShareSheet
import com.awd.r2cloud.ui.util.FileUtils
import com.awd.r2cloud.ui.viewmodel.BucketListUiState
import com.awd.r2cloud.ui.viewmodel.BucketListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBucketClick: (String) -> Unit,
    bucketViewModel: BucketListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    var bucketToDelete by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // File Action States
    var previewFile by remember { mutableStateOf<FileItem?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var sharingUrl by remember { mutableStateOf<String?>(null) }
    var fileToMove by remember { mutableStateOf<FileItem?>(null) }
    var fileToCopy by remember { mutableStateOf<FileItem?>(null) }

    LaunchedEffect(Unit) {
        bucketViewModel.getBuckets()
        bucketViewModel.loadRecentFiles()
    }

    if (sharingUrl != null) {
        ShareSheet(
            url = sharingUrl!!,
            fileName = previewFile?.name ?: "File",
            onDismiss = { sharingUrl = null }
        )
    }

    if (fileToMove != null) {
        FolderSelectorDialog(
            repository = bucketViewModel.repositoryForDialog,
            onDismiss = { fileToMove = null },
            onSelected = { destBucket, destPrefix ->
                bucketViewModel.moveFile(fileToMove!!, destBucket, "${destPrefix}${fileToMove!!.name}")
                fileToMove = null
                Toast.makeText(context, context.getString(R.string.moved_success), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (fileToCopy != null) {
        FolderSelectorDialog(
            repository = bucketViewModel.repositoryForDialog,
            onDismiss = { fileToCopy = null },
            onSelected = { destBucket, destPrefix ->
                bucketViewModel.copyFile(fileToCopy!!, destBucket, "${destPrefix}${fileToCopy!!.name}")
                fileToCopy = null
                Toast.makeText(context, context.getString(R.string.copied_success), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCreateDialog) {
        CreateBucketDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { bucketName, region ->
                bucketViewModel.createBucket(bucketName, region) { result ->
                    if (result.isSuccess) {
                        showCreateDialog = false
                        Toast.makeText(context, context.getString(R.string.bucket_created), Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: context.getString(R.string.unknown_error)
                        Toast.makeText(context, "${context.getString(R.string.error_label)}: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            },
            isCreating = bucketViewModel.isCreatingBucket
        )
    }

    if (bucketToDelete != null) {
        AlertDialog(
            onDismissRequest = { bucketToDelete = null },
            title = { Text(stringResource(R.string.delete) + " Bucket") },
            text = { Text(stringResource(R.string.bucket_delete_confirm, bucketToDelete!!)) },
            confirmButton = {
                Button(
                    onClick = {
                        bucketViewModel.deleteBucket(bucketToDelete!!) { result ->
                            bucketToDelete = null
                            if (result.isSuccess) {
                                Toast.makeText(context, context.getString(R.string.bucket_deleted), Toast.LENGTH_SHORT).show()
                            } else {
                                val errorMsg = result.exceptionOrNull()?.message ?: context.getString(R.string.unknown_error)
                                Toast.makeText(context, "${context.getString(R.string.error_label)}: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { bucketToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Advanced Preview Dialog
    if (previewFile != null && previewUrl != null) {
        val currentList = if (bucketViewModel.isSearchingGlobal || bucketViewModel.selectedCategory != null) 
            bucketViewModel.globalSearchResults else bucketViewModel.recentFiles
            
        FilePreviewDialog(
            fileItem = previewFile!!,
            url = previewUrl!!,
            allFiles = currentList,
            initialIndex = currentList.indexOfFirst { it.key == previewFile!!.key }.coerceAtLeast(0),
            onDismiss = { 
                previewFile = null
                previewUrl = null
            },
            onDownload = { file -> 
                bucketViewModel.downloadFile(file, context)
                Toast.makeText(context, "Unduhan dimulai...", Toast.LENGTH_SHORT).show()
            },
            onPageChanged = { index ->
                val nextFile = currentList.getOrNull(index)
                if (nextFile != null && nextFile.key != previewFile?.key) {
                    bucketViewModel.generatePreviewUrl(nextFile) { url ->
                        previewFile = nextFile
                        previewUrl = url
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.buckets), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { 
                        bucketViewModel.getBuckets() 
                        bucketViewModel.loadRecentFiles()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.create)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                StorageAnalyticsCard(
                    usedSize = bucketViewModel.totalStorageSize,
                    limitSize = bucketViewModel.storageLimit,
                    percentage = bucketViewModel.storageUsagePercentage,
                    distribution = bucketViewModel.storageDistribution
                )
            }
            
            item {
                OperationsMonitoringCard(
                    classAUsed = bucketViewModel.classAOperations,
                    classALimit = bucketViewModel.classALimit,
                    classAPercentage = bucketViewModel.classAUsagePercentage,
                    classBOperations = bucketViewModel.classBOperations,
                    classBLimit = bucketViewModel.classBLimit,
                    classBPercentage = bucketViewModel.classBUsagePercentage
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = bucketViewModel.searchQuery,
                        onValueChange = { bucketViewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.search_files)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (bucketViewModel.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { bucketViewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = bucketViewModel.selectedCategory == "images",
                                onClick = { bucketViewModel.onCategorySelect("images") },
                                label = { Text(stringResource(R.string.images)) },
                                leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = bucketViewModel.selectedCategory == "videos",
                                onClick = { bucketViewModel.onCategorySelect("videos") },
                                label = { Text(stringResource(R.string.videos)) },
                                leadingIcon = { Icon(Icons.Default.VideoFile, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = bucketViewModel.selectedCategory == "documents",
                                onClick = { bucketViewModel.onCategorySelect("documents") },
                                label = { Text(stringResource(R.string.documents)) },
                                leadingIcon = { Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }

            if (bucketViewModel.isSearchingGlobal || bucketViewModel.selectedCategory != null) {
                item {
                    Text(
                        text = stringResource(R.string.search_results),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (bucketViewModel.globalSearchResults.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            message = stringResource(R.string.no_results_found)
                        )
                    }
                } else {
                    items(bucketViewModel.globalSearchResults) { file ->
                        FileListItem(
                            file = file,
                            isSelected = false,
                            isStarred = bucketViewModel.starredFileKeys.contains("${file.bucketName}|${file.key}"),
                            publicLink = null,
                            onFolderClick = {},
                            onDeleteClick = { bucketViewModel.deleteFile(file) },
                            onDownloadClick = { Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_SHORT).show() },
                            onFileClick = {
                                bucketViewModel.generatePreviewUrl(file) { url ->
                                    previewFile = file
                                    previewUrl = url
                                }
                            },
                            onRenameClick = {},
                            onLongClick = {},
                            onStarredClick = { bucketViewModel.toggleStarred(file) },
                            onShareClick = {
                                bucketViewModel.generatePreviewUrl(file) { url ->
                                    previewFile = file
                                    sharingUrl = url
                                }
                            },
                            onCopyPublicLink = {},
                            onMoveClick = { fileToMove = file },
                            onCopyClick = { fileToCopy = file }
                        )
                    }
                }
            } else {
                // Recents Section
                if (bucketViewModel.recentFiles.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.recent),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(bucketViewModel.recentFiles) { file ->
                                RecentFileCard(
                                    file = file,
                                    onClick = {
                                        bucketViewModel.generatePreviewUrl(file) { url ->
                                            previewFile = file
                                            previewUrl = url
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (bucketViewModel.isPoolMode) stringResource(R.string.virtual_pool) else stringResource(R.string.my_buckets),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = bucketViewModel.isPoolMode,
                            onClick = { bucketViewModel.isPoolMode = !bucketViewModel.isPoolMode },
                            label = { Text(stringResource(R.string.pool_mode)) },
                            leadingIcon = { if (bucketViewModel.isPoolMode) Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                when (val state = bucketViewModel.uiState) {
                    is BucketListUiState.Loading -> {
                        item {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is BucketListUiState.Success -> {
                        if (bucketViewModel.isPoolMode) {
                            item { UnifiedPoolView(state.buckets) }
                        } else {
                            val filteredBuckets = state.buckets.filter { 
                                it.name.contains(bucketViewModel.searchQuery, ignoreCase = true)
                            }
                            
                            if (filteredBuckets.isEmpty()) {
                                item {
                                    EmptyState(
                                        icon = if (bucketViewModel.searchQuery.isEmpty()) Icons.Default.Inventory2 else Icons.Default.SearchOff,
                                        message = if (bucketViewModel.searchQuery.isEmpty()) stringResource(R.string.no_buckets) else stringResource(R.string.search_no_results, bucketViewModel.searchQuery)
                                    )
                                }
                            } else {
                                items(filteredBuckets) { bucket ->
                                    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        BucketCard(
                                            bucket = bucket,
                                            onClick = { onBucketClick(bucket.name) },
                                            onDelete = { bucketToDelete = bucket.name },
                                            onTogglePublic = { enabled ->
                                                bucketViewModel.togglePublicAccess(bucket.name, enabled) { result ->
                                                    if (result.isSuccess) {
                                                        val msg = if (enabled) context.getString(R.string.enabled_label) else context.getString(R.string.disabled_label)
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val errorMsg = result.exceptionOrNull()?.message ?: context.getString(R.string.unknown_error)
                                                        Toast.makeText(context, "${context.getString(R.string.error_label)}: $errorMsg", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is BucketListUiState.Error -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = state.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { 
                                    bucketViewModel.getBuckets() 
                                    bucketViewModel.loadRecentFiles()
                                }) {
                                    @Suppress("DEPRECATION")
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun RecentFileCard(file: FileItem, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .width(140.dp)
            .height(160.dp),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (file.thumbnailUrl != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter(file.thumbnailUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = file.bucketName ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun OperationsMonitoringCard(
    classAUsed: Long,
    classALimit: Long,
    classAPercentage: Float,
    classBOperations: Long,
    classBLimit: Long,
    classBPercentage: Float
) {
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ops_usage),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OperationRow(
                label = stringResource(R.string.class_a_ops),
                subLabel = stringResource(R.string.class_a_limit),
                used = classAUsed,
                limit = classALimit,
                percentage = classAPercentage,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OperationRow(
                label = stringResource(R.string.class_b_ops),
                subLabel = stringResource(R.string.class_b_limit),
                used = classBOperations,
                limit = classBLimit,
                percentage = classBPercentage,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun OperationRow(
    label: String,
    subLabel: String,
    used: Long,
    limit: Long,
    percentage: Float,
    color: Color
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(subLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.outline)
            }
            Text("${formatNumber(used)} / ${formatNumber(limit)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            color = if (percentage > 0.9f) MaterialTheme.colorScheme.error else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> "%.1fM".format(number / 1_000_000f)
        number >= 1_000 -> "%.1fK".format(number / 1_000f)
        else -> number.toString()
    }
}

@Composable
fun UnifiedPoolView(buckets: List<com.awd.r2cloud.domain.model.Bucket>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.virtual_pool), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            buckets.forEach { bucket ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(if (bucket.isOnline) Color(0xFF4CAF50) else Color.Red, MaterialTheme.shapes.extraSmall))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(bucket.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(FileUtils.formatSize(bucket.size), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StorageAnalyticsCard(
    usedSize: Long,
    limitSize: Long,
    percentage: Float,
    distribution: List<com.awd.r2cloud.ui.screen.dashboard.PieChartData>
) {
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.storage_usage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.storage_capacity),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${stringResource(R.string.used)}: ${FileUtils.formatSize(usedSize)} / ${FileUtils.formatSize(limitSize)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    distribution.forEach { item ->
                        val labelId = when(item.label) {
                            "images" -> R.string.images
                            "videos" -> R.string.videos
                            "documents" -> R.string.documents
                            else -> R.string.others
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(item.color, MaterialTheme.shapes.extraSmall))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(labelId), 
                                style = MaterialTheme.typography.labelSmall, 
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                text = FileUtils.formatSize(item.value.toLong()), 
                                style = MaterialTheme.typography.labelSmall, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Box(
                    modifier = Modifier.size(100.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    com.awd.r2cloud.ui.screen.dashboard.PieChart(
                        data = distribution, 
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(R.string.used),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BucketCard(
    bucket: com.awd.r2cloud.domain.model.Bucket,
    onClick: () -> Unit, 
    onDelete: () -> Unit,
    onTogglePublic: (Boolean) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (bucket.isOnline) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (bucket.isOnline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bucket.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (bucket.verifiedViaApi) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = stringResource(R.string.verified_via_api), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    if (bucket.isPublic) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Public, contentDescription = stringResource(R.string.public_access), modifier = Modifier.size(14.dp), tint = Color(0xFF2196F3))
                    }
                }
                Text(
                    text = "${FileUtils.formatSize(bucket.size)} • ${bucket.creationDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (bucket.isPublic) stringResource(R.string.disable_pub) else stringResource(R.string.enable_pub)) },
                        onClick = {
                            showMenu = false
                            onTogglePublic(!bucket.isPublic)
                        },
                        leadingIcon = { Icon(if (bucket.isPublic) Icons.Default.PublicOff else Icons.Default.Public, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete) + " Bucket") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        @Suppress("DEPRECATION")
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBucketDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit,
    isCreating: Boolean
) {
    var bucketName by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    val regions = listOf(
        stringResource(R.string.region_auto) to null,
        stringResource(R.string.region_nam) to "wnam",
        stringResource(R.string.region_eur) to "weur",
        stringResource(R.string.region_apac) to "apac"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_bucket)) },
        text = {
            Column {
                OutlinedTextField(
                    value = bucketName,
                    onValueChange = { 
                        bucketName = it.lowercase() 
                        error = null
                    },
                    label = { Text(stringResource(R.string.bucket_name)) },
                    placeholder = { Text(stringResource(R.string.bucket_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(R.string.name_rules))
                        }
                    },
                    enabled = !isCreating,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() })
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.region), style = MaterialTheme.typography.labelSmall)
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isCreating) expanded = it }
                ) {
                    OutlinedTextField(
                        value = regions.find { it.second == selectedRegion }?.first ?: stringResource(R.string.region_auto),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.region)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = !isCreating
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        regions.forEach { (label, value) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedRegion = value
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bucketName.isBlank()) {
                        error = context.getString(R.string.bucket_name_empty)
                    } else if (!bucketName.matches(Regex("^[a-z0-9.-]*$"))) {
                        error = "Invalid name. Use lowercase, numbers, dots or hyphens."
                    } else {
                        onCreate(bucketName, selectedRegion)
                    }
                },
                enabled = !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.create))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

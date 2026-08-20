package com.awd.r2cloud.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awd.r2cloud.R
import com.awd.r2cloud.domain.model.Bucket
import com.awd.r2cloud.domain.model.FileItem
import com.awd.r2cloud.domain.repository.R2Repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSelectorDialog(
    repository: R2Repository,
    onDismiss: () -> Unit,
    onSelected: (bucketName: String, key: String) -> Unit
) {
    var currentBucket by remember { mutableStateOf<Bucket?>(null) }
    var currentPrefix by remember { mutableStateOf<String?>(null) }
    val prefixStack = remember { mutableStateListOf<String?>() }
    
    var buckets by remember { mutableStateOf<List<Bucket>>(emptyList()) }
    var folders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentBucket, currentPrefix) {
        isLoading = true
        if (currentBucket == null) {
            repository.getBuckets().onSuccess { buckets = it }
        } else {
            repository.listObjects(currentBucket!!.name, currentPrefix).onSuccess { items ->
                folders = items.filter { it.isFolder }
            }
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentBucket != null) {
                    IconButton(onClick = {
                        if (prefixStack.isNotEmpty()) {
                            currentPrefix = prefixStack.removeAt(prefixStack.size - 1)
                        } else {
                            currentBucket = null
                            currentPrefix = null
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = when {
                        currentBucket == null -> stringResource(R.string.select_bucket)
                        currentPrefix == null -> currentBucket!!.name
                        else -> currentPrefix!!.removeSuffix("/").substringAfterLast("/")
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (currentBucket == null) {
                            items(buckets) { bucket ->
                                ListItem(
                                    headlineContent = { Text(bucket.name) },
                                    leadingContent = { Icon(Icons.Default.Storage, null) },
                                    modifier = Modifier.clickable { currentBucket = bucket }
                                )
                            }
                        } else {
                            item {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.this_location), fontWeight = FontWeight.Bold) },
                                    leadingContent = { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.clickable { 
                                        onSelected(currentBucket!!.name, currentPrefix ?: "")
                                        onDismiss()
                                    }
                                )
                            }
                            items(folders) { folder ->
                                ListItem(
                                    headlineContent = { Text(folder.name) },
                                    leadingContent = { Icon(Icons.Default.Folder, null) },
                                    modifier = Modifier.clickable {
                                        prefixStack.add(currentPrefix)
                                        currentPrefix = folder.key
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

package com.awd.r2cloud.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.awd.r2cloud.R
import com.awd.r2cloud.data.worker.BackupManager
import com.awd.r2cloud.ui.viewmodel.BackupViewModel
import com.awd.r2cloud.ui.viewmodel.BucketListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
    bucketViewModel: BucketListViewModel = hiltViewModel(),
    backupManager: BackupManager
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedBucket by remember { mutableStateOf("") }
    
    val isWiFiOnly by viewModel.isWiFiOnly.collectAsState()
    val backupInterval by viewModel.backupInterval.collectAsState()

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            selectedUri = it
            showAddDialog = true
        }
    }

    if (showAddDialog && selectedUri != null) {
        val folder = DocumentFile.fromTreeUri(context, selectedUri!!)
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.setup_backup)) },
            text = {
                Column {
                    Text(stringResource(R.string.folder_label, folder?.name ?: "Unknown"), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.target_bucket), style = MaterialTheme.typography.labelSmall)
                    
                    val buckets = (bucketViewModel.uiState as? com.awd.r2cloud.ui.viewmodel.BucketListUiState.Success)?.buckets ?: emptyList()
                    
                    if (buckets.isEmpty()) {
                        Text(stringResource(R.string.no_buckets), color = MaterialTheme.colorScheme.error)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedBucket,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.buckets)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                buckets.forEach { bucket ->
                                    DropdownMenuItem(
                                        text = { Text(bucket.name) },
                                        onClick = {
                                            selectedBucket = bucket.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedBucket.isNotBlank()) {
                            viewModel.addFolder(selectedUri.toString(), folder?.name ?: "Unknown", selectedBucket)
                            showAddDialog = false
                        }
                    },
                    enabled = selectedBucket.isNotBlank()
                ) {
                    Text(stringResource(R.string.connect))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auto_backup)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { backupManager.runNow() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Backup Now")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { folderLauncher.launch(null) }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(stringResource(R.string.global_settings), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                ElevatedCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.wifi_only), modifier = Modifier.weight(1f))
                            Switch(checked = isWiFiOnly, onCheckedChange = { viewModel.setWiFiOnly(it) })
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.backup_interval), modifier = Modifier.weight(1f))
                            
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text("${backupInterval}h")
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf(1, 6, 12, 24).forEach { hours ->
                                        DropdownMenuItem(
                                            text = { Text("${hours} hours") },
                                            onClick = {
                                                viewModel.setBackupInterval(hours)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.selected_folders),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (viewModel.backupFolders.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_folders_backup), color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            items(viewModel.backupFolders) { folder ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.name, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.to_label, folder.bucketName), style = MaterialTheme.typography.bodySmall)
                            if (folder.lastBackup != null) {
                                Text("${stringResource(R.string.last_sync)}: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(folder.lastBackup))}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Switch(
                            checked = folder.isEnabled,
                            onCheckedChange = { viewModel.toggleFolder(folder.uri, it) }
                        )
                        IconButton(onClick = { viewModel.removeFolder(folder.uri) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

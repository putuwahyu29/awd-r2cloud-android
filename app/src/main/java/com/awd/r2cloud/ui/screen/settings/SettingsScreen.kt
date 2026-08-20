package com.awd.r2cloud.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awd.r2cloud.R
import com.awd.r2cloud.domain.repository.AppLanguage
import com.awd.r2cloud.domain.repository.AppTheme
import com.awd.r2cloud.domain.repository.R2Repository
import com.awd.r2cloud.ui.viewmodel.AccountViewModel
import com.awd.r2cloud.ui.viewmodel.BucketListViewModel
import com.awd.r2cloud.ui.viewmodel.PreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAddAccount: () -> Unit,
    onLogoutAll: () -> Unit,
    onViewBackup: () -> Unit,
    repository: R2Repository,
    accountViewModel: AccountViewModel = hiltViewModel(),
    bucketViewModel: BucketListViewModel = hiltViewModel(),
    preferencesViewModel: PreferencesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentTheme by preferencesViewModel.theme.collectAsState()
    val currentLanguage by preferencesViewModel.language.collectAsState()
    
    var showEncryptionDialog by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var isAppLockEnabled by remember { mutableStateOf(repository.isAppLockEnabled()) }
    val activeAccount = accountViewModel.accounts.find { it.accountId == accountViewModel.activeAccountId }

    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text(stringResource(R.string.changelog)) },
            text = {
                Column {
                    Text("• " + stringResource(R.string.cl_ui_redesign), style = MaterialTheme.typography.bodySmall)
                    Text("• " + stringResource(R.string.cl_auto_backup), style = MaterialTheme.typography.bodySmall)
                    Text("• " + stringResource(R.string.cl_consistency), style = MaterialTheme.typography.bodySmall)
                    Text("• " + stringResource(R.string.cl_security), style = MaterialTheme.typography.bodySmall)
                    Text("• " + stringResource(R.string.cl_multi_lang), style = MaterialTheme.typography.bodySmall)
                    Text("• " + stringResource(R.string.cl_themes), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showEncryptionDialog && activeAccount != null) {
        var key by remember { mutableStateOf(activeAccount.encryptionKey ?: "") }
        AlertDialog(
            onDismissRequest = { showEncryptionDialog = false },
            title = { Text(stringResource(R.string.encryption)) },
            text = {
                Column {
                    Text(stringResource(R.string.encryption_key_desc), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text(stringResource(R.string.master_key)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updatedAccount = activeAccount.copy(encryptionKey = key.ifBlank { null })
                    repository.saveAccount(updatedAccount)
                    accountViewModel.refreshAccounts()
                    showEncryptionDialog = false
                }) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEncryptionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.accounts),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(accountViewModel.accounts) { account ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        accountViewModel.switchAccount(account.accountId)
                        bucketViewModel.getBuckets()
                    },
                    colors = if (account.accountId == accountViewModel.activeAccountId) {
                        CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        CardDefaults.elevatedCardColors()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.alias, fontWeight = FontWeight.Bold)
                            Text(account.accountId, style = MaterialTheme.typography.bodySmall)
                        }
                        if (account.accountId != accountViewModel.activeAccountId) {
                            IconButton(onClick = { accountViewModel.removeAccount(account.accountId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Account", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onAddAccount,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_account))
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.personalization),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme)) },
                        supportingContent = { 
                            val themeLabel = when(currentTheme) {
                                AppTheme.LIGHT -> stringResource(R.string.theme_light)
                                AppTheme.DARK -> stringResource(R.string.theme_dark)
                                AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                            }
                            Text(themeLabel) 
                        },
                        leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                        modifier = Modifier.clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AppTheme.entries.forEach { theme ->
                            val label = when(theme) {
                                AppTheme.LIGHT -> stringResource(R.string.theme_light)
                                AppTheme.DARK -> stringResource(R.string.theme_dark)
                                AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    preferencesViewModel.setTheme(theme)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.language)) },
                        supportingContent = { Text(if (currentLanguage == AppLanguage.ENGLISH) stringResource(R.string.lang_en) else stringResource(R.string.lang_id)) },
                        leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                        modifier = Modifier.clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(if (lang == AppLanguage.ENGLISH) stringResource(R.string.lang_en) else stringResource(R.string.lang_id)) },
                                onClick = {
                                    preferencesViewModel.setLanguage(lang)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.security_backup),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_lock)) },
                    supportingContent = { Text(stringResource(R.string.app_lock_desc)) },
                    leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = {
                                isAppLockEnabled = it
                                repository.setAppLockEnabled(it)
                            }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.encryption)) },
                    supportingContent = { Text(if (activeAccount?.encryptionKey != null) "${stringResource(R.string.enabled_label)} (AES-256)" else stringResource(R.string.disabled_label)) },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { showEncryptionDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Key")
                        }
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.auto_backup)) },
                    supportingContent = { Text(stringResource(R.string.manage_backup)) },
                    leadingContent = { Icon(Icons.Default.Sync, contentDescription = null) },
                    modifier = Modifier.clickable { onViewBackup() }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.about),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.check_updates)) },
                    supportingContent = { Text(stringResource(R.string.check_github)) },
                    leadingContent = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/putuwahyu29/awd-r2cloud-andorid")))
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.changelog)) },
                    supportingContent = { Text(stringResource(R.string.app_version_label)) },
                    leadingContent = { Icon(Icons.Default.HistoryEdu, contentDescription = null) },
                    modifier = Modifier.clickable { showChangelog = true }
                )
            }

            item {
                TextButton(
                    onClick = {
                        repository.clearAll()
                        onLogoutAll()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.logout_all))
                }
            }
        }
    }
}

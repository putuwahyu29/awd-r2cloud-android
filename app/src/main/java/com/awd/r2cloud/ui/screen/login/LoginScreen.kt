package com.awd.r2cloud.ui.screen.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awd.r2cloud.R
import com.awd.r2cloud.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) {
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text(stringResource(R.string.add_account), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onBack == null) {
                Spacer(modifier = Modifier.height(48.dp))
                Icon(
                    Icons.Default.CloudQueue,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.login_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            Text(
                text = stringResource(R.string.credentials),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))

            CredentialField(
                label = stringResource(R.string.account_alias_hint),
                value = viewModel.accountName,
                icon = Icons.Default.Label,
                onValueChange = { viewModel.accountName = it },
                onPaste = { clipboardManager.getText()?.text?.let { viewModel.accountName = it } }
            )
            Spacer(modifier = Modifier.height(12.dp))

            CredentialField(
                label = "Account ID",
                value = viewModel.accountId,
                icon = Icons.Default.Badge,
                onValueChange = { viewModel.accountId = it },
                onPaste = { clipboardManager.getText()?.text?.let { viewModel.accountId = it } }
            )
            Spacer(modifier = Modifier.height(12.dp))

            CredentialField(
                label = "Access Key ID",
                value = viewModel.accessKeyId,
                icon = Icons.Default.Key,
                onValueChange = { viewModel.accessKeyId = it },
                onPaste = { clipboardManager.getText()?.text?.let { viewModel.accessKeyId = it } }
            )
            Spacer(modifier = Modifier.height(12.dp))

            CredentialField(
                label = "Secret Access Key",
                value = viewModel.secretAccessKey,
                icon = Icons.Default.Lock,
                onValueChange = { viewModel.secretAccessKey = it },
                onPaste = { clipboardManager.getText()?.text?.let { viewModel.secretAccessKey = it } },
                isPassword = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.advanced_optional),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            CredentialField(
                label = "Cloudflare API Token",
                value = viewModel.apiToken,
                icon = Icons.Default.VpnKey,
                onValueChange = { viewModel.apiToken = it },
                onPaste = { clipboardManager.getText()?.text?.let { viewModel.apiToken = it } },
                isPassword = true
            )
            Text(
                stringResource(R.string.api_token_desc),
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (viewModel.errorResId != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(viewModel.errorResId!!),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = { viewModel.onConnectClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !viewModel.isLoading && viewModel.isFormValid
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(stringResource(R.string.connect), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://developers.cloudflare.com/r2/api/s3/tokens/"))
                    context.startActivity(intent)
                }
            ) {
                Text(stringResource(R.string.how_to_get_keys))
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun CredentialField(
    label: String,
    value: String,
    icon: ImageVector,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        trailingIcon = {
            Row {
                if (isPassword) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onPaste) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(20.dp))
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

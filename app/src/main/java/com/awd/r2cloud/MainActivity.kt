package com.awd.r2cloud

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.awd.r2cloud.data.worker.BackupManager
import com.awd.r2cloud.domain.repository.AppTheme
import com.awd.r2cloud.domain.repository.R2Repository
import com.awd.r2cloud.ui.navigation.NavGraph
import com.awd.r2cloud.ui.theme.AwdR2CloudTheme
import com.awd.r2cloud.ui.viewmodel.PreferencesViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: R2Repository

    @Inject
    lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        backupManager.startMonitoring()
        
        if (repository.isAppLockEnabled()) {
            showBiometricPrompt()
        } else {
            startApp()
        }
    }

    private fun startApp() {
        enableEdgeToEdge()
        
        val startDestination = if (repository.getActiveAccount() != null) {
            "buckets"
        } else {
            "login"
        }

        setContent {
            val preferencesViewModel: PreferencesViewModel = hiltViewModel()
            val currentTheme by preferencesViewModel.theme.collectAsState()
            val currentLanguage by preferencesViewModel.language.collectAsState()

            // Handle Locale Change
            LaunchedEffect(currentLanguage) {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(currentLanguage.code)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }

            // Sync AppCompatDelegate with App Theme Choice
            LaunchedEffect(currentTheme) {
                val mode = when (currentTheme) {
                    AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }

            AwdR2CloudTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        repository = repository,
                        backupManager = backupManager,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    finish() // Close app on error
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startApp()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_lock_title))
            .setSubtitle(getString(R.string.app_lock_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

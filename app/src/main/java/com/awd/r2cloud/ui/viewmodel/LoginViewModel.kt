package com.awd.r2cloud.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awd.r2cloud.R
import com.awd.r2cloud.domain.model.R2Credentials
import com.awd.r2cloud.domain.repository.R2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: R2Repository
) : ViewModel() {

    var accountName by mutableStateOf("")
    var accountId by mutableStateOf("")
    var accessKeyId by mutableStateOf("")
    var secretAccessKey by mutableStateOf("")
    var apiToken by mutableStateOf("")

    val isFormValid: Boolean
        get() = accountName.isNotBlank() && 
                accountId.isNotBlank() && 
                accessKeyId.isNotBlank() && 
                secretAccessKey.isNotBlank()

    var isLoading by mutableStateOf(false)
    var errorResId by mutableStateOf<Int?>(null)
    var isSuccess by mutableStateOf(false)

    fun onConnectClicked() {
        if (!isFormValid) {
            errorResId = R.string.fill_required
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorResId = null
            
            val credentials = R2Credentials(
                alias = accountName.trim(),
                accountId = accountId.trim(),
                accessKeyId = accessKeyId.trim(),
                secretAccessKey = secretAccessKey.trim(),
                apiToken = apiToken.trim().ifBlank { null }
            )
            val isValid = repository.validateCredentials(credentials)
            
            if (isValid) {
                repository.saveAccount(credentials)
                isSuccess = true
            } else {
                errorResId = R.string.invalid_credentials
            }
            isLoading = false
        }
    }
}

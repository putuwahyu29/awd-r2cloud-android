package com.awd.r2cloud.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.awd.r2cloud.domain.model.R2Credentials
import com.awd.r2cloud.domain.repository.R2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: R2Repository
) : ViewModel() {

    var accounts by mutableStateOf(emptyList<R2Credentials>())
        private set

    var activeAccountId by mutableStateOf<String?>(null)
        private set

    init {
        refreshAccounts()
    }

    fun refreshAccounts() {
        accounts = repository.getAllAccounts()
        activeAccountId = repository.getActiveAccount()?.accountId
    }

    fun switchAccount(accountId: String) {
        repository.setActiveAccount(accountId)
        refreshAccounts()
    }

    fun removeAccount(accountId: String) {
        repository.removeAccount(accountId)
        refreshAccounts()
    }
}

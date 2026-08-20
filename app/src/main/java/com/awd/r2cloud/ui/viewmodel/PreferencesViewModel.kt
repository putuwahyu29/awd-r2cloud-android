package com.awd.r2cloud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awd.r2cloud.domain.repository.AppLanguage
import com.awd.r2cloud.domain.repository.AppTheme
import com.awd.r2cloud.domain.repository.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    val theme = preferenceRepository.theme.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM
    )

    val language = preferenceRepository.language.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH
    )

    fun setTheme(theme: AppTheme) {
        preferenceRepository.setTheme(theme)
    }

    fun setLanguage(language: AppLanguage) {
        preferenceRepository.setLanguage(language)
    }
}

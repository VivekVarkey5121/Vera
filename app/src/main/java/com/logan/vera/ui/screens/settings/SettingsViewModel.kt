package com.logan.vera.ui.screens.settings

import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import com.logan.vera.ui.theme.ReaderTheme
import com.logan.vera.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import com.logan.vera.utils.TimerLock
import android.content.Context 

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val fontSize: Float = 16f,
    val selectedFont: FontFamily,
    val selectedTheme: ReaderTheme,
    val readLimitMins: Int = 15,
    val lockDurationMins: Int = 5,
    val forceLockMins: Int = 15

)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isDarkMode = preferencesManager.isDarkMode,
            fontSize = preferencesManager.fontSize,
            selectedFont = preferencesManager.fontFamily,
            selectedTheme = preferencesManager.theme,

            readLimitMins = TimerLock.getReadLimitMins(context),
            lockDurationMins = TimerLock.getLockDurationMins(context),
            forceLockMins = TimerLock.getForceLockMins(context),
            // dailyLockMins = TimerLock.getDailyLockMins(context)
        )
    )
    val uiState = _uiState.asStateFlow()

    fun toggleDarkMode() {
        val newValue = !_uiState.value.isDarkMode
        preferencesManager.isDarkMode = newValue
        _uiState.value = _uiState.value.copy(isDarkMode = newValue)
    }

    fun updateFontSize(size: Float) {
        preferencesManager.fontSize = size
        _uiState.value = _uiState.value.copy(fontSize = size)
    }

    fun updateFont(font: FontFamily) {
        preferencesManager.fontFamily = font
        _uiState.value = _uiState.value.copy(selectedFont = font)
    }

    fun updateTheme(theme: ReaderTheme) {
        preferencesManager.theme = theme
        _uiState.value = _uiState.value.copy(selectedTheme = theme)
    }

    fun updateReadLimit(mins: Int) {
        TimerLock.setReadLimitMins(context, mins)
        _uiState.value = _uiState.value.copy(readLimitMins = mins)
    }

    fun updateLockDuration(mins: Int) {
        TimerLock.setLockDurationMins(context, mins)
        _uiState.value = _uiState.value.copy(lockDurationMins = mins)
    }

    fun updateForceLock(mins: Int) {
        TimerLock.setForceLockMins(context, mins)
        _uiState.value = _uiState.value.copy(forceLockMins = mins)
    }

    //fun updateDailyLock(mins: Int) {
        //TimerLock.setDailyLockMins(context, mins)
        //_uiState.value = _uiState.value.copy(dailyLockMins = mins)
    //}
}

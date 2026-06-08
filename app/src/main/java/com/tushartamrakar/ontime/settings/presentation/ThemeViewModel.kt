package com.tushartamrakar.ontime.settings.presentation

import androidx.lifecycle.ViewModel
import com.tushartamrakar.ontime.core.ui.theme.ThemeMode
import com.tushartamrakar.ontime.core.ui.theme.ThemePreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes [ThemePreferenceManager.themeMode] to the UI layer.
 *
 * Because [ThemePreferenceManager] is @Singleton, every instance of this
 * ViewModel (MainActivity's `by viewModels()` AND AppSettingsScreen's
 * `hiltViewModel()`) reads from and writes to the SAME StateFlow.
 * Selecting a theme in Settings is instantly reflected at the Activity level
 * with no re-launch needed.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferenceManager.themeMode

    fun setThemeMode(mode: ThemeMode) {
        themePreferenceManager.setThemeMode(mode)
    }
}

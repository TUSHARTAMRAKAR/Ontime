package com.tushartamrakar.ontime.core.ui.theme

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the user's chosen ThemeMode across app restarts.
 * Lives as a @Singleton — any number of ViewModels can inject it and they ALL
 * share the same StateFlow. When AppSettingsScreen changes the theme, the
 * identical flow in MainActivity's ThemeViewModel immediately reflects it,
 * giving instant live theme switching without any Activity restart.
 */
@Singleton
class ThemePreferenceManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadSaved())

    /** Emit to this flow to observe theme changes anywhere in the app. */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadSaved(): ThemeMode {
        val saved = prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.DARK
    }

    companion object {
        private const val PREFS_NAME = "ontime_prefs"
        private const val KEY_THEME  = "theme_mode"
    }
}

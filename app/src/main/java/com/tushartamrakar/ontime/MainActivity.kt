package com.tushartamrakar.ontime

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.tushartamrakar.ontime.core.navigation.DeepLinkHandler
import com.tushartamrakar.ontime.core.ui.theme.LocalOntimeColors
import com.tushartamrakar.ontime.core.ui.theme.OntimeTheme
import com.tushartamrakar.ontime.settings.presentation.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    // FragmentActivity (not AppCompatActivity) — required by BiometricPrompt.
    // AppCompatActivity requires Theme.AppCompat → crash. FragmentActivity has no such requirement.

    // Activity-scoped ThemeViewModel. ThemePreferenceManager is @Singleton, so
    // the same StateFlow is shared with AppSettingsScreen's ThemeViewModel instance —
    // theme changes are reflected here instantly without any Activity restart.
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()

            OntimeTheme(themeMode = themeMode) {
                // Use LocalOntimeColors so the Surface background is theme-aware.
                val colors = LocalOntimeColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = colors.background,
                ) {
                    // Pass the launching Intent so OntimeApp can read the
                    // deep link route on cold start.
                    OntimeApp(launchIntent = intent)
                }
            }
        }
    }

    /**
     * Called when a notification is tapped while the app is already running
     * (because FLAG_ACTIVITY_SINGLE_TOP is set on notification intents).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinkHandler.emit(intent)
    }
}

package com.tushartamrakar.ontime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.OntimeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OntimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background,
                ) {
                    OntimeApp()
                }
            }
        }
    }
}
package com.tushartamrakar.ontime.alarm.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary

@Composable
fun AlarmsScreen(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "⏰ Alarms",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}
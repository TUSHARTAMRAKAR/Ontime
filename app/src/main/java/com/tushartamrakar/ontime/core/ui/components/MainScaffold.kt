package com.tushartamrakar.ontime.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.navigation.Screen

// ─── Tab Item ─────────────────────────────────────────────────────────────────
data class TabItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

val bottomTabs = listOf(
    TabItem(Screen.Home, "Home", Icons.Filled.Home),
    TabItem(Screen.Alarms, "Alarms", Icons.Filled.AccessAlarm),
    TabItem(Screen.Calendar, "Calendar", Icons.Filled.CalendarMonth),
    TabItem(Screen.Focus, "Focus", Icons.Filled.Timer),
    TabItem(Screen.Profile, "Profile", Icons.Filled.Person),
)

// ─── Main Scaffold ────────────────────────────────────────────────────────────
@Composable
fun MainScaffold(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
                tonalElevation = androidx.compose.ui.unit.Dp(0f),
            ) {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.screen.route,
                        onClick = {
                            navController.navigate(tab.screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(text = tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Surface,
                        ),
                    )
                }
            }
        },
        content = content,
    )
}
package com.tushartamrakar.ontime

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.tushartamrakar.ontime.navigation.OntimeNavGraph

@Composable
fun OntimeApp() {
    val navController = rememberNavController()
    OntimeNavGraph(navController = navController)
}
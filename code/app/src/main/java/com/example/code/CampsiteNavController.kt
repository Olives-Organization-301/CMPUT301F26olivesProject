package com.example.code

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun CampsiteNavController() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "searchScreen") {
        // MAIN ENTRY POINT

        // ENTRANT SCREENS
        composable("searchScreen") { SearchScreen(navController) }
        composable("campsiteDetails") { CampsiteDetailsScreen(navController) }

        // CAMPSITE OWNER SCREENS


        // ADMIN SCREENS
    }
}
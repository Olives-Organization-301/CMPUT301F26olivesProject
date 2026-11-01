package com.example.code

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun CampsiteNavController() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "profileScreen") {
        // MAIN ENTRY POINT

        // ENTRANT SCREENS
        composable("mainSearchScreen") { MainSearchScreen(navController) }
        composable("campsiteDetails") { CampsiteDetailsScreen(navController) }
        composable("reservationsListScreen") { ReservationsListScreen(navController) }
        composable("profileScreen") { ProfileScreen(navController) }
        /* TODO: There should be only 4 Entrants Screens.
            QR Screen, Map Screen, Date Selections, Edit Profile, Policy and Filter Settings should
            be Popups instead when toggled.
            I Want Review Screen to be integrated with Entrants
            Also (I hope y'all don't mind) I want the screens to be named based on actors as well
            Also sorry for these comments, they should be centralized in a Wiki but
            I am in the zone right now
        */

        // CAMPSITE OWNER SCREENS


        // ADMIN SCREENS

    }
}
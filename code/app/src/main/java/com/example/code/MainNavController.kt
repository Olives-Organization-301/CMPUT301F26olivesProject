package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.code.classes.Campsite

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteNavController(
    campsiteDatabase: MutableList<Campsite>,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "loginScreen") {
        // MAIN ENTRY POINTs
        composable("loginScreen") { LoginScreen(navController) }

        // ENTRANT SCREENS
        composable("mainSearchScreen") { MainSearchScreen(navController, campsiteDatabase) }
        composable("campsiteDetails") { CampsiteDetailsScreen(navController, campsiteDatabase, "1") }
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
        composable("campsiteOwnerMainScreen"){campsiteOwnerMainScreen(navController)}

        // ADMIN SCREENS
        composable("AdminMainSearchScreen"){AdminMainSearchScreen(navController)}

    }
}
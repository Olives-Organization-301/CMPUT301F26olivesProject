package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteNavController(
    appState: AppState
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "loginScreen") {
        // MAIN ENTRY POINTs
        composable("loginScreen") { LoginScreen(navController) }

        // ENTRANT SCREENS
        composable("mainSearchScreen") { MainSearchScreen(
            navController, appState,
            onUpdateCampsiteId = { appState.viewingCampsiteID = it }
        ) }
        composable("campsiteDetails") { CampsiteDetailsScreen(navController, appState) }
        composable("reservationsListScreen") { ReservationsListScreen(navController) }
        composable("profileScreen") { ProfileScreen(navController) }
        composable("entrantAlerts") {EntrantAlertsScreen(navController)}

        // CAMPSITE OWNER SCREENS
        composable("campsiteOwnerLogin") { CampsiteOwnerLoginScreen(navController) }
        composable("campsiteOwnerMainScreen"){ CampsiteOwnerMainScreen(navController) }
        composable("campsiteChat"){CampsiteChatScreen(navController)}
        // ADMIN SCREENS

    }
}
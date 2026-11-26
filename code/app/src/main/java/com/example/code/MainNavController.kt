package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.code.classes.AppState

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteNavController(
    appState: AppState,
    startDestination: String = "loginScreen", // tests can start on a different screen
    navController: NavHostController = rememberNavController() // tests can pass their own to navigate
) {

    NavHost(navController = navController, startDestination = startDestination) {
        // MAIN ENTRY POINTs
        composable("loginScreen") { LoginScreen(navController, appState) }

        // ENTRANT SCREENS
        composable("mainSearchScreen") { MainSearchScreen(
            navController, appState,
            onUpdateCampsiteId = { appState.viewingCampsiteID = it }
        ) }
        composable("campsiteDetails") { CampsiteDetailsScreen(navController, appState) }
        composable("reservationsListScreen") { ReservationsListScreen(navController, appState) }
        composable("profileScreen") { ProfileScreen(navController, appState) }
        composable("entrantAlerts") { EntrantAlertsScreen(navController, appState)}
        composable("entrantMessages") { EntrantMessagesScreen(navController, appState) }
        // Opens Messages straight into one campsite's chat (e.g. from the Reservations list)
        composable(
            "entrantMessages/{campsiteID}",
            arguments = listOf(navArgument("campsiteID") { type = NavType.StringType })
        ) { entry ->
            EntrantMessagesScreen(
                navController, appState,
                initialCampsiteID = entry.arguments?.getString("campsiteID")
            )
        }

        // LOGIN SCREENS
        // "?signUp=true" opens the screen in sign-up mode; plain "entrantLogin" etc. still works (defaults to login)
        composable(
            ENTRANT_LOGIN_ROUTE,
            arguments = listOf(signUpArg())
        ) { entry ->
            EntrantLoginScreen(navController, appState, startInSignUp = entry.signUp())
        }
        composable(
            ADMIN_LOGIN_ROUTE,
            arguments = listOf(signUpArg())
        ) { entry ->
            AdminLoginScreen(navController, appState, startInSignUp = entry.signUp())
        }
        composable(
            OWNER_LOGIN_ROUTE,
            arguments = listOf(signUpArg())
        ) { entry ->
            CampsiteOwnerLoginScreen(navController, appState, startInSignUp = entry.signUp())
        }

        // CAMPSITE OWNER SCREENS
        composable("campsiteOwnerMainScreen") { CampsiteOwnerMainScreen(navController, appState) }
        composable("campsiteOwnerProfileScreen") { CampsiteOwnerProfileScreen(navController, appState) }
        composable("campsiteChat") { CampsiteChatScreen(navController) }
        // ADMIN SCREENS
        composable("AdminMainSearchScreen") { AdminMainSearchScreen(navController, appState) }
    }
}

// Full route patterns for the login screens. Use these in popUpTo(...): a plain "entrantLogin"
// no longer matches the destination once it has the optional ?signUp argument.
const val ENTRANT_LOGIN_ROUTE = "entrantLogin?signUp={signUp}"
const val ADMIN_LOGIN_ROUTE = "adminLogin?signUp={signUp}"
const val OWNER_LOGIN_ROUTE = "campsiteOwnerLogin?signUp={signUp}"

// Optional Boolean "signUp" argument (defaults to false = login mode)
private fun signUpArg() = navArgument("signUp") {
    type = NavType.BoolType
    defaultValue = false
}

private fun NavBackStackEntry.signUp(): Boolean = arguments?.getBoolean("signUp") ?: false

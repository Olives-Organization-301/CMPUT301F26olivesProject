package com.example.code.classes

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.code.DatabaseAdmins
import com.example.code.DatabaseAlerts
import com.example.code.DatabaseCampsiteOwners
import com.example.code.DatabaseCampsites
import com.example.code.DatabaseChat
import com.example.code.DatabaseEntrants

/** # App State
 *  Implementation of application state management.
 *
 *  Prototype was gained from a Conversation with Android Studio's agent
 *  Prompt: "Add DatabaseChat instance to AppState for real-time campsite owner messaging"
 *
 */
class AppState @RequiresApi(Build.VERSION_CODES.O) constructor(
    val admins: DatabaseAdmins = DatabaseAdmins(),
    val owners: DatabaseCampsiteOwners = DatabaseCampsiteOwners(),
    val campsites: DatabaseCampsites = DatabaseCampsites(),
    val entrants: DatabaseEntrants = DatabaseEntrants(),
    val alerts: DatabaseAlerts = DatabaseAlerts(),
    val chat: DatabaseChat = DatabaseChat()
) {

    // State so screens/effects (e.g. lottery notifications) react to logging in and out
    var userID: String? by mutableStateOf(null)
    var role: String? by mutableStateOf(null)
    var viewingCampsiteID: String? = null
}

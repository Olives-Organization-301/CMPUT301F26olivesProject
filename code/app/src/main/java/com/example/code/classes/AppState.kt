package com.example.code.classes

import com.example.code.DatabaseAdmins
import com.example.code.DatabaseAlerts
import com.example.code.DatabaseCampsiteOwners
import com.example.code.DatabaseCampsites
import com.example.code.DatabaseEntrants

// The databases are constructor parameters so tests can pass in stand-ins;
// the defaults keep the app using the real Firestore-backed ones.
class AppState(
    val admins: DatabaseAdmins = DatabaseAdmins(),
    val owners: DatabaseCampsiteOwners = DatabaseCampsiteOwners(),
    val campsites: DatabaseCampsites = DatabaseCampsites(),
    val entrants: DatabaseEntrants = DatabaseEntrants(),
    val alerts: DatabaseAlerts = DatabaseAlerts()
) {

    var userID: String? = null
    var role: String? = null
    var viewingCampsiteID: String? = null
}
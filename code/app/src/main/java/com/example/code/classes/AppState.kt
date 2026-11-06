package com.example.code.classes

import com.example.code.DatabaseAdmins
import com.example.code.DatabaseCampsiteOwners
import com.example.code.DatabaseCampsites
import com.example.code.DatabaseEntrants

class AppState {
    val admins = DatabaseAdmins()
    val owners = DatabaseCampsiteOwners()
    val campsites = DatabaseCampsites()
    val entrants = DatabaseEntrants()

    var userID: String? = null
    var role: String? = null
    var viewingCampsiteID: String? = null
}
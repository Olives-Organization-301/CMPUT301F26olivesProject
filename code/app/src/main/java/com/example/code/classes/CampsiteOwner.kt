package com.example.code.classes

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.code.DatabaseCampsites

/** CampsiteOwner Class used to represent the campsite Owner Actor
 */
class CampsiteOwner(
    username: String = "",
    email: String = "",
    userID: String = "",
    var phoneNumber: String = "",
    var description: String = "",
    var name: String = "",
    var dateOfBirth: String = "",
    var wantsNotifications: Boolean = true,
    var profileImage: String = "",
): User(username, email, userID) {

    // Have a list of Campsites
    private var campsites = mutableListOf<Campsite>()

    /**
     * Asynchronously fetches all campsites owned by this CampsiteOwner from Firestore database.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun viewCampsites(databaseCampsites: DatabaseCampsites, onResult: (List<Campsite>) -> Unit) {
        databaseCampsites.getCampsitesForOwner(getUserID(), onResult)
    }
}

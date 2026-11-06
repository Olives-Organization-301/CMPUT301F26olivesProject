package com.example.code.classes

import com.example.code.DatabaseCampsites

/** CampsiteOwner Class used to represent the campsite Owner Actor
 *
 */
class CampsiteOwner(
    username: String = "",
    email: String = "",
    userID: String = ""
): User(username, email, userID) {

    // Have a list of Campsites
    private var campsites = mutableListOf<Campsite>()

    /**
     * Asynchronously fetches all campsites owned by this CampsiteOwner from Firestore database.
     */
    fun viewCampsites(databaseCampsites: DatabaseCampsites, onResult: (List<Campsite>) -> Unit) {
        databaseCampsites.getCampsitesForOwner(getUserID(), onResult)
    }

}

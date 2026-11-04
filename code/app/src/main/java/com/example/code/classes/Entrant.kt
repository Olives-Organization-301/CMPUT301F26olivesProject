package com.example.code.classes

class Entrant(
    username: String,
    email: String,
    userID: String
): User(username, email, userID) {

    private var activeCampsites = mutableListOf<Campsite>()
    private var waitingListCampsites = mutableListOf<Campsite>()

    private var campsiteLists = mapOf<String, Campsite>()

    fun addActiveCampsite(campsite: Campsite) {
        activeCampsites.add(campsite)
    }

    fun addWaitingListCampsite(campsite: Campsite) {
        waitingListCampsites.add(campsite)
    }

    fun removeActiveCampsite() {

    }

}
package com.example.code.classes

class Entrant(
    username: String,
    email: String,
    userID: String
): User(username, email, userID) {

    // Use Maps so User can add their own Custom lists
    private var campsiteLists = mapOf<String, MutableList<String>>(
        "Active Campsites" to mutableListOf(),
        "Past Campsites" to mutableListOf()
    )

    fun addActiveCampsite(campsiteID: String) {
        campsiteLists["Active Campsite"]!!.add(campsiteID)
    }

    fun addWaitingListCampsite(campsiteID: String) {
        campsiteLists["Past Campsites"]!!.add(campsiteID)
    }

    fun removeActiveCampsite() {

    }

}
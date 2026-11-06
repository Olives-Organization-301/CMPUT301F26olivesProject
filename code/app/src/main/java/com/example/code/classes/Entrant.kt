package com.example.code.classes

class Entrant(
    username: String = "",
    email: String = "",
    userID: String = "",
    val phoneNumber: String = "",
    val description: String = "",
): User(username, email, userID) {

    // Use Maps so User can add their own Custom lists
    private var campsiteLists = mapOf<String, MutableList<String>>(
        "Active_Campsites" to mutableListOf(),
        "Past_Campsites" to mutableListOf(),
        "Waiting_List" to mutableListOf()
    )

    fun addCampsite(key: String, campsiteID: String) {
        if (key in campsiteLists.keys) {
            campsiteLists[key]!!.add(campsiteID)
        }
    }

    fun addNewList(newKey: String) {
        if (newKey in campsiteLists.keys) {

        }
    }

    fun removeCampsite(key: String, campsiteID: String) {
        if (key in campsiteLists.keys && campsiteID in campsiteLists[key]!!) {
            campsiteLists[key]!!.remove(campsiteID)
        }
    }
}
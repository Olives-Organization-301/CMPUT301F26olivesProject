package com.example.code.classes

/**
 * Represents abstract user; or the actors in general
 * Has a few get and set functions for usage in the ID
 */
abstract class User(
    var username: String = "",
    var email: String = "",
    private var userID: String = "",
) {
    fun getUserID(): String = userID
    fun setUserID(id: String) { userID = id }
}

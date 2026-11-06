package com.example.code.classes

/**
 * Represents abstract user class
 */
abstract class User(
    var username: String = "",
    var email: String = "",
    private var userID: String = "",
) {
    fun getUserID(): String = userID
    fun setUserID(id: String) { userID = id }
}

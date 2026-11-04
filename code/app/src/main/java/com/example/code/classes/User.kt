package com.example.code.classes

/**
 * Represents abstract user class
 *
 */
abstract class User(
    var username: String,
    var email: String,
    private val userID: String
) {
    fun getUserID(): String { return userID }

}
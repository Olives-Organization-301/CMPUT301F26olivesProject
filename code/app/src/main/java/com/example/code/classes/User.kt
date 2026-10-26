package com.example.code.classes

/**
 * Represents abstract user class
 *
 */
abstract class User(
    protected var username: String,
    private var email: String,
    private val userID: UInt
) {
    fun getUsername(): String = username
    fun getEmail(): String = email
    fun getUserID(): UInt = userID


}
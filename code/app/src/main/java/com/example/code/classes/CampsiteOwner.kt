package com.example.code.classes

class CampsiteOwner(
    username: String,
    email: String,
    userID: UInt
): User(username, email, userID) {

    private var campsites = mutableListOf<Campsite>()


}
package com.example.code.classes

class CampsiteOwner(
    username: String,
    email: String,
    userID: String
): User(username, email, userID) {

    private var campsites = mutableListOf<Campsite>()


}
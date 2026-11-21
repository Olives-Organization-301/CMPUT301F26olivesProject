package com.example.code.classes

/** # Chat Message Model
 *  Implementation of US 02.12.01: Campsite owner messaging a user who is reserving their campsite.
 *
 *  Prototype was gained from a Conversation with Android Studio's agent
 *  Prompt: "Create a ChatMessage data class to store sender, timestamp, text, campsite details, and message metadata"
 *
 */
data class ChatMessage(
    var messageID: String = "",
    var campsiteID: String = "",
    var campsiteName: String = "",
    var sender: String = "",
    var timestamp: String = "",
    var text: String = "",
    var fromOwner: Boolean = true,
    var entrantID: String = "",
    var ownerID: String = ""
)

package com.example.code.classes

/** # Chat Message Model
 *  Implementation of US 02.12.01: Campsite owner messaging a user who is reserving their campsite.
 *
 *  Prototype was gained from a Conversation with Android Studio's agent
 *  Prompt: "Create a ChatMessage data class to store sender, timestamp, text, campsite details, and message metadata"
 *
 * Modified by https://claude.ai/share/c834d783-5930-4800-8f67-da339fafedfd
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
    var ownerID: String = "",
    var automated: Boolean = false,        // true for messages the app sends (lottery results)
    var type: String = MESSAGE_TEXT,       // MESSAGE_TEXT, LOTTERY_WON or LOTTERY_LOST
    var locationIndex: Int = -1,           // LOTTERY_WON only: which location was won (0 = location 1)
    var sentAt: Long = System.currentTimeMillis()   // for ordering (timestamp is only for display)
)

// Kinds of chat messages
// A Normal Chat Message
const val MESSAGE_TEXT = "text"
const val LOTTERY_WON = "lottery_won"
const val LOTTERY_LOST = "lottery_lost"

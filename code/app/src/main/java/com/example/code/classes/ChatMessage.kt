package com.example.code.classes

import com.example.code.MESSAGE_TEXT

/** # Chat Message Model
 *  Implementation of US 02.12.01: Campsite owner messaging a user who is reserving their campsite.
 *
 *  Prototype was gained from a Conversation with Android Studio's agent
 *  Prompt: "Create a ChatMessage data class to store sender, timestamp, text, campsite details, and message metadata"
 *
 * Modified by https://claude.ai/share/c834d783-5930-4800-8f67-da339fafedfd (Gabe)
 *
 * @param messageID         identify message object
 * @param campsiteID        campsite entrant is messaging towards
 * @param campsiteName      campsite name
 * @param sender            who sent the message to whom
 * @param timestamp         log in time stamp
 * @param text              message contents
 * @param fromOwner         specify Owner sent it
 * @param entrantID         who the owner is messaging the student from (Cache ID)
 * @param ownerID           Cache ID of campsite owner
 * @param automated         (For notifications, and reserving dates) check if automated
 * @param type              differentiate between messages and telling the
 *                          entrant they won or lost the lottery
 * @param locationIndex     cache in location index for which location they won
 * @param sentAt            cache in when they sent the message
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
    var type: String = MESSAGE_TEXT,       // MESSAGE_TEXT, LOTTERY_WON, LOTTERY_LOST, RESERVATION_CONFIRMED or RESERVATION_DECLINED
    var locationIndex: Int = -1,           // LOTTERY_WON / RESERVATION_*: which location (0 = location 1)
    var sentAt: Long = System.currentTimeMillis(),  // for ordering (timestamp is only for display)
    var stayDates: List<String> = emptyList(),
    var lotteryID: String = "",
    var rulesAcknowledged: Boolean = false,
)

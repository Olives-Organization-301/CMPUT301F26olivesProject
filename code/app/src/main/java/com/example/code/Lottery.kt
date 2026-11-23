package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.LOTTERY_LOST
import com.example.code.classes.LOTTERY_WON

/** Draws [campsite]'s lottery, saves the result, and sends every entrant an automated message
 *  in their conversation with the campsite:
 *  - one "You won a place in location N!" message per location they won
 *  - one "you lost" message if they won nothing
 *  Does nothing if the lottery was already drawn.
 *  TODO: nothing calls this yet; call it when the lottery ends (e.g. an owner "Draw Lottery" button)
 *  @return true if the lottery was drawn
 */
@RequiresApi(Build.VERSION_CODES.O)
fun drawLottery(appState: AppState, campsite: Campsite, onSaved: (Boolean) -> Unit = {}): Boolean {
    if (campsite.isLotteryDrawn()) return false

    campsite.generateFinalList()
    appState.campsites.saveLotteryResults(campsite, onSaved)
    sendLotteryMessages(appState, campsite)
    return true
}

/** Sends every entrant on [campsite]'s waiting list their automated lottery result
 *  (uses the winners already stored in the campsite, so it can also be used with set results)
 *  https://claude.ai/share/c834d783-5930-4800-8f67-da339fafedfd
 */
fun sendLotteryMessages(appState: AppState, campsite: Campsite) {
    val name = campsite.getName()
    val now = System.currentTimeMillis()
    for (entrantID in campsite.getWaitingList()) {
        val won = campsite.getLocationsWon(entrantID)
        if (won.isEmpty()) {
            appState.chat.addMessage(
                lotteryMessage(campsite, entrantID, LOTTERY_LOST, -1, now,
                    "I'm Sorry, you lost the lottery for $name.")
            )
        } else {
            for (location in won) {
                appState.chat.addMessage(
                    // + location keeps several wins in location order
                    lotteryMessage(campsite, entrantID, LOTTERY_WON, location, now + location,
                        "Congratulations! You won a place in location ${location + 1} at $name. " +
                            "Use the button below to select your dates.")
                )
            }
        }
    }
}

// An automated message from [campsite] to [entrantID]
private fun lotteryMessage(
    campsite: Campsite,
    entrantID: String,
    type: String,
    locationIndex: Int,
    sentAt: Long,
    text: String
) = ChatMessage(
    campsiteID = campsite.campsiteID,
    campsiteName = campsite.getName(),
    sender = campsite.getName(),
    text = text,
    fromOwner = true,
    entrantID = entrantID,
    ownerID = campsite.getOwnerID(),
    automated = true,
    type = type,
    locationIndex = locationIndex,
    sentAt = sentAt
)

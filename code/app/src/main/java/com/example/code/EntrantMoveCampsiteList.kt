package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.Entrant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Constant Keys of the entrant's campsite lists (as stored in Firestore)
 * Used to subscribe to the three campsites
 * Convo: https://claude.ai/share/e282e67a-f3db-4225-b23d-1cadc614a63f (Cite later)
 */
const val ACTIVE_CAMPSITES = "Active_Campsites"
const val PAST_CAMPSITES = "Past_Campsites"
const val WAITING_LIST = "Waiting_List"

/** List key shown to the user: "Waiting_List" -> "Waiting List"
 * Display Keys without the underscore
 */
fun formatListName(key: String): String = key.replace('_', ' ')

/** Format Lottery date shown on cards: "November 20, 2026" ("Not set" if there is no date) */
@RequiresApi(Build.VERSION_CODES.O)
fun formatLotteryDate(date: LocalDateTime?): String =
    date?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())) ?: "Not set"

/** Once a campsite's lottery has started, moves it from the entrant's Waiting List to their
 *  Active Campsites list, and moves the entrant from the campsite's waitingList to its activeLotteryList.
 *  @return true if anything was moved
 */
@RequiresApi(Build.VERSION_CODES.O)
fun moveToActiveLotteryIfStarted(appState: AppState, entrant: Entrant, campsite: Campsite): Boolean {
    if (!campsite.isLotteryActive()) return false
    if (entrant.getCampsiteLists()[WAITING_LIST]?.contains(campsite.campsiteID) != true) return false
    appState.entrants.moveEntrantCampsite(entrant, WAITING_LIST, ACTIVE_CAMPSITES, campsite.campsiteID)
    appState.campsites.moveToActiveLottery(campsite, entrant.getUserID())
    return true
}

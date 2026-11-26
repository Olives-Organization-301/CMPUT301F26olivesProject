package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.Entrant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.LocalDateTime


/**
 *  ReservationListScreen: Where the Entrant can use past and present campsites, and delete them
 *  From Lists
 *  I used Sonnet 5.0 to integrate the backend and
 *  Prompt: "Can you make the GUI based on the given Image?"
 *  Conversation: https://claude.ai/share/4dfdf023-c5f7-4f56-b9f3-f9e8bb4d5756
 *
 *  Prompt: "Can you itnegrate the database with the frontend?"
 *  Conversation: https://claude.ai/share/b0990579-fe55-4946-828d-1830efc58118
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReservationsListScreen(
    navController: NavController,
    appState: AppState
) {

    val entrant by produceState<Entrant?>(
        initialValue = null,
        key1 = appState.userID
    ) {
        val id = appState.userID
        if (id == null) {
            value = null
        } else {
            appState.entrants.searchEntrants(id) { found ->
                value = found
            }
        }
    }

    val campsites: Map<String, MutableList<String>> = entrant?.getCampsiteLists() ?: emptyMap()

    var selectedList by remember { mutableStateOf("Active_Campsites") }
    var expanded by remember { mutableStateOf(false) }

    // Bumped whenever campsites move between lists, so the shown list refreshes
    var listVersion by remember { mutableIntStateOf(0) }

    // UI-observable copy of the currently selected list
    val displayedCampsites = remember(entrant, selectedList, listVersion) {
        mutableStateListOf<String>().apply {
            addAll(campsites[selectedList] ?: emptyList())
        }
    }

    val campsiteCache = remember { mutableStateMapOf<String, Campsite>() }

    // Once a waiting-list campsite's lottery has started, move it to the Active Campsites list
    // At launch, if there's anything on the waiting list whose lottery begun, it's active, so move it there.
    LaunchedEffect(entrant) {
        val e = entrant ?: return@LaunchedEffect
        e.getCampsiteLists()[WAITING_LIST].orEmpty().toList().forEach { id ->
            appState.campsites.searchCampsite(id) { found ->
                if (found != null) {
                    campsiteCache[id] = found
                    if (moveToActiveLotteryIfStarted(appState, e, found)) listVersion++
                }
            }
        }
    }

    // Launch a Coroutine at the start of the app in order to display campsites
    // Then you search campsites and see if you can find their info for displaying
    LaunchedEffect(displayedCampsites.toList()) {
        displayedCampsites
            .filter { it !in campsiteCache }
            .forEach { id ->
                appState.campsites.searchCampsite(id) { found ->
                    if (found != null) campsiteCache[id] = found
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        // To avoid clipping it into the top of the screen
        Spacer(Modifier.height(48.dp))

        // Header: title + close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatListName(selectedList),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "✕",
                fontSize = 24.sp,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Select Screens list
        // (Box is needed so the DropdownMenu anchors to the button)
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("Select Your List: ${formatListName(selectedList)}  ▾")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Use Lambda to iterate
                campsites.keys.forEach { list ->
                    DropdownMenuItem(
                        text = { Text(formatListName(list)) },
                        onClick = {
                            // TODO: Actually Switch between the Campsites
                            selectedList = list
                            expanded = false // Close menu after selection
                        }
                    )

                }
            }
        }


        // The Cards of Campsites
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(displayedCampsites, key = { it }) { campsite ->
                CampsiteCard(
                    campsiteID = campsite,
                    campsiteData = campsiteCache[campsite],
                    onClick = {
                        // Open this campsite's details screen
                        appState.viewingCampsiteID = campsite
                        navController.navigate("campsiteDetails")
                    },
                    onChat = {
                        // Open the chat between this entrant and this campsite
                        navController.navigate("entrantMessages/$campsite")
                    },
                    onRemove = {
                        // Remove the campsite from the entrant's own list
                        entrant?.let {
                            appState.entrants.removeEntrantCampsite(it, selectedList, campsite)
                        }
                        // If it's the waiting list or active lottery, also remove the entrant from the campsite
                        val userID = appState.userID
                        if ((selectedList == WAITING_LIST || selectedList == ACTIVE_CAMPSITES) && userID != null) {
                            campsiteCache[campsite]?.removeEntrant(userID)
                            appState.campsites.removeFromWaitingList(campsite, userID)
                        }
                        displayedCampsites.remove(campsite)
                    }
                )
            }
        }


    }


}

/** The Main Function that puts the Campsite Card
 * Used Sonnet 5.0 in order to get the card
 * Prompt: "Can you make the card its own function?"
 * Conversation: https://claude.ai/share/b0990579-fe55-4946-828d-1830efc58118
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteCard(
    campsiteID: String,
    campsiteData: Campsite?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onChat: () -> Unit = {}
) {
    // Per-card UI state (placeholder until the Database exists)
    var starred by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB0B0B0))
    ) {
        // Thumbnail fills the card (desaturated + black gradient), same as the search screen
        // Card has been replaced to accomodate an image in the background
        val thumbnail = campsiteThumbnailRes(campsiteData)
        CampsiteImageBackground(
            imageRes = thumbnail,
            fallback = Color(0xFFB0B0B0),
            modifier = Modifier.fillMaxSize()
        ) {
            // A Buffer to ensure text is coloured properly if thumbnail has loaded or not.
            // It will be white if an image exists, black if an image doesn't exist.
            CompositionLocalProvider(
                LocalContentColor provides if (thumbnail != null) Color.White else Color.Black
            ) {
                // Put text starts here
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = campsiteData?.getName() ?: campsiteID,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = campsiteData?.getLocationSummary() ?: "Loading…",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Start: ${formatLotteryDate(campsiteData?.getStartDate())}",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "End: ${formatLotteryDate(campsiteData?.getEndDate())}",
                            fontSize = 12.sp
                        )
                    }

                    // Action icons (remove / favorite / chat)
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 22.sp,
                            modifier = Modifier.clickable { onRemove() }
                        )
                        Text(
                            text = if (starred) "★" else "☆",
                            fontSize = 22.sp,
                            modifier = Modifier.clickable { starred = !starred }
                        )
                        // Chat: opens this campsite's conversation in Messages
                        Text(
                            text = "💬",
                            fontSize = 22.sp,
                            modifier = Modifier
                                .clickable { onChat() }
                                .semantics { contentDescription = "Message campsite" }
                        )
                    }
                }
            }
        }
    }
}

// ----- Entrant list keys + lottery helpers (moved from EntrantMoveCampsiteList.kt) -----

/**
 * Constant Keys of the entrant's campsite lists (as stored in Firestore)
 * Used to subscribe to the three campsites
 * Convo: https://claude.ai/share/e282e67a-f3db-4225-b23d-1cadc614a63f (Cite later)
 */
const val ACTIVE_CAMPSITES = "Active_Campsites"
const val PAST_CAMPSITES = "Past_Campsites"
const val RESERVED_CAMPSITES = "Reserved_Campsites" // Campsites the entrant actually reserved
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
 *  Active Campsites list. The entrant stays on the campsite's waitingList, which is the lottery pool.
 *  @return true if anything was moved
 */
@RequiresApi(Build.VERSION_CODES.O)
fun moveToActiveLotteryIfStarted(appState: AppState, entrant: Entrant, campsite: Campsite): Boolean {
    if (!campsite.isLotteryActive()) return false
    if (entrant.getCampsiteLists()[WAITING_LIST]?.contains(campsite.campsiteID) != true) return false
    appState.entrants.moveEntrantCampsite(entrant, WAITING_LIST, ACTIVE_CAMPSITES, campsite.campsiteID)
    return true
}

package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.Entrant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

// TODO: DELETE these colours. We want colour choices to be in ui.Theme
private val ScreenBackground = Color(0xFFFDF8F3)
private val ButtonPurple = Color(0xFF6750A4)

/** Campsite Details Screen
 * Show the Details of the Screen
 * I Used Sonnet 5.0 in order to make the basic GUI Structure
 * Prompt: "Can you make the GUI based on this screen?"
 * Conversation: https://claude.ai/share/2b8f9dba-e685-463b-b161-acccbf698b8a
 * Date: Nov 13, 2026
 * I used Sonnet 5.0 in order to integrate the backend
 * Prompt: "Can you integrate the backend with the GUI?"
 * Conversation: https://claude.ai/share/fbdd45b9-ed51-42b6-a072-85a9fb6f5d32
 * Date: Nov 13, 2026
 * I used Opus 5.5 In order add popups to indicate joining/leaving a list
 * Pormpt: "Can you add a few dialogue boxes for joining/leaving a lottery?"
 * Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
 * Date: Nov 20, 2026
 * Adding Image, extra widgets and other stuff comes here
 * Conversation: https://claude.ai/share/e282e67a-f3db-4225-b23d-1cadc614a63f
 * Date: Nov 20, 2026
 */
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteDetailsScreen(
    navController: NavController,
    appState: AppState,
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

    val chosenCampsite by produceState<Campsite?>(
        initialValue = null,
        key1 = appState.viewingCampsiteID
    ) {
        val id = appState.viewingCampsiteID
        if (id == null) {
            value = null
        } else {
            appState.campsites.searchCampsite(id) { found ->
                value = found
            }
        }
    }

    val campsite = chosenCampsite

    // Whether this entrant is already on the campsite's waiting list or active lottery
    var isJoined by remember(campsite, entrant) {
        val id = appState.userID
        val lists = entrant?.getCampsiteLists()
        mutableStateOf(
            campsite != null && id != null && (
                campsite.entrantInList(id) ||
                    lists?.get(WAITING_LIST)?.contains(campsite.campsiteID) == true ||
                    lists?.get(ACTIVE_CAMPSITES)?.contains(campsite.campsiteID) == true
                )
        )
    }
    // True while today is within the campsite's lottery dates
    val lotteryActive = campsite?.isLotteryActive() == true

    // Once the lottery has started, move this campsite from the entrant's Waiting List to Active Campsites
    // (movedToActiveLottery is state, so the button redraws as "Leave Active Lottery" after the move)
    var movedToActiveLottery by remember(campsite, entrant) { mutableStateOf(false) }
    LaunchedEffect(campsite, entrant) {
        val c = campsite
        val e = entrant
        if (c != null && e != null && moveToActiveLotteryIfStarted(appState, e, c)) {
            movedToActiveLottery = true
        }
    }

    /**
     * isSaving:                A Boolean that prevents a User requesting something to firebase
     *                          while it's doing something
     * showJoinDialog:          Shows Dialogue box of you joining
     * joinSucceeded:           Confirmation Boolean from Firestone to make sure Entrant's joining
     *                          is saved within the Firestone Database
     * showLeaveDialog:         Shows Leaving Dialogue
     * leaveSucceeded:          Ensures Firesstone DB has removed Entrant from the list
     * showDatesDialog:         Shows the Calendar for vacation Dates
     * joinedActiveLottery:     If lottery is active, check that firestone has cached joining it
     * leftActiveLottery:       If lottery is active, check that firestone has cached leaving it
     */
    var isSaving by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinSucceeded by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var leaveSucceeded by remember { mutableStateOf(false) }
    var showDatesDialog by remember { mutableStateOf(false) }
    var joinedActiveLottery by remember { mutableStateOf(false) }
    var leftActiveLottery by remember { mutableStateOf(false) }

    val owner by produceState<CampsiteOwner?>(
        initialValue = null,
        key1 = campsite?.getOwnerID()
    ) {
        val ownerID = campsite?.getOwnerID()
        if (ownerID == null) {
            value = null
        } else {
            appState.owners.searchCampsiteOwner(ownerID) { found ->
                value = found
            }
        }
    }

    val detailsImage = campsiteDetailsRes(campsite)
    // White text over the photo, black text on the plain background
    val textColor = if (detailsImage != null) Color.White else Color.Black
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Details", "Reviews")

    // Entire Interface
    // Photo background (desaturated + black gradient), or the plain background if there's no image
    CampsiteImageBackground(
        imageRes = detailsImage,
        fallback = ScreenBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        // Place to put contents after converting into an image
        CompositionLocalProvider(LocalContentColor provides textColor) {
            Column(modifier = Modifier.fillMaxSize()) {

                // To avoid clipping it into the top of the screen
                Spacer(Modifier.height(48.dp))

                // Header: centered title + location subtitle, exit button on the right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = campsite?.getName() ?: "Loading...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        // Country, Province, City (slightly smaller, greyer)
                        Text(
                            text = campsite?.let { locationText(it) } ?: "",
                            fontSize = 16.sp,
                            color = textColor.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Use IconButton in order to Make the X Icon
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(2.dp, textColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Borderless tabs: no container colour and no divider line
                // Tabs for reviews and details for you to switch in-between
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = textColor,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                            selectedContentColor = textColor,
                            unselectedContentColor = textColor.copy(alpha = 0.6f)
                        )
                    }
                }

                // Scrollable tab content (takes all space between the tabs and buttons)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    when (selectedTab) {
                        0 -> CampsiteDetailsTab(campsite, owner)
                        else -> CampsiteReviewsTab(campsite, textColor)
                    }
                }

                // Join / Leave Waiting List + possible dates
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val userID = appState.userID
                    val currentEntrant = entrant

                    if (campsite != null && userID != null && isJoined) {
                        // Which of the entrant's lists the campsite is on (the campsite itself only has a waitingList)
                        val inActiveLottery = movedToActiveLottery ||
                            currentEntrant?.getCampsiteLists()?.get(ACTIVE_CAMPSITES)
                                ?.contains(campsite.campsiteID) == true
                        // Button that handles you joining the active list or not
                        Button(
                            enabled = !isSaving,
                            onClick = {
                                isSaving = true
                                leftActiveLottery = inActiveLottery
                                val onLeft: (Boolean) -> Unit = { ok ->
                                    isSaving = false
                                    leaveSucceeded = ok
                                    if (ok) isJoined = false
                                    showLeaveDialog = true
                                }
                                appState.campsites.removeFromWaitingList(campsite, userID, onLeft)
                                if (currentEntrant != null) {
                                    appState.entrants.removeEntrantCampsite(
                                        currentEntrant,
                                        if (inActiveLottery) ACTIVE_CAMPSITES else WAITING_LIST,
                                        campsite.campsiteID
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonPurple)
                        ) {
                            Box(modifier = Modifier.size(18.dp).background(Color.White, CircleShape))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                if (inActiveLottery) "Leave Active Lottery" else "Leave Waiting List",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Button(
                            enabled = campsite != null && currentEntrant != null && userID != null && !isSaving,
                            onClick = {
                                if (campsite != null && userID != null && currentEntrant != null) {
                                    isSaving = true
                                    joinedActiveLottery = lotteryActive
                                    // 2. entrant doc: campsiteLists.<list> += campsiteID
                                    val addToEntrant: (Boolean) -> Unit = { campsiteOk ->
                                        appState.entrants.addEntrantCampsite(
                                            currentEntrant,
                                            if (lotteryActive) ACTIVE_CAMPSITES else WAITING_LIST,
                                            campsite.campsiteID
                                        ) { entrantOk ->
                                            isSaving = false
                                            joinSucceeded = campsiteOk && entrantOk
                                            if (joinSucceeded) isJoined = true
                                            showJoinDialog = true
                                        }
                                    }
                                    // 1. campsite doc: waitingList += entrantID (same list before or during the lottery)
                                    appState.campsites.addToWaitingList(campsite, userID, addToEntrant)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonPurple)
                        ) {
                            Box(modifier = Modifier.size(18.dp).background(Color.White, CircleShape))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                when {
                                    isSaving -> "Joining…"
                                    lotteryActive -> "Join Active Lottery"
                                    else -> "Join Waiting List"
                                },
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Possible dates for this campsite
                    OutlinedButton(
                        enabled = campsite != null,
                        onClick = { showDatesDialog = true },
                        border = BorderStroke(1.dp, textColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Dates", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Confirmation popup after pressing "Join Waiting List"
    if (showJoinDialog) {
        JoinWaitingListDialog(
            success = joinSucceeded,
            campsiteName = campsite?.getName(),
            activeLottery = joinedActiveLottery,
            onDismiss = { showJoinDialog = false }
        )
    }

    // Calendar of available dates after pressing "Dates"
    if (showDatesDialog) {
        AvailableDatesDialog(
            ranges = campsite?.getVacationStartTimes().orEmpty(),
            onDismiss = { showDatesDialog = false }
        )
    }

    // Confirmation popup after pressing "Leave Waiting List"
    if (showLeaveDialog) {
        LeaveWaitingListDialog(
            success = leaveSucceeded,
            campsiteName = campsite?.getName(),
            activeLottery = leftActiveLottery,
            onDismiss = { showLeaveDialog = false }
        )
    }
}

// TODO: Cite Properly https://claude.ai/share/e282e67a-f3db-4225-b23d-1cadc614a63f
/** First location as "Country, Province, City", plus how many other locations there are */
private fun locationText(campsite: Campsite): String = campsite.getLocationSummary()

/** Details tab: owner, lottery dates, rating, occupancy, location and description */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CampsiteDetailsTab(campsite: Campsite?, owner: CampsiteOwner?) {
    // This is where the DETAILS are
    Text(owner?.username ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Text(
        "Lottery Begin Date: ${campsite?.getStartDate()?.toLocalDate() ?: ""}",
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    )
    Text(
        "Lottery End Date: ${campsite?.getEndDate()?.toLocalDate() ?: ""}",
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    )
    Text(
        "Rating: ${campsite?.getRating()?.let { "$it/5" } ?: ""}",
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    )
    Text(
        "Max Occupancy: ${campsite?.getTotalOccupancy() ?: ""}",
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    )
    // Display one or multiple locations
    // One location: a single line. Several: each location with its own max occupancy
    val locations = campsite?.getCoordinates().orEmpty()
    if (locations.size <= 1) {
        Text(
            "Location: ${locations.firstOrNull()?.getLocationText() ?: ""}",
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp
        )
    } else {
        Text("Locations:", fontWeight = FontWeight.Medium, fontSize = 18.sp)
        locations.forEachIndexed { index, location ->
            Text(
                "• ${location.getLocationText()} (up to ${campsite?.getOccupancyAt(index) ?: 0} people)",
                fontSize = 16.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text("Description:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Text(
        text = campsite?.getDescription() ?: "",
        fontSize = 16.sp
    )
}

/** Reviews tab: one card per review, or a message if there are none yet */
@Composable
private fun CampsiteReviewsTab(campsite: Campsite?, textColor: Color) {
    // This is where the REVIEWS are
    val reviews = campsite?.getReviews()?.entries?.toList().orEmpty()
    if (reviews.isEmpty()) {
        Text("No reviews yet.", fontSize = 16.sp, color = textColor.copy(alpha = 0.75f))
        return
    }
    // Iterate through review and reply
    reviews.forEach { (review, replies) ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .background(textColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            // TODO: reviews are stored as IDs now; fetch the Review object by ID before showing these
            // // Ratings are stored out of 10 and shown out of 5
            // Text(
            //     "★ ${review.rating / 2f}/5${if (review.isEdited) "  (edited)" else ""}",
            //     fontWeight = FontWeight.Bold,
            //     fontSize = 16.sp
            // )
            // Text(review.review, fontSize = 16.sp)
            // Text(
            //     "${review.calculateTotalUpvote()} votes · ${replies.size} replies",
            //     fontSize = 13.sp,
            //     color = textColor.copy(alpha = 0.7f)
            // )
        }
    }
}

/** Colour behind the dates a campsite is available (light blue) */
// TODO: Move this to Color UI for consistent themes
internal val CalendarHighlight = Color(0xFFBBDEFB)

/** Test hook: true when a calendar day is drawn highlighted */
val DayHighlightedKey = SemanticsPropertyKey<Boolean>("DayHighlighted")
var SemanticsPropertyReceiver.dayHighlighted by DayHighlightedKey

/** True if [date] falls inside any start/end pair (both ends included) */
@RequiresApi(Build.VERSION_CODES.O)
fun isDateAvailable(date: LocalDate, ranges: List<Pair<LocalDateTime, LocalDateTime>>): Boolean =
    ranges.any { (start, end) ->
        !date.isBefore(start.toLocalDate()) && !date.isAfter(end.toLocalDate())
    }

/** Popup that shows the campsite's available dates on a calendar */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AvailableDatesDialog(
    ranges: List<Pair<LocalDateTime, LocalDateTime>>,
    onDismiss: () -> Unit
) {
    // Showcase available dates through popup calendar
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Available Dates") },
        text = {
            Column {
                if (ranges.isEmpty()) {
                    Text("No dates have been set for this campsite yet.", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                }
                AvailableDatesCalendar(ranges)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

/** Month calendar. Every start/end pair is drawn as a rounded light-blue band with black text.
 *  Opens on the month of the earliest pair (or the current month if there are none).
 *  Each day has the test tag "calendarDay_<yyyy-MM-dd>".
 *  TLDR: The function that colours the Dates
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AvailableDatesCalendar(
    ranges: List<Pair<LocalDateTime, LocalDateTime>>,
    modifier: Modifier = Modifier,
    initialMonth: YearMonth = ranges.minOfOrNull { it.first }?.let { YearMonth.from(it) } ?: YearMonth.now()
) {
    var month by remember { mutableStateOf(initialMonth) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Month header: ‹ November 2026 ›
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrease Month
            TextButton(
                onClick = { month = month.minusMonths(1) },
                modifier = Modifier.testTag("calendarPrevMonth")
            ) { Text("‹", fontSize = 20.sp) }
            // Display Month Name (Month, Year)
            Text(
                text = "${month.month.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale)} ${month.year}",
                modifier = Modifier
                    .weight(1f)
                    .testTag("calendarMonthTitle"),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            // Increase Month
            TextButton(
                onClick = { month = month.plusMonths(1) },
                modifier = Modifier.testTag("calendarNextMonth")
            ) { Text("›", fontSize = 20.sp) }
        }

        // Weekday labels (weeks start on Sunday)
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Day grid: Insert Month names in proper place
        // Get Month's Length

        val leadingBlanks = month.atDay(1).dayOfWeek.value % 7   // Sunday = 0
        val daysInMonth = month.lengthOfMonth()
        val rows = (leadingBlanks + daysInMonth + 6) / 7

        // Iterate through the months, displaying their day
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - leadingBlanks + 1
                    if (day in 1..daysInMonth) {
                        // Handle colouring if they're reserved or not
                        CalendarDay(
                            date = month.atDay(day),
                            ranges = ranges,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(40.dp))
                    }
                }
            }
        }
    }
}

/** One day cell. Highlighted days join into a band; the band is rounded where a pair
 *  starts or ends (and at the edges of a week or month so it doesn't look cut off). */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarDay(
    date: LocalDate,
    ranges: List<Pair<LocalDateTime, LocalDateTime>>,
    modifier: Modifier = Modifier
) {
    /**
     * highlighted      - use function to see if it's not in the month in question or not
     * roundStart       - Get date range start
     * roundEnd         - Get date range end
     * startRadius      - Colour in the radius
     * endRadius        - Colour the radius end
     * shape            - Get the rounded Corner for the highlighter
     */
    val highlighted = isDateAvailable(date, ranges)
    val roundStart = !isDateAvailable(date.minusDays(1), ranges) ||
        date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfMonth == 1
    val roundEnd = !isDateAvailable(date.plusDays(1), ranges) ||
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfMonth == date.lengthOfMonth()
    val startRadius = if (roundStart) 16.dp else 0.dp
    val endRadius = if (roundEnd) 16.dp else 0.dp
    val shape = RoundedCornerShape(
        topStart = startRadius, bottomStart = startRadius,
        topEnd = endRadius, bottomEnd = endRadius
    )

    // If it's an available spot, the Box colours in the dates
    Box(
        modifier = modifier
            .height(40.dp)
            .padding(vertical = 2.dp)
            .then(if (highlighted) Modifier.background(CalendarHighlight, shape) else Modifier)
            .testTag("calendarDay_$date")
            .semantics { dayHighlighted = highlighted },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (highlighted) Color.Black else LocalContentColor.current.copy(alpha = 0.5f)
        )
    }
}

/** Popup shown after an entrant tries to join a campsite's waiting list.
 * @param success: whether both database writes succeeded
 * @param campsiteName: name shown in the message (falls back to "this campsite")
 * @param onDismiss: called when OK is pressed or the dialog is dismissed
 * I used Opus 5.5 In order add popups to indicate joining/leaving a list
 * Pormpt: "Can you add a few dialogue boxes for joining/leaving a lottery?"
 * Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
 * Date: Nov 20, 2026
 */
@Composable
fun JoinWaitingListDialog(
    success: Boolean,
    campsiteName: String?,
    onDismiss: () -> Unit,
    activeLottery: Boolean = false   // true when joining the active lottery instead of the waiting list
) {
    val listName = if (activeLottery) "active lottery" else "waiting list"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    !success -> "Couldn't join"
                    activeLottery -> "You're in the active lottery!"
                    else -> "You're on the waiting list!"
                }
            )
        },
        text = {
            Text(
                if (success)
                    "You've joined the $listName for ${campsiteName ?: "this campsite"}. " +
                        "You'll be notified if you're selected in the lottery."
                else
                    "Something went wrong while joining the $listName. Please try again."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

/** Popup shown after an entrant tries to leave a campsite's waiting list.
 * @param success: whether removing the entrant from the waiting list succeeded
 * @param campsiteName: name shown in the message (falls back to "this campsite")
 * @param onDismiss: called when OK is pressed or the dialog is dismissed
 * I used Opus 5.5 In order add popups to indicate joining/leaving a list
 * Pormpt: "Can you add a few dialogue boxes for joining/leaving a lottery?"
 * Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
 * Date: Nov 20, 2026
 */
@Composable
fun LeaveWaitingListDialog(
    success: Boolean,
    campsiteName: String?,
    onDismiss: () -> Unit,
    activeLottery: Boolean = false   // true when leaving the active lottery instead of the waiting list
) {
    val listName = if (activeLottery) "active lottery" else "waiting list"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (success) "You've left the $listName" else "Couldn't leave") },
        text = {
            Text(
                if (success)
                    "You've been removed from the $listName for ${campsiteName ?: "this campsite"}. " +
                        "You can join again at any time before the lottery closes."
                else
                    "Something went wrong while leaving the $listName. Please try again."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

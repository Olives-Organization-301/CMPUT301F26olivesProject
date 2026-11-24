package com.example.code

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.Entrant
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/** Messages Screen
 * Shows the entrant's conversations with campsites (one per campsite). Opening one shows the
 * chat: messages from the campsite (its owner, or automated ones like lottery results) on the
 * left, the entrant's own on the right, and a text box to reply.
 * A "You won" lottery message has a Select Dates button that opens the campsite's calendar.
 * Date: November 22, 2026
 * Prompt: Make a message board for me
 * Conversation: https://claude.ai/share/c834d783-5930-4800-8f67-da339fafedfd (All of it, the functions as well)
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EntrantMessagesScreen(navController: NavController, appState: AppState) {
    // Initialize UserID
    val userID = appState.userID

    // Query ID to make Entrant Objects
    val entrant by produceState<Entrant?>(initialValue = null, key1 = userID) {
        if (userID == null) value = null
        else appState.entrants.searchEntrants(userID) { found -> value = found }
    }

    // campsiteID -> its messages; the most recently active conversation first
    val conversations = if (userID == null) emptyMap() else appState.chat.conversationsFor(userID)

    // Campsites the entrant can message: everything on their Waiting List, Active Campsites
    // and Reserved Campsites (plus any campsite that has already messaged them)
    val listedIDs = entrant?.getCampsiteLists()?.let { lists ->
        listOf(WAITING_LIST, ACTIVE_CAMPSITES, RESERVED_CAMPSITES).flatMap { lists[it].orEmpty() }
    }.orEmpty().distinct()

    // Names of listed campsites that have no messages yet (looked up once)
    val campsiteNames = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(listedIDs) {
        listedIDs.filter { it !in conversations && it !in campsiteNames }.forEach { id ->
            appState.campsites.searchCampsite(id) { found ->
                if (found != null) campsiteNames[id] = found.getName()
            }
        }
    }

    // Conversations with messages first (most recent first), then the rest by name
    val summaries =
        conversations.map { (id, msgs) -> ConversationSummary(id, msgs.last().campsiteName, msgs.last()) } +
            listedIDs.filter { it !in conversations }
                .map { id -> ConversationSummary(id, campsiteNames[id].orEmpty(), null) }
                .sortedBy { it.campsiteName }

    // Conversation that is open (null = showing the list)
    var openCampsiteID by remember { mutableStateOf<String?>(null) }

    val openID = openCampsiteID
    if (openID == null || userID == null) {
        // Open Messaging for Conversations
        ConversationList(
            conversations = summaries,
            onOpen = { openCampsiteID = it },
            onClose = { navController.popBackStack() }
        )
    } else {
        // Phone back button returns to the list instead of leaving the screen
        // Open a Chat between Entrant and Campsite Owner
        BackHandler { openCampsiteID = null }
        ChatThread(
            appState = appState,
            campsiteID = openID,
            entrantID = userID,
            entrant = entrant,
            messages = appState.chat.conversation(openID, userID),
            onBack = { openCampsiteID = null }
        )
    }
}

/** One row in the conversation list. [latest] is null if nobody has messaged yet. */
private data class ConversationSummary(
    val campsiteID: String,
    val campsiteName: String,
    val latest: ChatMessage?
)

/** The list of conversations: campsite name and its latest message */
@Composable
private fun ConversationList(
    conversations: List<ConversationSummary>,
    onOpen: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(15.dp)) {
        // To avoid clipping it into the top of the screen
        Spacer(Modifier.height(48.dp))

        // Header: title + close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Messages",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "✕",
                fontSize = 24.sp,
                modifier = Modifier.clickable { onClose() }
            )
        }

        if (conversations.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "No campsites to message yet. Join a campsite's waiting list to message it.",
                color = LocalContentColor.current.copy(alpha = 0.6f)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(conversations, key = { it.campsiteID }) { (campsiteID, campsiteName, latest) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(campsiteID) }
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = campsiteName.ifBlank { "Campsite" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when {
                            latest == null -> "No messages yet. Say hi!"
                            latest.automated -> "AUTOMATED MESSAGE: ${latest.text}"
                            !latest.fromOwner -> "You: ${latest.text}"
                            else -> latest.text
                        },
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

/** One conversation with a campsite, plus a text box to reply */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ChatThread(
    appState: AppState,
    campsiteID: String,
    entrantID: String,
    entrant: Entrant?,
    messages: List<ChatMessage>,
    onBack: () -> Unit
) {
    // The campsite itself (for its owner and its calendar dates)
    val campsite by produceState<Campsite?>(initialValue = null, key1 = campsiteID) {
        appState.campsites.searchCampsite(campsiteID) { found -> value = found }
    }
    val campsiteName = campsite?.getName() ?: messages.firstOrNull()?.campsiteName.orEmpty()

    var messageText by remember { mutableStateOf("") }
    // Location whose Select Dates calendar is open (null = closed)
    var reservingLocation by remember { mutableStateOf<Int?>(null) }
    // Days picked per location
    // TODO: save these to Firestore (nothing is stored yet)
    val pickedDates = remember(campsiteID) { mutableStateMapOf<Int, List<LocalDate>>() }

    // Keep the newest message in view
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 15.dp)) {
        Spacer(Modifier.height(48.dp))

        // Header: back + campsite name
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Text(
                text = campsiteName.ifBlank { "Campsite" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Empty chat: nobody has messaged yet
        if (messages.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "No messages yet. Send ${campsiteName.ifBlank { "the campsite" }} a message below.",
                color = LocalContentColor.current.copy(alpha = 0.6f)
            )
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.messageID.ifBlank { "${it.sentAt}" } }) { message ->
                ChatBubble(
                    message = message,
                    campsiteName = campsiteName,
                    timeText = timeFormatter.format(Date(message.sentAt))
                ) {
                    // Winning message: button to pick dates for that location
                    if (message.type == LOTTERY_WON && message.locationIndex >= 0) {
                        val picked = pickedDates[message.locationIndex].orEmpty()
                        val status = reservationStatus(messages, message.locationIndex)
                        if (status == RESERVATION_DECLINED) {
                            // Declined: no new reservation can be made for this location
                            Button(onClick = {}, enabled = false) { Text("Reservation Declined") }
                        } else {
                            // Button to pick dates
                            val confirmed = status == RESERVATION_CONFIRMED || picked.isNotEmpty()
                            Button(
                                onClick = { reservingLocation = message.locationIndex },
                                enabled = campsite != null
                            ) { Text(if (confirmed) "Change Dates" else "Select Dates") }
                            if (picked.isNotEmpty()) {
                                Text("You picked ${picked.size} day(s).", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Reply box
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f)
            )
            Button(
                enabled = messageText.isNotBlank(),
                onClick = {
                    appState.chat.addMessage(
                        ChatMessage(
                            campsiteID = campsiteID,
                            campsiteName = campsiteName,
                            sender = entrant?.name?.ifBlank { null } ?: entrant?.username.orEmpty(),
                            timestamp = timeFormatter.format(Date()),
                            text = messageText.trim(),
                            fromOwner = false,
                            entrantID = entrantID,
                            ownerID = campsite?.getOwnerID()
                                ?: messages.firstOrNull { it.ownerID.isNotBlank() }?.ownerID.orEmpty()
                        )
                    )
                    messageText = ""
                }
            ) { Text("Send") }
        }
    }

    // Calendar popup after pressing "Select Dates"
    val location = reservingLocation
    val site = campsite
    if (location != null && site != null) {
        ReserveDatesDialog(
            campsite = site,
            title = "Select Dates: Location ${location + 1}",
            initiallySelected = pickedDates[location].orEmpty().toSet(),
            onDismiss = { reservingLocation = null },
            onConfirm = { dates ->
                pickedDates[location] = dates
                reservingLocation = null
                // Message + moves the campsite into the entrant's Reserved Campsites list
                confirmReservation(appState, site, entrant, entrantID, location)
            },
            onDecline = {
                pickedDates.remove(location)
                reservingLocation = null
                // Message + notification, locks this location, updates the entrant's lists
                declineReservation(appState, site, entrant, entrantID, location)
            }
        )
    }
}

/** Moves [campsiteID] into [entrant]'s Reserved Campsites list (from Active Campsites or the
 *  Waiting List, or just adds it if it's on neither). Does nothing if it's already reserved.
 */
fun reserveCampsite(appState: AppState, entrant: Entrant, campsiteID: String) {
    val lists = entrant.getCampsiteLists()
    if (lists[RESERVED_CAMPSITES]?.contains(campsiteID) == true) return
    val from = listOf(ACTIVE_CAMPSITES, WAITING_LIST)
        .firstOrNull { lists[it]?.contains(campsiteID) == true }
    if (from != null) {
        appState.entrants.moveEntrantCampsite(entrant, from, RESERVED_CAMPSITES, campsiteID)
    } else {
        appState.entrants.addEntrantCampsite(entrant, RESERVED_CAMPSITES, campsiteID)
    }
}

/** One chat bubble. The campsite's messages (owner or automated) are on the left,
 *  the entrant's own on the right. Automated messages are labelled.
 *  @param actions: optional buttons under the message (e.g. Select Dates)
 */
@Composable
private fun ChatBubble(
    message: ChatMessage,
    campsiteName: String,
    timeText: String,
    actions: @Composable () -> Unit = {}
) {
    val mine = !message.fromOwner
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        // Who sent the message
        Text(
            text = "${if (mine) "You" else campsiteName.ifBlank { "Campsite" }} · $timeText",
            fontSize = 12.sp,
            color = LocalContentColor.current.copy(alpha = 0.6f)
        )
        Column(
            modifier = Modifier
                .padding(top = 4.dp)
                .widthIn(max = 300.dp)
                .background(
                    if (mine) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Contents of the message (Add automated header if it's a notification)
            if (message.automated) {
                Text("AUTOMATED MESSAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(message.text, fontSize = 15.sp)
            actions()
        }
    }
}

/** Popup with the campsite's calendar where the winner taps the days they're available.
 *  Only the campsite's available days (light-blue band) can be picked.
 *  Under the calendar: "Decline reservation" and "Confirm reservation" (needs at least one day).
 *  The ✕ (or tapping outside the popup / the back button) closes it without deciding.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReserveDatesDialog(
    campsite: Campsite,
    title: String = "Reserve Dates",
    initiallySelected: Set<LocalDate>,
    onDismiss: () -> Unit,
    onConfirm: (List<LocalDate>) -> Unit,
    onDecline: () -> Unit
) {
    val ranges = campsite.getVacationStartTimes()
    var selected by remember(campsite) { mutableStateOf(initiallySelected) }

    AlertDialog(
        onDismissRequest = onDismiss,
        // Title + ✕ (closes the popup without confirming or declining)
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, modifier = Modifier.weight(1f))
                Text(
                    text = "✕",
                    fontSize = 22.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(start = 8.dp)
                        .testTag("closeReserveDates")
                )
            }
        },
        text = {
            Column {
                Text("Tap the days you're available at ${campsite.getName()}.", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                if (ranges.isEmpty()) {
                    Text("No dates have been set for this campsite yet.", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                }
                SelectableDatesCalendar(
                    ranges = ranges,
                    selected = selected,
                    onToggle = { date ->
                        selected = if (date in selected) selected - date else selected + date
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text("${selected.size} day(s) selected", fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))

                // Decline / Confirm buttons under the calendar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) { Text("Decline reservation", fontSize = 13.sp, textAlign = TextAlign.Center) }
                    Button(
                        enabled = selected.isNotEmpty(),
                        onClick = { onConfirm(selected.sorted()) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) { Text("Confirm reservation", fontSize = 13.sp, textAlign = TextAlign.Center) }
                }
            }
        },
        // The two buttons above replace the dialog's own Confirm / Cancel
        confirmButton = {}
    )
}

/** Same month calendar as AvailableDatesCalendar (CampsiteDetails), but available days can be
 *  tapped to select them. Selected days get a filled circle.
 *  Each day has the test tag "reserveDay_<yyyy-MM-dd>".
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SelectableDatesCalendar(
    ranges: List<Pair<LocalDateTime, LocalDateTime>>,
    selected: Set<LocalDate>,
    onToggle: (LocalDate) -> Unit,
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
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹", fontSize = 20.sp) }
            Text(
                text = "${month.month.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale)} ${month.year}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("›", fontSize = 20.sp) }
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

        // Day grid
        val leadingBlanks = month.atDay(1).dayOfWeek.value % 7   // Sunday = 0
        val daysInMonth = month.lengthOfMonth()
        val rows = (leadingBlanks + daysInMonth + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - leadingBlanks + 1
                    if (day in 1..daysInMonth) {
                        val date = month.atDay(day)
                        SelectableCalendarDay(
                            date = date,
                            ranges = ranges,
                            isSelected = date in selected,
                            onToggle = onToggle,
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

/** One day cell: available days form the same light-blue band as CampsiteDetails' calendar,
 *  and a selected day gets a filled circle on top. Only available days can be tapped.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SelectableCalendarDay(
    date: LocalDate,
    ranges: List<Pair<LocalDateTime, LocalDateTime>>,
    isSelected: Boolean,
    onToggle: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val available = isDateAvailable(date, ranges)
    val roundStart = !isDateAvailable(date.minusDays(1), ranges) ||
        date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfMonth == 1
    val roundEnd = !isDateAvailable(date.plusDays(1), ranges) ||
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfMonth == date.lengthOfMonth()
    val startRadius = if (roundStart) 16.dp else 0.dp
    val endRadius = if (roundEnd) 16.dp else 0.dp
    val band = RoundedCornerShape(
        topStart = startRadius, bottomStart = startRadius,
        topEnd = endRadius, bottomEnd = endRadius
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .padding(vertical = 2.dp)
            .then(if (available) Modifier.background(CalendarHighlight, band) else Modifier)
            .then(if (available) Modifier.clickable { onToggle(date) } else Modifier)
            .testTag("reserveDay_$date"),
        contentAlignment = Alignment.Center
    ) {
        // Filled circle behind the number when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (available) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                available -> Color.Black
                else -> LocalContentColor.current.copy(alpha = 0.5f)
            }
        )
    }
}

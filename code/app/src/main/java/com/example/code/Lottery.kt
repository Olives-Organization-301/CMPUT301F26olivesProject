package com.example.code

import java.time.LocalDate
import kotlinx.coroutines.delay
import java.time.format.DateTimeParseException
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.Entrant
import androidx.core.content.edit
import kotlin.time.Duration.Companion.milliseconds

// Kinds of chat messages
// A Normal Chat Message
// TODO: Create a new ENUM Class in order to stop these global variables from polluting the program
const val MESSAGE_TEXT = "text"
const val LOTTERY_WON = "lottery_won"
const val LOTTERY_LOST = "lottery_lost"
// The entrant confirmed / declined their reservation for a location they won
const val RESERVATION_CONFIRMED = "reservation_confirmed"
const val RESERVATION_DECLINED = "reservation_declined"

/** One result message per won location, or one loss when none were won. */
internal fun lotteryResultMessages(campsite: Campsite, now: Long): List<ChatMessage> =
    campsite.getWaitingList().distinct().flatMap { entrantID ->
        val locations = campsite.getLocationsWon(entrantID)
        if (locations.isEmpty()) {
            listOf(lotteryMessage(campsite, entrantID, LOTTERY_LOST, -1, now,
                "I'm Sorry, you lost the lottery for ${campsite.getName()}."))
        } else {
            locations.map { location ->
                lotteryMessage(campsite, entrantID, LOTTERY_WON, location, now + location,
                    "Congratulations! You won a place in location ${location + 1} at ${campsite.getName()}. " +
                        "Use the button below to select your dates.")
            }
        }
    }

@RequiresApi(Build.VERSION_CODES.O)
internal fun Campsite.isReadyToDraw(now: java.time.LocalDateTime): Boolean =
    getEndDate()?.let { !now.isBefore(it) } == true && !isLotteryDrawn()

// ---------------------------------------------------------------------------------------------
// Confirming / declining a won location (the two buttons in ReserveDatesDialog)
// TODO: finish citation, Convo: https://claude.ai/share/a85f01c0-dfd7-4bfa-9315-8d607e4416f0
// ---------------------------------------------------------------------------------------------

/** The entrant's latest decision for [locationIndex] in one conversation:
 *  RESERVATION_CONFIRMED, RESERVATION_DECLINED, or null if they haven't decided yet */
fun reservationStatus(messages: List<ChatMessage>, locationIndex: Int): String? =
    latestReservation(messages, locationIndex)?.type

/** Display name of a campsite location: "Country, Province, City", or "location N" if it has none */
fun locationName(campsite: Campsite, locationIndex: Int): String =
    campsite.getCoordinates().getOrNull(locationIndex)?.getLocationText()?.ifBlank { null }
        ?: "location ${locationIndex + 1}"

internal fun latestReservation(messages: List<ChatMessage>, locationIndex: Int): ChatMessage? =
    messages.filter {
        it.locationIndex == locationIndex &&
            (it.type == RESERVATION_CONFIRMED || it.type == RESERVATION_DECLINED)
    }.maxByOrNull { it.sentAt }

@RequiresApi(Build.VERSION_CODES.O)
internal fun ChatMessage.reservationDates(): List<LocalDate> =
    stayDates.mapNotNull { date ->
        try { LocalDate.parse(date) } catch (_: DateTimeParseException) { null }
    }

@RequiresApi(Build.VERSION_CODES.O)
internal fun reservationHasEnded(messages: List<ChatMessage>, today: LocalDate): Boolean {
    val confirmed = messages.filter { it.locationIndex >= 0 }
        .map { it.locationIndex }.distinct().mapNotNull { latestReservation(messages, it) }
        .filter { it.type == RESERVATION_CONFIRMED }
    return confirmed.isNotEmpty() && confirmed.all { message ->
        val dates = message.reservationDates()
        dates.isNotEmpty() && dates.size == message.stayDates.size && dates.all { it.isBefore(today) }
    }
}

/** Reconciles expired stays while the entrant is signed in, including after a relaunch. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun ReservationHistoryEffect(appState: AppState) {
    val entrantID = appState.userID
    if (appState.role != "entrant" || entrantID.isNullOrBlank()) return
    val entrant = appState.entrants.entrants.firstOrNull { it.getUserID() == entrantID } ?: return
    val conversations = appState.chat.messages
        .filter { it.entrantID == entrantID && it.type in listOf(RESERVATION_CONFIRMED, RESERVATION_DECLINED) }
        .groupBy { it.campsiteID }
    LaunchedEffect(entrant, conversations) {
        while (true) {
            val today = LocalDate.now()
            entrant.getCampsiteLists()[RESERVED_CAMPSITES].orEmpty().toList().forEach { id ->
                val messages = conversations[id].orEmpty()
                if (reservationHasEnded(messages, today)) {
                    val revision = messages.filter { it.type == RESERVATION_CONFIRMED }.maxOf { it.sentAt }
                    appState.entrants.archiveReservation(entrant, id, revision)
                }
            }
            delay(30_000.milliseconds)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun confirmReservation(
    appState: AppState,
    campsite: Campsite,
    entrant: Entrant,
    locationIndex: Int,
    dates: List<LocalDate>,
    onComplete: (Boolean) -> Unit
) {
    val storedDates = dates.distinct().sorted().map(LocalDate::toString)
    val message = lotteryMessage(
        campsite, entrant.getUserID(), RESERVATION_CONFIRMED, locationIndex,
        System.currentTimeMillis(), "You reserved spots for location ${locationIndex + 1} for ${campsite.getName()}"
    ).copy(stayDates = storedDates)
    appState.entrants.confirmStay(entrant, message, onComplete)
}

/** "Decline reservation": sends the automated "You declined..." message (which also becomes a
 *  phone notification through LotteryNotificationEffect), which locks that location's Select
 *  Dates button. Then fixes the entrant's lists:
 *  - another location here is confirmed -> stays in Reserved Campsites
 *  - other locations are still undecided -> stays in (or goes back to) Active Campsites
 *  - nothing left to reserve -> removed from their lists */
fun declineReservation(
    appState: AppState,
    campsite: Campsite,
    entrant: Entrant?,
    entrantID: String,
    locationIndex: Int
) {
    val campsiteID = campsite.campsiteID
    appState.chat.addMessage(
        lotteryMessage(campsite, entrantID, RESERVATION_DECLINED, locationIndex,
            System.currentTimeMillis(),
            "You declined a reservation for ${campsite.getName()} in ${locationName(campsite, locationIndex)}")
    )
    if (entrant == null) return

    /**
     * [messages] - get messages between campsite and entrant
     * [statuses] - check how user has reserved the locations a campsite has gotten
     * [lists] - get the entrant's campsites
     * [inList] - if entrant's in the campsite
     */
    val messages = appState.chat.conversation(campsiteID, entrantID)
    val statuses = messages
        .filter { it.type == LOTTERY_WON && it.locationIndex >= 0 }
        .map { it.locationIndex }
        .distinct()
        .map { reservationStatus(messages, it) }
    val lists = entrant.getCampsiteLists()
    val inList = { key: String -> lists[key]?.contains(campsiteID) == true }

    // Handle Firebase operations when declining a reservation
    when {
        // Do not change if there was already a confirmed reservation
        RESERVATION_CONFIRMED in statuses -> {}
        // If one status has been reserved but you didn't move it
        null in statuses -> if (inList(RESERVED_CAMPSITES)) {
            appState.entrants.moveEntrantCampsite(entrant, RESERVED_CAMPSITES, ACTIVE_CAMPSITES, campsiteID)
        }
        // Remove campsite if we declined every reservation
        else -> listOf(ACTIVE_CAMPSITES, WAITING_LIST, RESERVED_CAMPSITES)
            .filter(inList)
            .forEach { appState.entrants.removeEntrantCampsite(entrant, it, campsiteID) }
    }
}

// An automated message from [campsite] to [entrantID]
// Make a ChatMessage Object
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


// ---------------------------------------------------------------------------------------------
// Lottery result notifications
// ---------------------------------------------------------------------------------------------
/* Shows a phone notification when the logged-in entrant receives a lottery result
 * (the automated LOTTERY_WON / LOTTERY_LOST messages sent above), or declines a reservation
 * (RESERVATION_DECLINED).
 *
 * DatabaseChat already listens to the "messages" collection in real time, so this watches that
 * list. A result for the current entrant that hasn't been handled yet becomes a notification,
 * if the entrant opted in (Edit User Profile > "Opt in to notifications") and the phone allows
 * this app to notify. Handled message IDs are kept in SharedPreferences so the same result isn't
 * shown again every time the app opens.
 *
 * The app never asks for notification permission by itself; the profile settings do that
 * (see requestNotificationPermission).
 *
 * Limitation: only works while the app is running (open or in recent apps). Notifying a closed
 * app needs Firebase Cloud Messaging plus a server.
 * TODO: Cite Properly https://claude.ai/share/9c41f27c-83b1-4829-a2cc-41cecd741c16
 */

private const val CHANNEL_ID = "lottery_results"
private const val PREFS = "lottery_notifications"
private const val KEY_HANDLED = "handled_message_ids"

/** Put once at the top of the app (MainActivity). Re-checks for new lottery results whenever
 *  someone logs in/out or the messages change. */
@Composable
fun LotteryNotificationEffect(appState: AppState) {
    val context = LocalContext.current.applicationContext
    // The launch effect that handles sending notifications after it was allowed
    LaunchedEffect(Unit) {
        createLotteryChannel(context)
        snapshotFlow { Triple(appState.role, appState.userID, lotteryResultsFor(appState).size) }
            .collect { notifyLotteryResults(context, appState) }
    }
}

/** Posts a notification for each lottery result the logged-in entrant hasn't been told about.
 *  Also call this right after notifications get turned on, so waiting results show up. */
fun notifyLotteryResults(context: Context, appState: AppState) {
    val userID = appState.userID ?: return
    if (appState.role != "entrant" || lotteryResultsFor(appState).isEmpty()) return

    appState.entrants.searchEntrants(userID) { entrant ->
        if (entrant == null || appState.userID != userID) return@searchEntrants
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val handled = prefs.getStringSet(KEY_HANDLED, emptySet()).orEmpty().toMutableSet()
        val fresh = lotteryResultsFor(appState)
            .filter { it.messageID.isNotBlank() && it.messageID !in handled }
            .sortedBy { it.sentAt }

        when {
            fresh.isEmpty() -> return@searchEntrants
            // Opted out: mark them handled, so opting in later doesn't dump old results
            !entrant.wantsNotifications -> {}
            // Opted in but the phone blocks it: leave them for when it's allowed
            !hasNotificationPermission(context) -> return@searchEntrants
            else -> fresh.forEach { showLotteryNotification(context, it) }
        }
        handled += fresh.map { it.messageID }
        prefs.edit { putStringSet(KEY_HANDLED, handled) }
    }
}

/** Lottery results addressed to the logged-in entrant (empty if no entrant is logged in) */
private fun lotteryResultsFor(appState: AppState): List<ChatMessage> {
    val userID = appState.userID ?: return emptyList()
    if (appState.role != "entrant") return emptyList()
    return appState.chat.messages.filter {
        it.automated && it.entrantID == userID &&
            (it.type == LOTTERY_WON || it.type == LOTTERY_LOST || it.type == RESERVATION_DECLINED)
    }
}

/** True if the phone lets this app show notifications */
fun hasNotificationPermission(context: Context): Boolean {
    // Check if they have notifications
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) return false
    // Return true if they have notifications
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

/** Asks the phone to allow notifications. Called from the profile settings only.
 *  Android 13+: shows the system "Allow notifications?" prompt through [launcher].
 *  Older Android (or the prompt was already declined): opens this app's notification settings.
 *  @param launcher from rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
 */
fun requestNotificationPermission(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    alreadyAsked: Boolean = false
) {
    // The if statements are used for version checking
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alreadyAsked &&
        // Give the Phone Emulator permission to notify
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Give the Phone Emulator permission to notify
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Notification channel for lottery results (required on Android 8+; safe to call repeatedly) */
private fun createLotteryChannel(context: Context) {
    // Create a channel to handle which lottery channels you have gotten or not
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Lottery results",
        NotificationManager.IMPORTANCE_HIGH
    ).apply { description = "Tells you when a campsite lottery you entered has been drawn" }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

/** Shows one lottery result. Tapping it opens the app.
 *  Only called after hasNotificationPermission() passes, hence the lint suppression. */
@SuppressLint("MissingPermission") // Suppress notifications if you've opted out
private fun showLotteryNotification(context: Context, message: ChatMessage) {
    // Message to send in phone emulator notification
    val title = when (message.type) {
        LOTTERY_WON -> "You won the lottery for ${message.campsiteName}!"
        RESERVATION_DECLINED -> "Reservation declined for ${message.campsiteName}"
        else -> "Lottery results for ${message.campsiteName}"
    }

    // Open the emulator and start sending notification if there's permission
    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Build the UI for notification (Builds the UI)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(message.text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(openApp)
        .setAutoCancel(true)
        .build()

    try {
        // Each message gets its own notification (several wins = several notifications)
        // Send a notification
        NotificationManagerCompat.from(context).notify(message.messageID.hashCode(), notification)
    } catch (e: SecurityException) {
        // No notification if they reject
        android.util.Log.e("Notifications", "notification permission missing", e)
    }
}

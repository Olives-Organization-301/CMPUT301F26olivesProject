package com.example.code

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


// ---------------------------------------------------------------------------------------------
// Lottery result notifications
// ---------------------------------------------------------------------------------------------
/* Shows a phone notification when the logged-in entrant receives a lottery result
 * (the automated LOTTERY_WON / LOTTERY_LOST messages sent above).
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
 */

private const val CHANNEL_ID = "lottery_results"
private const val PREFS = "lottery_notifications"
private const val KEY_HANDLED = "handled_message_ids"

/** Put once at the top of the app (MainActivity). Re-checks for new lottery results whenever
 *  someone logs in/out or the messages change. */
@Composable
fun LotteryNotificationEffect(appState: AppState) {
    val context = LocalContext.current.applicationContext
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
        prefs.edit().putStringSet(KEY_HANDLED, handled).apply()
    }
}

/** Lottery results addressed to the logged-in entrant (empty if no entrant is logged in) */
private fun lotteryResultsFor(appState: AppState): List<ChatMessage> {
    val userID = appState.userID ?: return emptyList()
    if (appState.role != "entrant") return emptyList()
    return appState.chat.messages.filter {
        it.automated && it.entrantID == userID && (it.type == LOTTERY_WON || it.type == LOTTERY_LOST)
    }
}

/** True if the phone lets this app show notifications */
fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) return false
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alreadyAsked &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Notification channel for lottery results (required on Android 8+; safe to call repeatedly) */
private fun createLotteryChannel(context: Context) {
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
@SuppressLint("MissingPermission")
private fun showLotteryNotification(context: Context, message: ChatMessage) {
    val title = when (message.type) {
        LOTTERY_WON -> "You won the lottery for ${message.campsiteName}!"
        else -> "Lottery results for ${message.campsiteName}"
    }

    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

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
        NotificationManagerCompat.from(context).notify(message.messageID.hashCode(), notification)
    } catch (e: SecurityException) {
        android.util.Log.e("Notifications", "notification permission missing", e)
    }
}

package com.example.code

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

internal data class LotteryDrawTarget(val id: String, val shared: Boolean)

/** All participating owners may process a shared draw; entrants may process one they entered.
 * Codex prompts: "work on 2.04.08" and "check it correct if it works properly we are going to present with all devices online".
 */
@RequiresApi(Build.VERSION_CODES.O)
internal fun dueLotteryTargets(
    userID: String,
    owner: Boolean,
    now: LocalDateTime,
    campsites: List<Campsite>,
    lotteries: List<SharedLottery>,
): List<LotteryDrawTarget> {
    val solo = campsites.filter {
        it.sharedLotteryID == null && (it.lotteryNotificationsPending || it.isReadyToDraw(now)) &&
            if (owner) it.getOwnerID() == userID else it.entrantInList(userID)
    }.map { LotteryDrawTarget(it.campsiteID, shared = false) }
    val shared = lotteries.filter {
        (it.notificationsPending || !it.drawn && !now.isBefore(it.closes)) &&
            if (owner) userID in it.ownerIDs else userID in it.entrants
    }.map { LotteryDrawTarget(it.id, shared = true) }
    return solo + shared
}

/** US 02.05.01. Codex prompt: "work on it so that it does not need to have a paid plan, just something that works" */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun ForegroundLotteryDrawEffect(appState: AppState, lifecycle: Lifecycle) {
    val userID = appState.userID ?: return
    val role = appState.role
    if (role != "owner" && role != "entrant") return
    LaunchedEffect(appState, lifecycle, userID, role) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val database = appState.campsites
                val targets = dueLotteryTargets(
                    userID, role == "owner", lotteryNow(), database.campsites, database.sharedLotteries.all()
                )
                for (target in targets) {
                    try {
                        database.lotteryDraws.process(target, userID, role == "owner")
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("LotteryDraw", "Could not process ${target.id}; will retry", error)
                    }
                }
                delay(15_000)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun SharedLottery.drawWinners(now: LocalDateTime, random: Random = Random.Default): List<String>? {
    if (drawn || now.isBefore(closes)) return null
    return entrants.distinct().shuffled(random).take(winnerCount.coerceAtLeast(0))
}

internal fun sharedLotteryMessages(
    lottery: SharedLottery,
    name: String,
    entrants: List<String>,
    winners: List<String>,
    drawnAt: Long,
): List<ChatMessage> {
    val winning = winners.toSet()
    return entrants.distinct().map { entrant ->
        val won = entrant in winning
        ChatMessage(
            campsiteID = lottery.anchorID, campsiteName = name, sender = name,
            fromOwner = true, entrantID = entrant, ownerID = lottery.ownerID,
            automated = true, type = if (won) LOTTERY_WON else LOTTERY_LOST,
            text = if (won) "You won a place in a shared lottery. Choose one campsite and your stay dates."
                else "You were not selected in the lottery for $name.",
            lotteryID = lottery.id, sentAt = drawnAt
        )
    }
}

/** Stable across devices and compatible with result IDs from the earlier Cloud Function. */
internal fun lotteryResultMessageID(message: ChatMessage, drawnAt: Long): String {
    val listing = message.lotteryID.takeIf { it.isNotBlank() }?.let { "shared/$it" } ?: message.campsiteID
    val key = JSONArray(listOf(listing, drawnAt, message.entrantID, message.locationIndex))
        .toString().replace("\\/", "/")
    val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
    return "lottery-" + hash.joinToString("") { "%02x".format(it) }
}

/**
 * Uses Firestore transactions and the existing result outbox. Every client rechecks the
 * live listing at close. A committed draw survives a restart, and stable message IDs
 * allow delivery to resume without sending duplicate results.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class LotteryDrawRepository(
    private val db: FirebaseFirestore,
    private val parseCampsite: (DocumentSnapshot, Boolean) -> Campsite?,
    private val parseShared: (DocumentSnapshot) -> SharedLottery?,
) {
    private data class Delivery(val drawnAt: Long, val messages: List<ChatMessage>)

    suspend fun process(target: LotteryDrawTarget, userID: String, owner: Boolean) {
        val ref = db.collection(if (target.shared) "lotteries" else "campsites").document(target.id)
        val delivery = db.runTransaction { tx ->
            val doc = tx.get(ref)
            val pending = doc.getBoolean("lotteryNotificationsPending") == true
            val frozen = doc.entrantIDs("lotteryDrawEntrants")
            val now = lotteryNow()
            val drawnAt = (doc.get("lotteryDrawnAt") as? Number)?.toLong()
            if (pending && drawnAt == null) return@runTransaction null

            if (target.shared) {
                val lottery = parseShared(doc) ?: return@runTransaction null
                if (!authorized(userID, owner, lottery.ownerID, lottery.entrants, frozen) &&
                    !(owner && userID in lottery.ownerIDs)) return@runTransaction null
                val name = doc.getString("name") ?: "Shared lottery"
                if (pending && drawnAt != null) {
                    Delivery(drawnAt, sharedLotteryMessages(lottery, name, frozen, lottery.winners, drawnAt))
                } else {
                    val winners = lottery.drawWinners(now) ?: return@runTransaction null
                    val sentAt = System.currentTimeMillis()
                    val entrants = lottery.entrants.distinct()
                    tx.update(ref, mapOf(
                        "finalList" to winners,
                        "lotteryDrawnAt" to sentAt,
                        "lotteryDrawEntrants" to entrants,
                        "lotteryNotificationsPending" to entrants.isNotEmpty()
                    ))
                    Delivery(sentAt, sharedLotteryMessages(lottery, name, entrants, winners, sentAt))
                }
            } else {
                val site = parseCampsite(doc, false) ?: return@runTransaction null
                if (site.sharedLotteryID != null ||
                    !authorized(userID, owner, site.getOwnerID(), site.getWaitingList(), frozen)) {
                    return@runTransaction null
                }
                if (pending && drawnAt != null) {
                    site.setWaitingList(frozen)
                    Delivery(drawnAt, lotteryResultMessages(site, drawnAt))
                } else {
                    if (!site.isReadyToDraw(now)) return@runTransaction null
                    site.generateFinalList()
                    val sentAt = requireNotNull(site.lotteryDrawnAt)
                    val entrants = site.getWaitingList()
                    tx.update(ref, mapOf(
                        "finalList" to site.getFinalList(),
                        "locationWinners" to site.getLocationWinners(),
                        "lotteryDrawnAt" to sentAt,
                        "lotteryDrawEntrants" to entrants,
                        "lotteryNotificationsPending" to entrants.isNotEmpty()
                    ))
                    Delivery(sentAt, lotteryResultMessages(site, sentAt))
                }
            }
        }.awaitDraw() ?: return

        if (delivery.messages.isEmpty()) return
        for (batch in delivery.messages.chunked(40)) {
            if (!deliverBatch(ref, delivery.drawnAt, batch)) return
        }
        db.runTransaction { tx ->
            if (tx.get(ref).pendingDraw(delivery.drawnAt)) {
                tx.update(ref, mapOf(
                    "lotteryNotificationsPending" to false,
                    "lotteryDrawEntrants" to emptyList<String>()
                ))
            }
        }.awaitDraw()
    }

    private suspend fun deliverBatch(ref: DocumentReference, drawnAt: Long, batch: List<ChatMessage>): Boolean =
        db.runTransaction { tx ->
            if (!tx.get(ref).pendingDraw(drawnAt)) return@runTransaction false
            val losses = batch.filter { it.type == LOTTERY_LOST && it.lotteryID.isNotBlank() }
                .map { db.collection("Entrants").document(it.entrantID) to it.campsiteID }
                .filter { (profile, _) -> tx.get(profile).exists() }
            for ((profile, anchor) in losses) {
                tx.update(profile,
                    FieldPath.of("campsiteLists", ACTIVE_CAMPSITES), FieldValue.arrayRemove(anchor),
                    FieldPath.of("campsiteLists", WAITING_LIST), FieldValue.arrayRemove(anchor)
                )
            }
            for (message in batch) {
                val id = lotteryResultMessageID(message, drawnAt)
                tx.set(db.collection("messages").document(id), message.copy(messageID = id))
            }
            true
        }.awaitDraw()

    private fun DocumentSnapshot.pendingDraw(drawnAt: Long) =
        getBoolean("lotteryNotificationsPending") == true && (get("lotteryDrawnAt") as? Number)?.toLong() == drawnAt

    private fun DocumentSnapshot.entrantIDs(field: String) =
        (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

    private fun authorized(userID: String, owner: Boolean, ownerID: String, entrants: List<String>, frozen: List<String>) =
        if (owner) userID == ownerID else userID in entrants || userID in frozen
}

private suspend fun <T> Task<T>.awaitDraw(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { result ->
        if (result.isSuccessful) continuation.resume(result.result)
        else continuation.resumeWithException(result.exception ?: CancellationException("Lottery transaction cancelled"))
    }
}

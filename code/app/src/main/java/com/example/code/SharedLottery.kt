package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.ChatMessage
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalDateTime

internal data class SharedLotteryBooking(
    val campsiteID: String,
    val location: Int,
    val dates: List<String>,
    val status: String,
)

internal data class SharedLottery(
    val id: String,
    val ownerID: String,
    val campsiteIDs: List<String>,
    val opens: LocalDateTime,
    val closes: LocalDateTime,
    val capacity: Int?,
    val winnerCount: Int,
    val entrants: List<String>,
    val winners: List<String>,
    val drawn: Boolean,
    val bookings: Map<String, SharedLotteryBooking>,
    val notificationsPending: Boolean = false,
) {
    val anchorID: String get() = campsiteIDs.first()

    fun gate(now: LocalDateTime) = registrationGate(opens, closes, drawn, now)

    fun admission(entrantID: String, now: LocalDateTime) =
        waitlistAdmission(gate(now), entrants, capacity, entrantID)

    fun bookedDates(campsiteID: String, location: Int): Set<String> = bookings.values
        .filter { it.campsiteID == campsiteID && it.location == location && it.status == RESERVATION_CONFIRMED }
        .flatMap { it.dates }.toSet()

    fun canBook(entrantID: String, campsiteID: String, location: Int, dates: List<String>): Boolean =
        drawn && entrantID in winners && entrantID !in bookings && campsiteID in campsiteIDs &&
            location >= 0 && dates.isNotEmpty() && dates.none { it in bookedDates(campsiteID, location) }
}

/**
 * US 02.04.02 stores one registration window, waitlist and result for several owner campsites.
 * The first campsite anchors the entry in the existing entrant screens. Confirmations use the
 * actual campsite ID, so owner reservations and entrant history still show the booked campsite.
 *
 * Codex prompts: "now i need ideas for 2.04.03, 2.04.02, and how it should interact with any previous functions or user stories"
 * and "work on this, make testcases, and document the code"
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class SharedLotteryRepository(private val db: FirebaseFirestore) {
    private val lotteries = db.collection("lotteries")
    private val campsites = db.collection("campsites")
    private val entrants = db.collection("Entrants")
    private val messages = db.collection("messages")
    private val listings = mutableStateListOf<SharedLottery>()

    init {
        lotteries.addSnapshotListener { snapshot, error ->
            if (error != null) android.util.Log.e("DB", "Could not load shared lotteries", error)
            if (snapshot != null) {
                listings.clear()
                listings.addAll(snapshot.documents.mapNotNull(::parse))
            }
        }
    }

    fun find(id: String): SharedLottery? = listings.firstOrNull { it.id == id }
    fun all(): List<SharedLottery> = listings.toList()

    fun save(
        id: String?,
        ownerID: String,
        campsiteIDs: List<String>,
        opens: LocalDateTime,
        closes: LocalDateTime,
        capacity: Int?,
        winnerCount: Int,
    ): Task<Boolean> {
        val ref = id?.let(lotteries::document) ?: lotteries.document()
        return db.runTransaction { tx ->
            val now = lotteryNow()
            val current = parse(tx.get(ref))
            if (campsiteIDs.distinct().size != campsiteIDs.size || campsiteIDs.size < 2 ||
                !opens.isBefore(closes) || !now.isBefore(closes) || winnerCount <= 0 ||
                capacity?.let { it <= 0 || it < (current?.entrants?.size ?: 0) } == true ||
                (id != null && (current == null || current.ownerID != ownerID ||
                    current.gate(now) == WaitlistJoinResult.CLOSED))) return@runTransaction false

            val previousIDs = current?.campsiteIDs.orEmpty()
            if (current != null && campsiteIDs.first() != current.anchorID) return@runTransaction false
            val membershipChanged = previousIDs.toSet() != campsiteIDs.toSet()
            if (membershipChanged && (current?.gate(now) == WaitlistJoinResult.JOINED || !now.isBefore(opens))) {
                return@runTransaction false
            }
            // Once entry has opened, editing cannot push its start back into the future.
            if (current?.gate(now) == WaitlistJoinResult.JOINED && opens != current.opens) return@runTransaction false

            val siteDocs = (previousIDs + campsiteIDs).distinct().map { tx.get(campsites.document(it)) }
            if (siteDocs.any { site ->
                val member = site.getString("lotteryID")
                !site.exists() || siteOwner(site) != ownerID ||
                    if (site.id in previousIDs) member != ref.id else
                        !member.isNullOrBlank() || site.get("lotteryDrawnAt") != null ||
                            (site.get("locationWinners") as? Map<*, *>)?.isNotEmpty() == true ||
                            listOf("waitingList", "activeLotteryList", "finalList").any { site.strings(it).isNotEmpty() }
            }) return@runTransaction false

            val fields = mapOf(
                "ownerID" to ownerID,
                "name" to (siteDocs.first { it.id == campsiteIDs.first() }.getString("name") ?: "Shared lottery"),
                "campsiteIDs" to campsiteIDs,
                "lotteryStartDate" to opens.toString(),
                "lotteryEndDate" to closes.toString(),
                "waitlistCapacity" to capacity,
                "winnerCount" to winnerCount
            )
            if (current == null) tx.set(ref, fields + ("waitingList" to emptyList<String>()))
            else tx.update(ref, fields)
            if (membershipChanged) {
                siteDocs.forEach { site ->
                    if (site.id in campsiteIDs) tx.update(site.reference, "lotteryID", ref.id)
                    else tx.update(site.reference, "lotteryID", FieldValue.delete())
                }
            }
            true
        }
    }

    /** The group and the entrant's list are updated together, including retries from another campsite. */
    fun updateEntry(id: String, entrantID: String, join: Boolean): Task<WaitlistJoinResult> {
        val ref = lotteries.document(id)
        return db.runTransaction { tx ->
            val lottery = parse(tx.get(ref)) ?: return@runTransaction WaitlistJoinResult.FAILED
            val now = lotteryNow()
            val result = if (join) lottery.admission(entrantID, now) else lottery.gate(now)
            if (result != WaitlistJoinResult.JOINED) return@runTransaction result
            val entrant = entrants.document(entrantID)
            if (join) {
                tx.update(ref, "waitingList", FieldValue.arrayUnion(entrantID))
                tx.update(entrant, FieldPath.of("campsiteLists", ACTIVE_CAMPSITES), FieldValue.arrayUnion(lottery.anchorID))
            } else {
                tx.update(ref, "waitingList", FieldValue.arrayRemove(entrantID))
                tx.update(entrant,
                    FieldPath.of("campsiteLists", ACTIVE_CAMPSITES), FieldValue.arrayRemove(lottery.anchorID),
                    FieldPath.of("campsiteLists", WAITING_LIST), FieldValue.arrayRemove(lottery.anchorID)
                )
            }
            WaitlistJoinResult.JOINED
        }
    }

    /** A winner claims one campsite and location. Read the same group document for every claim. */
    fun confirm(
        id: String,
        entrantID: String,
        campsiteID: String,
        location: Int,
        dates: List<LocalDate>,
        acknowledgedRules: List<String>,
    ): Task<Boolean> {
        val ref = lotteries.document(id)
        val siteRef = campsites.document(campsiteID)
        val entrant = entrants.document(entrantID)
        val messageRef = messages.document()
        return db.runTransaction { tx ->
            val lottery = parse(tx.get(ref)) ?: return@runTransaction false
            val site = tx.get(siteRef)
            val profile = tx.get(entrant)
            val storedDates = dates.distinct().sorted().map(LocalDate::toString)
            val locations = site.get("coordinates") as? List<*>
            val locationCount = maxOf(1, locations?.size ?: 0, (site.get("maxOccupancy") as? List<*>)?.size ?: 0)
            if (!profile.exists() || !site.exists() || site.getString("lotteryID") != id ||
                location !in 0 until locationCount || acknowledgedRules != site.strings("rules") ||
                !lottery.canBook(entrantID, campsiteID, location, storedDates) ||
                !validStayDates(dates, site, lotteryNow().toLocalDate())) return@runTransaction false

            val name = site.getString("name") ?: site.getString("campsiteName") ?: campsiteID
            val previousRevision = (profile.get(FieldPath.of("reservationRevision", campsiteID)) as? Number)?.toLong() ?: 0L
            val sentAt = maxOf(System.currentTimeMillis(), previousRevision + 1)
            tx.update(ref, FieldPath.of("bookings", entrantID),
                bookingFields(campsiteID, location, storedDates, RESERVATION_CONFIRMED))
            tx.update(entrant,
                FieldPath.of("campsiteLists", ACTIVE_CAMPSITES), FieldValue.arrayRemove(lottery.anchorID),
                FieldPath.of("campsiteLists", WAITING_LIST), FieldValue.arrayRemove(lottery.anchorID),
                FieldPath.of("campsiteLists", PAST_CAMPSITES), FieldValue.arrayRemove(campsiteID),
                FieldPath.of("campsiteLists", RESERVED_CAMPSITES), FieldValue.arrayUnion(campsiteID),
                FieldPath.of("reservationDates", campsiteID, location.toString()), storedDates,
                FieldPath.of("reservationRevision", campsiteID), sentAt
            )
            tx.set(messageRef, ChatMessage(
                messageID = messageRef.id, campsiteID = campsiteID, campsiteName = name,
                sender = name, entrantID = entrantID, ownerID = lottery.ownerID, automated = true,
                type = RESERVATION_CONFIRMED, locationIndex = location, sentAt = sentAt,
                text = "You reserved location ${location + 1} at $name.",
                stayDates = storedDates, lotteryID = id, rulesAcknowledged = true
            ))
            true
        }
    }

    fun decline(id: String, entrantID: String): Task<Boolean> {
        val ref = lotteries.document(id)
        val messageRef = messages.document()
        return db.runTransaction { tx ->
            val lottery = parse(tx.get(ref)) ?: return@runTransaction false
            if (!lottery.drawn || entrantID !in lottery.winners || entrantID in lottery.bookings) return@runTransaction false
            tx.update(ref, FieldPath.of("bookings", entrantID), bookingFields("", -1, emptyList(), RESERVATION_DECLINED))
            tx.update(entrants.document(entrantID),
                FieldPath.of("campsiteLists", ACTIVE_CAMPSITES), FieldValue.arrayRemove(lottery.anchorID),
                FieldPath.of("campsiteLists", WAITING_LIST), FieldValue.arrayRemove(lottery.anchorID)
            )
            tx.set(messageRef, ChatMessage(
                messageID = messageRef.id, campsiteID = lottery.anchorID, ownerID = lottery.ownerID,
                entrantID = entrantID, automated = true, type = RESERVATION_DECLINED,
                text = "You declined the shared lottery reservation.", lotteryID = id
            ))
            true
        }
    }

    fun reject(message: ChatMessage, ownerID: String): Task<Boolean> =
        cancelBooking(message.lotteryID, message.entrantID, message.campsiteID, ownerID)

    fun cancel(id: String, entrantID: String, campsiteID: String): Task<Boolean> =
        cancelBooking(id, entrantID, campsiteID, ownerID = null)

    /** Release the booked nights and publish the cancellation in the same transaction. */
    private fun cancelBooking(id: String, entrantID: String, campsiteID: String, ownerID: String?): Task<Boolean> {
        val ref = lotteries.document(id)
        val profile = entrants.document(entrantID)
        val cancellation = messages.document()
        return db.runTransaction { tx ->
            val lottery = parse(tx.get(ref)) ?: return@runTransaction false
            val booking = lottery.bookings[entrantID] ?: return@runTransaction false
            if ((ownerID != null && lottery.ownerID != ownerID) || booking.status != RESERVATION_CONFIRMED ||
                booking.campsiteID != campsiteID || booking.dates.none { it >= lotteryNow().toLocalDate().toString() }) {
                return@runTransaction false
            }
            val site = tx.get(campsites.document(campsiteID))
            val profileSnapshot = tx.get(profile)
            val name = site.getString("name") ?: campsiteID
            val revision = (profileSnapshot.get(FieldPath.of("reservationRevision", campsiteID)) as? Number)?.toLong() ?: 0L
            tx.update(ref, FieldPath.of("bookings", entrantID, "status"), RESERVATION_DECLINED)
            tx.update(profile,
                FieldPath.of("campsiteLists", RESERVED_CAMPSITES), FieldValue.arrayRemove(campsiteID)
            )
            tx.set(cancellation, ChatMessage(
                messageID = cancellation.id, campsiteID = campsiteID, campsiteName = name, sender = name,
                entrantID = entrantID, ownerID = lottery.ownerID, automated = true, type = RESERVATION_DECLINED,
                text = if (ownerID == null) "You cancelled your reservation." else "The owner rejected your reservation.",
                locationIndex = booking.location, sentAt = maxOf(System.currentTimeMillis(), revision + 1), lotteryID = id
            ))
            true
        }
    }

    private fun validStayDates(dates: List<LocalDate>, site: DocumentSnapshot, today: LocalDate): Boolean {
        val ranges = (site.get("vacationStartTimes") as? List<*>).orEmpty().mapNotNull {
            val range = it as? Map<*, *> ?: return@mapNotNull null
            val start = parseDate(range["start"])?.toLocalDate() ?: return@mapNotNull null
            val end = parseDate(range["end"])?.toLocalDate() ?: return@mapNotNull null
            start..end
        }
        return dates.isNotEmpty() && dates.all { date -> !date.isBefore(today) && ranges.any { date in it } }
    }

    internal fun parse(doc: DocumentSnapshot): SharedLottery? {
        val sites = doc.strings("campsiteIDs")
        val opens = parseDate(doc.get("lotteryStartDate")) ?: return null
        val closes = parseDate(doc.get("lotteryEndDate")) ?: return null
        if (sites.isEmpty()) return null
        val bookings = (doc.get("bookings") as? Map<*, *>)?.mapNotNull { (key, value) ->
            val entrantID = key as? String ?: return@mapNotNull null
            val booking = value as? Map<*, *> ?: return@mapNotNull null
            entrantID to SharedLotteryBooking(
                booking["campsiteID"] as? String ?: "",
                (booking["location"] as? Number)?.toInt() ?: -1,
                (booking["dates"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
                booking["status"] as? String ?: ""
            )
        }?.toMap().orEmpty()
        return SharedLottery(doc.id, doc.getString("ownerID").orEmpty(), sites, opens, closes,
            (doc.get("waitlistCapacity") as? Number)?.toInt(), (doc.get("winnerCount") as? Number)?.toInt() ?: 5,
            doc.strings("waitingList"), doc.strings("finalList"), doc.get("lotteryDrawnAt") != null, bookings,
            doc.getBoolean("lotteryNotificationsPending") == true)
    }

    private fun bookingFields(campsiteID: String, location: Int, dates: List<String>, status: String) =
        mapOf("campsiteID" to campsiteID, "location" to location, "dates" to dates, "status" to status)

    private fun siteOwner(doc: DocumentSnapshot) =
        sequenceOf("ownerID", "OwnerID", "ownerId", "owner_id").firstNotNullOfOrNull { doc.get(it)?.toString() }

    private fun DocumentSnapshot.strings(key: String) = (get(key) as? List<*>)?.filterIsInstance<String>().orEmpty()
    private fun parseDate(value: Any?) = (value as? String)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
}

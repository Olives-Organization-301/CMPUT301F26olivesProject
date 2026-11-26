package com.example.code

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.code.classes.Campsite
import com.example.code.classes.Coordinates
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Codex prompt: Populate five reviewer-ready campsites with photos, chats, and lottery edge cases. */
class ReviewerFirestoreSeed {
    private val prefix = "olives_reviewer_20261126"
    private val db = FirebaseFirestore.getInstance()
    private val now = System.currentTimeMillis()

    private fun entrant(number: Int) = "${prefix}_entrant_${number.toString().padStart(2, '0')}"
    private fun owner(number: Int) = "${prefix}_owner_$number"
    private fun site(number: Int) = "${prefix}_site_$number"
    private fun entrantEmail(number: Int) = "olives.entrant${number.toString().padStart(2, '0')}@example.com"
    private fun ownerEmail(number: Int) = "olives.owner$number@example.com"

    @Test
    fun seedAndVerifyReviewerDataOnServer() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("writeReviewerFirestore") == "true")
        val marker = "CampsiteOwners/${owner(1)}"
        val existing = Tasks.await(db.document(marker).get(Source.SERVER), 30, TimeUnit.SECONDS)
        val today = LocalDate.now()
        val thursday = today.with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
        val photo = image(R.drawable.campsite_details)
        val thumbnail = image(R.drawable.campsite_thumbnail)
        val avatars = listOf(image(R.drawable.default_profile_image), image(R.drawable.default_gift_image))
        val entrants = (1..20).map(::entrant)
        val firstFive = entrants.take(5)
        val sites = listOf(
            campsite(1, "Thursday Draw", listOf(3), thursday, firstFive, photo),
            campsite(2, "One Entrant Two Locations", listOf(1, 2), today.minusDays(3), listOf(entrant(6)), thumbnail),
            campsite(3, "Twenty Entrants Two Locations", listOf(10, 10), today.minusDays(3), entrants, photo),
            campsite(4, "QR Entry", listOf(4), today, emptyList(), thumbnail),
            campsite(5, "Completed Stay and Reviews", listOf(3), thursday.minusWeeks(2), firstFive, photo)
        )
        checkLotteryBoundaries(sites[0], thursday)
        sites[1].generateFinalList()
        assertEquals(listOf(0, 1), sites[1].getLocationsWon(entrant(6)))
        if (existing.exists()) {
            val snapshot = Tasks.await(db.document("campsites/${site(3)}").get(Source.SERVER), 30, TimeUnit.SECONDS)
            val winners = (snapshot.get("locationWinners") as Map<*, *>).entries.associate { (index, ids) ->
                index.toString() to (ids as List<*>).filterIsInstance<String>()
            }
            sites[2].setLocationWinners(winners)
            sites[2].setFinalList(winners.values.flatten().distinct())
        } else {
            repeat(3) {
                if (overlap(sites[2]).isEmpty()) sites[2].generateFinalList()
            }
        }
        assertEquals(listOf(10, 10), sites[2].getLocationWinners().values.map { it.size })
        assertTrue(overlap(sites[2]).isNotEmpty())
        sites[4].setLocationWinners(mapOf("0" to listOf(entrant(1), entrant(3), entrant(4))))
        sites[4].setFinalList(listOf(entrant(1), entrant(3), entrant(4)))

        val docs = linkedMapOf<String, Map<String, Any?>>()
        for (number in 1..2) {
            val id = owner(number)
            val profile = profile(id, "Demo Owner $number", ownerEmail(number), avatars[number - 1], true)
            docs["CampsiteOwners/$id"] = profile
            docs["images/$id"] = imageRecord(profile, "campsiteownerID", id)
        }
        for (number in 1..20) {
            val id = entrant(number)
            val profile = profile(id, "Demo Entrant $number", entrantEmail(number), avatars[number % 2], number != 1)
            docs["Entrants/$id"] = profile + mapOf(
                "campsiteLists" to mapOf(
                    WAITING_LIST to if (number <= 5) listOf(site(1)) else emptyList(),
                    ACTIVE_CAMPSITES to emptyList<String>(),
                    RESERVED_CAMPSITES to if (number == 3) listOf(site(5)) else emptyList(),
                    PAST_CAMPSITES to if (number == 1) listOf(site(5)) else emptyList()
                ),
                "messageCampsiteIDs" to listOfNotNull(site(3), site(5).takeIf { number <= 5 },
                    site(2).takeIf { number == 6 }),
                "reservationIDs" to emptyList<String>()
            )
            docs["images/$id"] = imageRecord(profile, "entrantID", id)
        }
        sites.forEach { campsite ->
            val id = campsite.campsiteID
            docs["campsites/$id"] = campsite.toMap() + mapOf(
                "description" to campsite.getDescription(),
                "rules" to campsite.getRules(),
                "reviews" to campsite.getReviews()
            )
            val image = campsite.getDetailsImage()
            docs["images/campsite_$id"] = mapOf(
                "campsiteID" to id, "campsiteownerID" to campsite.getOwnerID(),
                "campsiteName" to campsite.getName(), "imageData" to image,
                "campsiteImage" to image, "timestamp" to now
            )
        }

        var sequence = 0
        fun message(campsite: Campsite, entrantID: String, fromOwner: Boolean, text: String,
                    type: String = MESSAGE_TEXT, location: Int = -1) {
            val messageID = "${prefix}_message_${++sequence}"
            docs["messages/$messageID"] = mapOf(
                "messageID" to messageID, "campsiteID" to campsite.campsiteID,
                "campsiteName" to campsite.getName(), "entrantID" to entrantID,
                "ownerID" to campsite.getOwnerID(),
                "sender" to if (fromOwner) campsite.getName() else "Demo Entrant ${entrantID.takeLast(2)}",
                "timestamp" to "Demo", "sentAt" to now + sequence, "fromOwner" to fromOwner,
                "automated" to (type != MESSAGE_TEXT), "type" to type,
                "locationIndex" to location, "text" to text
            )
        }
        listOf(sites[1], sites[2], sites[4]).forEach { campsite ->
            campsite.getWaitingList().forEach { id ->
                val locations = campsite.getLocationsWon(id)
                if (locations.isEmpty()) {
                    message(campsite, id, true, "You were not selected for this draw.", LOTTERY_LOST)
                } else {
                    locations.forEach { location ->
                        message(campsite, id, true, "You won a spot at location ${location + 1}.", LOTTERY_WON, location)
                    }
                }
            }
        }
        val completed = sites[4]
        message(completed, entrant(1), false, "Is there parking near the campsite?")
        message(completed, entrant(1), true, "Yes, parking is available next to the check-in area.")
        message(completed, entrant(2), false, "Can I enter the next lottery if I was not selected?")
        message(completed, entrant(2), true, "Yes, you can enter the next draw when it opens.")
        message(completed, entrant(1), true, "Your stay was confirmed.", RESERVATION_CONFIRMED, 0)
        message(completed, entrant(3), true, "Your reservation was confirmed.", RESERVATION_CONFIRMED, 0)
        message(completed, entrant(4), true, "You declined your reservation.", RESERVATION_DECLINED, 0)

        if (!existing.exists()) {
            Tasks.await(db.runTransaction { transaction ->
                check(!transaction.get(db.document(marker)).exists()) { "Reviewer data already exists" }
                docs.forEach { (path, fields) -> transaction.set(db.document(path), fields) }
            }, 60, TimeUnit.SECONDS)
        }
        val snapshots = Tasks.await(
            Tasks.whenAllSuccess<DocumentSnapshot>(
                docs.keys.map { db.document(it).get(Source.SERVER) }
            ), 60, TimeUnit.SECONDS
        )
        assertEquals(docs.size, snapshots.count { it.exists() && !it.metadata.isFromCache })
        val live = snapshots.first { it.id == site(1) }
        if (!existing.exists()) assertEquals(firstFive, live.get("waitingList"))
        assertEquals("Thursday Draw", live.getString("name")?.substringAfter("Demo 1 - "))
        val single = snapshots.first { it.id == site(2) }
        assertEquals(mapOf("0" to listOf(entrant(6)), "1" to listOf(entrant(6))), single.get("locationWinners"))
        val full = snapshots.first { it.id == site(3) }
        val fullWinners = full.get("locationWinners") as Map<*, *>
        val shared = (fullWinners["0"] as List<*>).intersect((fullWinners["1"] as List<*>).toSet())
        assertTrue(shared.isNotEmpty())
        println("Verified ${docs.size} Firestore documents. Thursday draw: ${live.getString("lotteryStartDate")} through ${live.getString("lotteryEndDate")}. Overlap: $shared")
    }

    private fun checkLotteryBoundaries(campsite: Campsite, thursday: LocalDate) {
        assertTrue(!campsite.isLotteryActive(thursday.minusDays(1)))
        assertTrue(campsite.isLotteryActive(thursday))
        assertTrue(campsite.isLotteryActive(thursday.plusDays(1)))
        assertTrue(!campsite.isLotteryActive(thursday.plusDays(2)))
        campsite.generateFinalList()
        assertEquals(3, campsite.getFinalList().size)
        assertEquals(2, campsite.checkLotteryResults().values.count { !it })
        campsite.setFinalList(emptyList())
        campsite.setLocationWinners(emptyMap())
    }

    private fun overlap(campsite: Campsite) =
        campsite.getLocationWinners()["0"].orEmpty().intersect(campsite.getLocationWinners()["1"].orEmpty().toSet())

    private fun campsite(number: Int, name: String, capacity: List<Int>, starts: LocalDate,
                         entrants: List<String>, image: String): Campsite =
        Campsite(
            name = "Demo $number - $name", ownerID = owner(if (number <= 3) 1 else 2), campsiteID = site(number),
            maxOccupancy = capacity.toMutableList(), price = 30f + number,
            coordinates = capacity.indices.map { index ->
                Coordinates("Canada", "Alberta", if (index == 0) "Banff" else "Canmore", "Demo Location ${index + 1}")
            }.toMutableList(),
            detailsImage = image, thumbnailImage = image,
            lotteryStartDate = starts.atStartOfDay(), lotteryEndDate = starts.plusDays(1).atTime(23, 59),
            vacationStartTimes = mutableListOf(starts.plusDays(2).atStartOfDay() to starts.plusDays(5).atStartOfDay())
        ).apply {
            setWaitingList(entrants)
            setDescription("Reviewer demo for $name.")
            setRules(listOf("Quiet hours begin at 10 PM", "Pack out all trash"))
        }

    private fun profile(id: String, name: String, email: String, image: String, notifications: Boolean) =
        mapOf(
            "userID" to id, "name" to name, "username" to name, "email" to email,
            "phoneNumber" to "403-555-0100", "description" to "Synthetic reviewer account.",
            "dateOfBirth" to "2000-01-01", "profileImage" to image, "wantsNotifications" to notifications
        )

    private fun imageRecord(profile: Map<String, Any>, roleField: String, id: String) =
        mapOf(
            roleField to id, "imageData" to profile.getValue("profileImage"),
            "profileImage" to profile.getValue("profileImage"), "timestamp" to now,
            "username" to profile.getValue("username"), "email" to profile.getValue("email")
        )

    private fun image(resource: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = BitmapFactory.decodeResource(context.resources, resource)
        return try {
            requireNotNull(encodeCampsitePhoto(bitmap))
        } finally {
            bitmap.recycle()
        }
    }
}

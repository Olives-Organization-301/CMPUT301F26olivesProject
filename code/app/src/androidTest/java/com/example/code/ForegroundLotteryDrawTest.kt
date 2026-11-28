package com.example.code

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.AppState
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Rule
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Prompts: "work on 2.04.08" and "make all test cases for this, stress test it to
 * the limit". Exercises cross-owner opt-in, booking owner routing, draw permission
 * and concurrent capacity enforcement against the local Firestore emulator.
 */
class ForegroundLotteryDrawTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun onlyOwnedOrEnteredLotteriesAreProcessedAndMemberCampsitesAreSkipped() {
        val now = LocalDateTime.of(2026, 12, 4, 12, 0)
        fun site(id: String, owner: String, closes: LocalDateTime) = Campsite(
            id, owner, id, mutableListOf(1), 10f, coordinates = mutableListOf(), lotteryEndDate = closes
        )
        val closed = site("closed", "owner", now).apply { addEntrant("entered") }
        val member = site("member", "owner", now).apply {
            sharedLotteryID = "group"
            addEntrant("entered")
        }
        val upcoming = site("upcoming", "owner", now.plusSeconds(1))
        val unrelated = site("unrelated", "another", now)
        val outbox = site("outbox", "owner", now).apply {
            lotteryDrawnAt = 1L
            lotteryNotificationsPending = true
        }
        val group = group(now)
        val sites = listOf(closed, member, upcoming, unrelated, outbox)
        val expected = listOf(LotteryDrawTarget("closed", false), LotteryDrawTarget("group", true))

        assertEquals(expected, dueLotteryTargets("entered", false, now, sites, listOf(group)))
        assertEquals(listOf(expected[0], LotteryDrawTarget("outbox", false), expected[1]),
            dueLotteryTargets("owner", true, now, sites, listOf(group)))
        assertTrue(dueLotteryTargets("stranger", false, now, sites, listOf(group)).isEmpty())
        assertEquals(listOf(LotteryDrawTarget("group", true)),
            dueLotteryTargets("other-owner", true, now, sites,
                listOf(group.copy(ownerIDs = listOf("owner", "other-owner")))))
    }

    @Test
    fun anotherOwnersOfferedCampsiteCanJoinSharedLottery() {
        val host = InstrumentationRegistry.getArguments().getString("firestoreEmulatorHost")
        assumeTrue(host == "10.0.2.2" || host == "127.0.0.1")
        val port = InstrumentationRegistry.getArguments().getString("firestoreEmulatorPort")?.toIntOrNull() ?: 8188
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(
            ApplicationProvider.getApplicationContext<Context>(),
            FirebaseOptions.Builder().setProjectId("demo-olives-lottery")
                .setApplicationId("1:1234567890:android:multiownertest")
                .setApiKey("emulator-only").build(),
            "multi-owner-$id"
        )
        try {
            val db = FirebaseFirestore.getInstance(app)
            db.useEmulator(requireNotNull(host), port)
            val first = "$id-first"
            val second = "$id-second"
            val ownerA = "$id-owner-a"
            val ownerB = "$id-owner-b"
            val start = lotteryNow().plusDays(2)
            val end = start.plusDays(2)
            val firstRef = db.collection("campsites").document(first)
            val secondRef = db.collection("campsites").document(second)
            Tasks.await(firstRef.set(mapOf("ownerID" to ownerA, "name" to "Pine")), 15, TimeUnit.SECONDS)
            val stay = LocalDate.now().plusDays(7)
            Tasks.await(secondRef.set(mapOf(
                "ownerID" to ownerB, "name" to "Cedar", "rules" to emptyList<String>(),
                "vacationStartTimes" to listOf(mapOf(
                    "start" to stay.atStartOfDay().toString(),
                    "end" to stay.plusDays(2).atTime(23, 59, 59).toString()
                ))
            )), 15, TimeUnit.SECONDS)
            val repository = SharedLotteryRepository(db)
            assertFalse(Tasks.await(repository.save(null, ownerA, listOf(first, second), start, end, null, 1), 15, TimeUnit.SECONDS))
            val database = DatabaseCampsites(db)
            assertFalse(Tasks.await(database.setSharedLotteryOffer(second, ownerA, true), 15, TimeUnit.SECONDS))
            assertTrue(Tasks.await(database.setSharedLotteryOffer(second, ownerB, true), 15, TimeUnit.SECONDS))
            assertTrue(Tasks.await(repository.save(null, ownerA, listOf(first, second), start, end, null, 1), 15, TimeUnit.SECONDS))
            val lottery = Tasks.await(db.collection("lotteries").get(), 15, TimeUnit.SECONDS).documents
                .first { (it.get("campsiteIDs") as? List<*>) == listOf(first, second) }
            assertEquals(listOf(ownerA, ownerB), lottery.get("ownerIDs"))
            assertEquals(lottery.id, Tasks.await(secondRef.get(), 15, TimeUnit.SECONDS).getString("lotteryID"))
            assertFalse(Tasks.await(database.setSharedLotteryOffer(second, ownerB, false), 15, TimeUnit.SECONDS))
            val entrant = "$id-entrant"
            Tasks.await(db.collection("Entrants").document(entrant).set(mapOf(
                "campsiteLists" to mapOf(ACTIVE_CAMPSITES to listOf(first))
            )), 15, TimeUnit.SECONDS)
            Tasks.await(lottery.reference.update(mapOf(
                "lotteryDrawnAt" to System.currentTimeMillis(), "finalList" to listOf(entrant)
            )), 15, TimeUnit.SECONDS)
            assertTrue(Tasks.await(repository.confirm(lottery.id, entrant, second, 0, listOf(stay), emptyList()),
                15, TimeUnit.SECONDS))
            val confirmation = Tasks.await(db.collection("messages").get(), 15, TimeUnit.SECONDS).documents
                .first { it.getString("type") == RESERVATION_CONFIRMED && it.getString("entrantID") == entrant }
            assertEquals(ownerB, confirmation.getString("ownerID"))
        } finally {
            app.delete()
        }
    }

    @Test
    fun concurrentSharedRegistrationsRespectOneCapacityAcrossOwners() {
        val host = InstrumentationRegistry.getArguments().getString("firestoreEmulatorHost")
        assumeTrue(host == "10.0.2.2" || host == "127.0.0.1")
        val port = InstrumentationRegistry.getArguments().getString("firestoreEmulatorPort")?.toIntOrNull() ?: 8188
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(
            ApplicationProvider.getApplicationContext<Context>(),
            FirebaseOptions.Builder().setProjectId("demo-olives-lottery")
                .setApplicationId("1:1234567890:android:multiownerloadtest")
                .setApiKey("emulator-only").build(),
            "multi-owner-load-$id"
        )
        try {
            val db = FirebaseFirestore.getInstance(app)
            db.useEmulator(requireNotNull(host), port)
            val groupID = "$id-group"
            val first = "$id-first"
            val second = "$id-second"
            val entrantIDs = (0 until 30).map { "$id-entrant-$it" }
            val batch = db.batch()
            batch.set(db.collection("campsites").document(first), mapOf("ownerID" to "$id-owner-a", "lotteryID" to groupID))
            batch.set(db.collection("campsites").document(second), mapOf("ownerID" to "$id-owner-b", "lotteryID" to groupID))
            batch.set(db.collection("lotteries").document(groupID), mapOf(
                "ownerID" to "$id-owner-a", "ownerIDs" to listOf("$id-owner-a", "$id-owner-b"),
                "campsiteIDs" to listOf(first, second),
                "lotteryStartDate" to lotteryNow().minusDays(1).toString(),
                "lotteryEndDate" to lotteryNow().plusDays(1).toString(),
                "waitingList" to emptyList<String>(), "waitlistCapacity" to 10, "winnerCount" to 5
            ))
            entrantIDs.forEach { entrant ->
                batch.set(db.collection("Entrants").document(entrant), mapOf(
                    "campsiteLists" to mapOf(ACTIVE_CAMPSITES to emptyList<String>())
                ))
            }
            Tasks.await(batch.commit(), 15, TimeUnit.SECONDS)
            val repository = SharedLotteryRepository(db)
            val attempts = entrantIDs.map { entrant -> repository.updateEntry(groupID, entrant, true) }
            val results = Tasks.await(Tasks.whenAllSuccess<WaitlistJoinResult>(attempts), 90, TimeUnit.SECONDS)
            assertEquals(10, results.count { it == WaitlistJoinResult.JOINED })
            assertEquals(20, results.count { it == WaitlistJoinResult.FULL })
            val lottery = Tasks.await(db.collection("lotteries").document(groupID).get(), 15, TimeUnit.SECONDS)
            val joined = (lottery.get("waitingList") as List<*>).filterIsInstance<String>()
            assertEquals(10, joined.size)
            assertEquals(10, joined.toSet().size)
            val retry = joined.first()
            assertEquals(WaitlistJoinResult.JOINED,
                Tasks.await(repository.updateEntry(groupID, retry, true), 15, TimeUnit.SECONDS))
            assertEquals(10, (Tasks.await(db.collection("lotteries").document(groupID).get(), 15, TimeUnit.SECONDS)
                .get("waitingList") as List<*>).size)
        } finally {
            app.delete()
        }
    }

    @Test
    fun sharedDrawProducesOneStableResultPerEntrant() {
        val closes = LocalDateTime.of(2026, 12, 4, 12, 0)
        val group = group(closes)
        assertNull(group.drawWinners(closes.minusNanos(1)))
        assertNull(group.copy(drawn = true).drawWinners(closes))
        assertEquals(emptyList<String>(), group.copy(entrants = emptyList()).drawWinners(closes))
        val winners = requireNotNull(group.drawWinners(closes, Random(7)))
        val messages = sharedLotteryMessages(group, "Pine", group.entrants, winners, 1234)
        assertEquals(1, winners.size)
        assertEquals(2, messages.size)
        assertEquals(1, messages.count { it.type == LOTTERY_WON })
        assertEquals(1, messages.count { it.type == LOTTERY_LOST })
        assertTrue(messages.all { it.lotteryID == group.id && it.campsiteID == group.anchorID })
        assertEquals(
            "lottery-dace56756813ad251b09cb26d5d36407f0e653161b548abb06a42e080998730e",
            lotteryResultMessageID(ChatMessage(lotteryID = "group", entrantID = "entrant"), 1234)
        )
    }

    /** Opt-in integration test. The demo Firebase project and an emulator address are required. */
    @Test
    fun firestoreEmulatorCommitsOneDrawAndResumesPendingResults() {
        val arguments = InstrumentationRegistry.getArguments()
        val host = arguments.getString("firestoreEmulatorHost")
        assumeTrue(host == "10.0.2.2" || host == "127.0.0.1")
        val port = arguments.getString("firestoreEmulatorPort")?.toIntOrNull() ?: 8188
        val app = FirebaseApp.initializeApp(
            ApplicationProvider.getApplicationContext<Context>(),
            FirebaseOptions.Builder().setProjectId("demo-olives-lottery")
                .setApplicationId("1:1234567890:android:lotterydrawtest")
                .setApiKey("emulator-only").build(),
            "lottery-draw-${UUID.randomUUID()}"
        )
        try {
            verifyEmulatorDraw(app, requireNotNull(host), port)
        } finally {
            app.delete()
        }
    }

    @Test
    fun foregroundEffectAutomaticallyDrawsClosedLottery() {
        val arguments = InstrumentationRegistry.getArguments()
        val host = arguments.getString("firestoreEmulatorHost")
        assumeTrue(host == "10.0.2.2" || host == "127.0.0.1")
        val port = arguments.getString("firestoreEmulatorPort")?.toIntOrNull() ?: 8188
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(
            ApplicationProvider.getApplicationContext<Context>(),
            FirebaseOptions.Builder().setProjectId("demo-olives-lottery")
                .setApplicationId("1:1234567890:android:lotteryeffecttest")
                .setApiKey("emulator-only").build(),
            "lottery-effect-$id"
        )
        try {
            val db = FirebaseFirestore.getInstance(app)
            db.useEmulator(requireNotNull(host), port)
            val ownerID = "$id-owner"
            val entrantID = "$id-entrant"
            val campsite = Campsite(
                "Automatic draw", ownerID, id, mutableListOf(1), 10f,
                coordinates = mutableListOf(), lotteryEndDate = lotteryNow().minusMinutes(1)
            ).apply { addEntrant(entrantID) }
            val ref = db.collection("campsites").document(id)
            Tasks.await(ref.set(campsite.toMap()), 15, TimeUnit.SECONDS)

            val database = DatabaseCampsites(db)
            composeTestRule.waitUntil(10_000) { database.campsites.any { it.campsiteID == id } }
            val state = AppState(
                admins = mockk(relaxed = true), owners = mockk(relaxed = true),
                campsites = database, entrants = mockk(relaxed = true),
                alerts = mockk(relaxed = true), chat = mockk(relaxed = true)
            ).apply { userID = ownerID; role = "owner" }
            composeTestRule.setContent {
                ForegroundLotteryDrawEffect(state, LocalLifecycleOwner.current.lifecycle)
            }

            composeTestRule.waitUntil(20_000) {
                Tasks.await(ref.get(), 5, TimeUnit.SECONDS).getLong("lotteryDrawnAt") != null
            }
            val drawn = Tasks.await(ref.get(), 15, TimeUnit.SECONDS)
            assertEquals(listOf(entrantID), drawn.get("finalList"))
            composeTestRule.waitUntil(20_000) {
                Tasks.await(db.collection("messages").whereEqualTo("campsiteID", id).get(), 5, TimeUnit.SECONDS)
                    .size() == 1
            }
        } finally {
            app.delete()
        }
    }

    private fun verifyEmulatorDraw(app: FirebaseApp, host: String, port: Int) = runBlocking {
        val db = FirebaseFirestore.getInstance(app)
        db.useEmulator(host, port)
        val database = DatabaseCampsites(db)
        val id = UUID.randomUUID().toString()
        val group = group(lotteryNow().minusMinutes(1)).copy(
            id = id, campsiteIDs = listOf("$id-pine", "$id-cedar"),
            entrants = listOf("$id-a", "$id-b")
        )
        val ref = db.collection("lotteries").document(id)
        val fields = mapOf(
            "name" to "Pine and Cedar",
            "ownerID" to group.ownerID,
            "campsiteIDs" to group.campsiteIDs,
            "lotteryStartDate" to group.opens.toString(),
            "lotteryEndDate" to group.closes.toString(),
            "waitingList" to group.entrants,
            "winnerCount" to group.winnerCount
        )
        Tasks.await(ref.set(fields), 15, TimeUnit.SECONDS)
        for (entrant in group.entrants) {
            Tasks.await(db.collection("Entrants").document(entrant).set(
                mapOf("campsiteLists" to mapOf(ACTIVE_CAMPSITES to listOf(group.anchorID)))
            ), 15, TimeUnit.SECONDS)
        }
        val target = LotteryDrawTarget(id, shared = true)
        database.lotteryDraws.process(target, "stranger", owner = false)
        assertNull(Tasks.await(ref.get(), 15, TimeUnit.SECONDS).getLong("lotteryDrawnAt"))

        coroutineScope {
            listOf(
                async { database.lotteryDraws.process(target, group.ownerID, owner = true) },
                async { database.lotteryDraws.process(target, group.entrants.first(), owner = false) }
            ).awaitAll()
        }
        val drawn = Tasks.await(ref.get(), 15, TimeUnit.SECONDS)
        val drawnAt = requireNotNull(drawn.getLong("lotteryDrawnAt"))
        assertFalse(drawn.getBoolean("lotteryNotificationsPending") == true)
        val firstDelivery = Tasks.await(db.collection("messages").whereEqualTo("lotteryID", id).get(), 15, TimeUnit.SECONDS)
        assertEquals(2, firstDelivery.size())
        val loser = firstDelivery.documents.single { it.getString("type") == LOTTERY_LOST }.getString("entrantID")
        val loserProfile = Tasks.await(db.collection("Entrants").document(requireNotNull(loser)).get(), 15, TimeUnit.SECONDS)
        assertEquals(emptyList<String>(), loserProfile.get("campsiteLists.$ACTIVE_CAMPSITES"))

        // Simulates restarting after a committed draw and partial result delivery.
        Tasks.await(ref.update("lotteryNotificationsPending", true, "lotteryDrawEntrants", group.entrants), 15, TimeUnit.SECONDS)
        database.lotteryDraws.process(target, group.ownerID, owner = true)
        val recovered = Tasks.await(ref.get(), 15, TimeUnit.SECONDS)
        assertEquals(drawnAt, recovered.getLong("lotteryDrawnAt"))
        assertFalse(recovered.getBoolean("lotteryNotificationsPending") == true)
        val messages = Tasks.await(db.collection("messages").whereEqualTo("lotteryID", id).get(), 15, TimeUnit.SECONDS)
        assertEquals(firstDelivery.documents.map { it.id }.toSet(), messages.documents.map { it.id }.toSet())

        val solo = Campsite(
            "Solo", group.ownerID, "$id-solo", mutableListOf(1), 10f,
            coordinates = mutableListOf(), lotteryEndDate = group.closes
        ).apply { setWaitingList(group.entrants) }
        val soloRef = db.collection("campsites").document(solo.campsiteID)
        Tasks.await(soloRef.set(solo.toMap()), 15, TimeUnit.SECONDS)
        database.lotteryDraws.process(LotteryDrawTarget(solo.campsiteID, false), group.ownerID, owner = true)
        val soloResult = Tasks.await(soloRef.get(), 15, TimeUnit.SECONDS)
        assertNotNull(soloResult.getLong("lotteryDrawnAt"))
        assertEquals(1, (soloResult.get("finalList") as List<*>).size)
        assertFalse(soloResult.getBoolean("lotteryNotificationsPending") == true)
    }

    private fun group(closes: LocalDateTime) = SharedLottery(
        "group", "owner", listOf("pine", "cedar"), closes.minusDays(2), closes,
        2, 1, listOf("entered", "other", "entered"), emptyList(), false, emptyMap()
    )
}

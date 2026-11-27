package com.example.code

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ForegroundLotteryDrawTest {
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

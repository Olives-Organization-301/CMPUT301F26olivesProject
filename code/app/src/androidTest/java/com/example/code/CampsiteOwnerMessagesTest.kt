package com.example.code

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.Entrant
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.time.LocalDateTime
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.test.onAllNodesWithText

/**
 * Prompts: "make test cases for this in any case" and "make all test cases for this,
 * stress test it to the limit, and with all the other test cases from before all
 * together so the app works". Covers owner B's shared-lottery inbox, per-campsite
 * message isolation, entrant replies, live Firestore delivery and 1,000 entrants.
 */
@RunWith(AndroidJUnit4::class)
class CampsiteOwnerMessagesTest {
    @get:Rule
    val compose = createComposeRule()

    /** Compare the owner inbox with current Firestore registrations without changing any records. */
    @Test
    fun liveOwnerInboxesReflectRegisteredEntrants() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("readOwnerReservations") == "true")
        val db = FirebaseFirestore.getInstance()
        val owners = Tasks.await(db.collection("CampsiteOwners").get(Source.SERVER), 30, TimeUnit.SECONDS)
        val campsites = Tasks.await(db.collection("campsites").get(Source.SERVER), 30, TimeUnit.SECONDS)
        val lotteries = Tasks.await(db.collection("lotteries").get(Source.SERVER), 30, TimeUnit.SECONDS)
        val entrants = Tasks.await(db.collection("Entrants").get(Source.SERVER), 30, TimeUnit.SECONDS)
        val state = AppState().apply { role = "owner" }
        compose.setContent { CampsiteChatScreen(rememberNavController(), state) }
        compose.waitUntil(30_000) {
            val loadedEntrants = state.entrants.entrants.map { it.getUserID() }.toSet()
            val loadedSites = state.campsites.campsites.map { it.campsiteID }.toSet()
            entrants.documents.all { it.id in loadedEntrants } &&
                campsites.documents.filter { it.getString("name") != null }.all { it.id in loadedSites } &&
                lotteries.documents.filter { it.get("campsiteIDs") is List<*> }
                    .all { state.campsites.sharedLotteries.find(it.id) != null }
        }

        for (owner in owners.documents) {
            val sites = campsites.documents.filter { site ->
                (site.get("ownerID") ?: site.get("OwnerID") ?: site.get("ownerId"))
                    ?.toString()?.trim() == owner.id
            }
            val rows = sites.sortedBy { it.getString("name") }.flatMap { site ->
                val groupEntrants = lotteries.documents.firstOrNull {
                    it.id == site.getString("lotteryID")
                }?.get("waitingList") as? List<*>
                entrants.documents.filter { entrant ->
                    val lists = entrant.get("campsiteLists") as? Map<*, *>
                    entrant.id in groupEntrants.orEmpty() ||
                        listOf(WAITING_LIST, ACTIVE_CAMPSITES, RESERVED_CAMPSITES).any { key ->
                        (lists?.get(key) as? List<*>)?.contains(site.id) == true
                    }
                }.map { entrant ->
                    entrant.getString("name").orEmpty().ifBlank {
                        entrant.getString("username") ?: entrant.id
                    }
                }.sorted()
            }
            compose.runOnIdle { state.userID = owner.id }
            if (rows.isEmpty()) {
                compose.onNodeWithText("No registered entrants yet.").assertIsDisplayed()
            } else {
                compose.onAllNodesWithText(rows.first())[0].assertIsDisplayed()
            }
            println("Owner inbox audit: ${owner.id}, ${sites.size} campsites, ${rows.size} registered entrants")
        }
    }

    @Test
    fun ownerCanMessageRegisteredAndReservedEntrantsAtTheirCampsites() {
        val pine = campsite("pine", "Pine Camp", "owner")
        val lake = campsite("lake", "Lake Camp", "owner")
        val other = campsite("other", "Other Camp", "other-owner")
        val testEntrants = listOf(
            entrant("alice", "Alice", RESERVED_CAMPSITES, pine.campsiteID),
            entrant("ben", "Ben", RESERVED_CAMPSITES, lake.campsiteID),
            entrant("waiting", "Waiting Entrant", WAITING_LIST, pine.campsiteID),
            entrant("active", "Active Entrant", ACTIVE_CAMPSITES, lake.campsiteID)
                .apply { addCampsite(WAITING_LIST, lake.campsiteID) },
            entrant("past", "Past Entrant", PAST_CAMPSITES, pine.campsiteID),
            entrant("other", "Other Owner Entrant", RESERVED_CAMPSITES, other.campsiteID)
        )
        val pineMessages = mutableStateListOf(ChatMessage(text = "Pine arrival question"))
        val chat = mockk<DatabaseChat>(relaxed = true)
        every { chat.conversation("pine", "alice") } answers { pineMessages.toList() }
        every { chat.conversation("lake", "ben") } returns listOf(ChatMessage(text = "Lake arrival question"))
        every { chat.conversation("pine", "waiting") } returns listOf(ChatMessage(text = "Can I bring a dog?"))
        every { chat.addMessage(any()) } answers { pineMessages.add(firstArg()); Unit }
        val appState = AppState(
            admins = mockk(relaxed = true),
            owners = mockk(relaxed = true),
            campsites = mockk(relaxed = true) {
                every { campsites } returns listOf(pine, lake, other)
            },
            entrants = mockk<DatabaseEntrants>(relaxed = true) {
                every { entrants } returns testEntrants
            },
            alerts = mockk(relaxed = true),
            chat = chat
        ).apply {
            userID = "owner"
            role = "owner"
        }

        compose.setContent { CampsiteNavController(appState, startDestination = "campsiteOwnerMainScreen") }
        compose.onNodeWithText("Messages").performClick()
        compose.onNodeWithText("Alice").assertIsDisplayed()
        compose.onNodeWithText("Ben").assertIsDisplayed()
        compose.onNodeWithText("Waiting Entrant").assertIsDisplayed()
        compose.onNodeWithText("Active Entrant").assertIsDisplayed()
        compose.onAllNodesWithText("Active Entrant").assertCountEquals(1)
        compose.onNodeWithText("Past Entrant").assertDoesNotExist()
        compose.onNodeWithText("Other Owner Entrant").assertDoesNotExist()

        compose.onNodeWithText("Alice").performClick()
        compose.onNodeWithText("Pine arrival question").assertIsDisplayed()
        compose.onNodeWithText("Lake arrival question").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("Check in at noon")
        compose.onNodeWithText("Send").performClick()
        compose.onNodeWithText("Check in at noon").assertIsDisplayed()
        verify(exactly = 1) {
            chat.addMessage(match {
                it.ownerID == "owner" && it.campsiteID == "pine" && it.entrantID == "alice" &&
                    it.text == "Check in at noon" && it.fromOwner
            })
        }

        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Ben").performClick()
        compose.onNodeWithText("Lake arrival question").assertIsDisplayed()
        compose.onNodeWithText("Check in at noon").assertDoesNotExist()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Waiting Entrant").performClick()
        compose.onNodeWithText("Can I bring a dog?").assertIsDisplayed()
    }

    @Test
    fun newlyRegisteredEntrantAppearsWhileOwnerInboxIsOpen() {
        val pine = campsite("pine", "Pine Camp", "owner")
        val registered = mutableStateListOf<Entrant>()
        val chat = mockk<DatabaseChat>(relaxed = true)
        every { chat.conversation("pine", "new") } returns listOf(ChatMessage(text = "Hello owner"))
        val appState = AppState(
            admins = mockk(relaxed = true),
            owners = mockk(relaxed = true),
            campsites = mockk(relaxed = true) { every { campsites } returns listOf(pine) },
            entrants = mockk<DatabaseEntrants>(relaxed = true) {
                every { entrants } returns registered
            },
            alerts = mockk(relaxed = true),
            chat = chat
        ).apply { userID = "owner"; role = "owner" }

        compose.setContent { CampsiteChatScreen(rememberNavController(), appState) }
        compose.onNodeWithText("No registered entrants yet.").assertIsDisplayed()
        compose.runOnIdle { registered.add(entrant("new", "New Entrant", WAITING_LIST, pine.campsiteID)) }
        compose.onNodeWithText("New Entrant").assertIsDisplayed().performClick()
        compose.onNodeWithText("Hello owner").assertIsDisplayed()
    }

    @Test
    fun sharedLotteryThreadsIncludeBothOwnersAndNeverMixCampsites() {
        val pine = campsite("pine", "Pine", "owner-a").apply { sharedLotteryID = "shared" }
        val cedar = campsite("cedar", "Cedar", "owner-b").apply { sharedLotteryID = "shared" }
        val stranger = campsite("stranger", "Stranger", "owner-c")
        val registered = entrant("registered", "Registered", ACTIVE_CAMPSITES, "pine")
            .apply { addCampsite(WAITING_LIST, "pine") }
        val booked = entrant("booked", "Booked", RESERVED_CAMPSITES, "cedar")
        val unrelated = entrant("unrelated", "Unrelated", ACTIVE_CAMPSITES, "stranger")
        val group = shared("shared", listOf("pine", "cedar"), listOf("registered", "booked", "missing"))
        val sites = listOf(pine, cedar, stranger)
        val entrants = listOf(registered, booked, unrelated)

        assertEquals(setOf("registered", "booked"),
            ownerMessageThreads("owner-a", sites, entrants, listOf(group)).map { it.second.getUserID() }.toSet())
        assertEquals(setOf("registered", "booked"),
            ownerMessageThreads("owner-b", sites, entrants, listOf(group)).map { it.second.getUserID() }.toSet())
        assertEquals(listOf("unrelated"),
            ownerMessageThreads("owner-c", sites, entrants, listOf(group)).map { it.second.getUserID() })
        assertTrue(ownerMessageThreads(null, sites, entrants, listOf(group)).isEmpty())
    }

    @Test
    fun sharedLotteryInboxHandlesThousandEntrantsWithoutDuplicatesOrLeaks() {
        val pine = campsite("pine", "Pine", "owner-a").apply { sharedLotteryID = "shared" }
        val cedar = campsite("cedar", "Cedar", "owner-b").apply { sharedLotteryID = "shared" }
        val foreign = campsite("foreign", "Foreign", "owner-c")
        val ids = (0 until 1000).map { "entrant-$it" }
        val entrants = ids.map { id -> entrant(id, id, ACTIVE_CAMPSITES, "pine") }
        val outsiders = (0 until 100).map { id -> entrant("outsider-$id", "Outsider $id", ACTIVE_CAMPSITES, "foreign") }
        val group = shared("shared", listOf("pine", "cedar"), ids + ids.take(50))
        repeat(20) {
            val bThreads = ownerMessageThreads("owner-b", listOf(pine, cedar, foreign), entrants + outsiders, listOf(group))
            assertEquals(1000, bThreads.size)
            assertEquals(1000, bThreads.map { it.second.getUserID() }.toSet().size)
            assertTrue(bThreads.all { it.first.campsiteID == "cedar" })
            assertFalse(bThreads.any { it.second.getUserID().startsWith("outsider-") })
        }
    }

    @Test
    fun ownerBAndEntrantCanReplyInCedarsThreadWhilePineStaysSeparate() {
        val pine = campsite("pine", "Pine", "owner-a").apply { sharedLotteryID = "shared" }
        val cedar = campsite("cedar", "Cedar", "owner-b").apply { sharedLotteryID = "shared" }
        val camper = entrant("camper", "Camper", ACTIVE_CAMPSITES, "pine")
        val group = shared("shared", listOf("pine", "cedar"), listOf("camper"))
        val repo = mockk<SharedLotteryRepository>(relaxed = true) {
            every { all() } returns listOf(group)
        }
        val stored = mutableStateListOf(
            ChatMessage(campsiteID = "pine", campsiteName = "Pine", entrantID = "camper", ownerID = "owner-a", text = "Pine only", fromOwner = false),
            ChatMessage(campsiteID = "cedar", campsiteName = "Cedar", entrantID = "camper", ownerID = "owner-b", text = "Cedar question", fromOwner = false)
        )
        val chat = mockk<DatabaseChat>(relaxed = true) {
            every { conversation(any(), any()) } answers {
                stored.filter { it.campsiteID == firstArg<String>() && it.entrantID == secondArg<String>() }
            }
            every { conversationsFor(any()) } answers {
                stored.filter { it.entrantID == firstArg<String>() }.groupBy { it.campsiteID }
            }
            every { addMessage(any()) } answers { stored.add(firstArg()); Unit }
        }
        val sites = mockk<DatabaseCampsites>(relaxed = true) {
            every { campsites } returns listOf(pine, cedar)
            every { sharedLotteries } returns repo
            every { searchCampsite(any(), any()) } answers {
                secondArg<(Campsite?) -> Unit>()(listOf(pine, cedar).firstOrNull { it.campsiteID == firstArg<String>() })
            }
        }
        val profiles = mockk<DatabaseEntrants>(relaxed = true) {
            every { entrants } returns listOf(camper)
            every { searchEntrants(any(), any()) } answers { secondArg<(Entrant?) -> Unit>()(camper) }
        }
        val state = AppState(
            admins = mockk(relaxed = true), owners = mockk(relaxed = true),
            campsites = sites, entrants = profiles, alerts = mockk(relaxed = true), chat = chat
        ).apply { userID = "owner-b"; role = "owner" }
        val entrantView = mutableStateOf(false)
        compose.setContent {
            if (entrantView.value) EntrantMessagesScreen(rememberNavController(), state)
            else CampsiteChatScreen(rememberNavController(), state)
        }
        compose.onNodeWithText("Camper").assertIsDisplayed().performClick()
        compose.onNodeWithText("Cedar question").assertIsDisplayed()
        compose.onNodeWithText("Pine only").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("Welcome to Cedar")
        compose.onNodeWithText("Send").performClick()
        verify { chat.addMessage(match {
            it.campsiteID == "cedar" && it.ownerID == "owner-b" && it.entrantID == "camper" &&
                it.text == "Welcome to Cedar"
        }) }

        compose.runOnIdle { state.userID = "camper"; state.role = "entrant"; entrantView.value = true }
        compose.onNodeWithText("Cedar").performClick()
        compose.onNodeWithText("Welcome to Cedar").assertIsDisplayed()
        compose.onNodeWithText("Pine only").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("Thanks Cedar")
        compose.onNodeWithText("Send").performClick()
        verify { chat.addMessage(match {
            it.campsiteID == "cedar" && it.ownerID == "owner-b" && it.entrantID == "camper" &&
                it.text == "Thanks Cedar" && !it.fromOwner
        }) }

        compose.runOnIdle { state.userID = "owner-b"; state.role = "owner"; entrantView.value = false }
        compose.onNodeWithText("Camper").performClick()
        compose.onNodeWithText("Thanks Cedar").assertIsDisplayed()
        compose.onNodeWithText("Pine only").assertDoesNotExist()
    }

    @Test
    fun firestoreRegistrationAppearsInOwnerInboxAndShowsEntrantMessage() {
        val arguments = InstrumentationRegistry.getArguments()
        val host = arguments.getString("firestoreEmulatorHost")
        assumeTrue(host == "127.0.0.1" || host == "10.0.2.2")
        val port = arguments.getString("firestoreEmulatorPort")?.toIntOrNull() ?: 8188
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(
            ApplicationProvider.getApplicationContext<Context>(),
            FirebaseOptions.Builder().setProjectId("demo-olives-lottery")
                .setApplicationId("1:1234567890:android:ownerinboxtest")
                .setApiKey("emulator-only").build(),
            "owner-inbox-$id"
        )
        try {
            val db = FirebaseFirestore.getInstance(app)
            db.useEmulator(requireNotNull(host), port)
            val ownerID = "$id-owner"
            val entrantID = "$id-entrant"
            val campsite = Campsite(
                "Registration Camp", ownerID, id, mutableListOf(1), 10f,
                coordinates = mutableListOf(), lotteryStartDate = lotteryNow().minusDays(1),
                lotteryEndDate = lotteryNow().plusDays(1)
            )
            Tasks.await(db.collection("campsites").document(id).set(campsite.toMap()), 15, TimeUnit.SECONDS)
            Tasks.await(db.collection("Entrants").document(entrantID).set(mapOf(
                "name" to "Registered Camper",
                "username" to "camper",
                "campsiteLists" to mapOf(ACTIVE_CAMPSITES to emptyList<String>())
            )), 15, TimeUnit.SECONDS)

            val campsites = DatabaseCampsites(db)
            val entrants = DatabaseEntrants(db)
            val chat = DatabaseChat(db)
            val state = AppState(
                admins = mockk(relaxed = true), owners = mockk(relaxed = true),
                campsites = campsites, entrants = entrants,
                alerts = mockk(relaxed = true), chat = chat
            ).apply { userID = ownerID; role = "owner" }
            compose.setContent { CampsiteChatScreen(rememberNavController(), state) }
            compose.waitUntil(10_000) {
                campsites.campsites.any { it.campsiteID == id } &&
                    entrants.entrants.any { it.getUserID() == entrantID }
            }
            compose.onNodeWithText("No registered entrants yet.").assertIsDisplayed()

            val joined = CompletableFuture<WaitlistJoinResult>()
            campsites.addToWaitingList(campsite, entrantID) { joined.complete(it) }
            assertEquals(WaitlistJoinResult.JOINED, joined.get(15, TimeUnit.SECONDS))
            val saved = CompletableFuture<Boolean>()
            entrants.addEntrantCampsite(Entrant(userID = entrantID), ACTIVE_CAMPSITES, id) {
                saved.complete(it)
            }
            assertTrue(saved.get(15, TimeUnit.SECONDS))
            compose.waitUntil(10_000) {
                entrants.entrants.any {
                    it.getUserID() == entrantID && id in it.getCampsiteLists()[ACTIVE_CAMPSITES].orEmpty()
                }
            }
            compose.onNodeWithText("Registered Camper").assertIsDisplayed().performClick()

            val incoming = db.collection("messages").document()
            Tasks.await(incoming.set(ChatMessage(
                campsiteID = id, entrantID = entrantID, ownerID = ownerID,
                sender = "Registered Camper", text = "Can I bring a dog?", fromOwner = false
            )), 15, TimeUnit.SECONDS)
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Can I bring a dog?").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Can I bring a dog?").assertIsDisplayed()
            assertEquals(1, Tasks.await(db.collection("messages").whereEqualTo("entrantID", entrantID)
                .get(Source.SERVER), 15, TimeUnit.SECONDS).size())
        } finally {
            app.delete()
        }
    }

    @Test
    fun firestoreSharedLotteryEntrantAndMessagesReachSecondOwnerOnlyInTheirThread() {
        val host = InstrumentationRegistry.getArguments().getString("firestoreEmulatorHost")
        assumeTrue(host == "127.0.0.1" || host == "10.0.2.2")
        val port = InstrumentationRegistry.getArguments().getString("firestoreEmulatorPort")?.toIntOrNull() ?: 8188
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(
            ApplicationProvider.getApplicationContext<Context>(),
            FirebaseOptions.Builder().setProjectId("demo-olives-lottery")
                .setApplicationId("1:1234567890:android:sharedownerinboxtest")
                .setApiKey("emulator-only").build(),
            "shared-owner-inbox-$id"
        )
        try {
            val db = FirebaseFirestore.getInstance(app)
            db.useEmulator(requireNotNull(host), port)
            val first = "$id-first"
            val second = "$id-second"
            val ownerA = "$id-owner-a"
            val ownerB = "$id-owner-b"
            val entrantID = "$id-entrant"
            val groupID = "$id-group"
            val batch = db.batch()
            batch.set(db.collection("campsites").document(first), Campsite(
                "Pine", ownerA, first, mutableListOf(1), 0f, coordinates = mutableListOf()
            ).apply { sharedLotteryID = groupID }.toMap())
            batch.set(db.collection("campsites").document(second), Campsite(
                "Cedar", ownerB, second, mutableListOf(1), 0f, coordinates = mutableListOf()
            ).apply { sharedLotteryID = groupID }.toMap())
            batch.set(db.collection("lotteries").document(groupID), mapOf(
                "ownerID" to ownerA, "ownerIDs" to listOf(ownerA, ownerB),
                "campsiteIDs" to listOf(first, second),
                "lotteryStartDate" to lotteryNow().minusDays(1).toString(),
                "lotteryEndDate" to lotteryNow().plusDays(1).toString(),
                "waitingList" to listOf(entrantID), "winnerCount" to 1
            ))
            batch.set(db.collection("Entrants").document(entrantID), mapOf(
                "name" to "Shared Camper", "username" to "camper",
                "campsiteLists" to mapOf(ACTIVE_CAMPSITES to listOf(first))
            ))
            batch.set(db.collection("messages").document("$id-first-message"), ChatMessage(
                campsiteID = first, campsiteName = "Pine", entrantID = entrantID,
                ownerID = ownerA, text = "Private Pine message", fromOwner = false
            ))
            batch.set(db.collection("messages").document("$id-second-message"), ChatMessage(
                campsiteID = second, campsiteName = "Cedar", entrantID = entrantID,
                ownerID = ownerB, text = "Question for Cedar", fromOwner = false
            ))
            Tasks.await(batch.commit(), 15, TimeUnit.SECONDS)

            val campsites = DatabaseCampsites(db)
            val entrants = DatabaseEntrants(db)
            val chat = DatabaseChat(db)
            val state = AppState(
                admins = mockk(relaxed = true), owners = mockk(relaxed = true),
                campsites = campsites, entrants = entrants,
                alerts = mockk(relaxed = true), chat = chat
            ).apply { userID = ownerB; role = "owner" }
            compose.setContent { CampsiteChatScreen(rememberNavController(), state) }
            compose.waitUntil(15_000) {
                campsites.campsites.any { it.campsiteID == second } &&
                    campsites.sharedLotteries.find(groupID) != null &&
                    entrants.entrants.any { it.getUserID() == entrantID } &&
                    chat.conversation(second, entrantID).isNotEmpty()
            }
            compose.onNodeWithText("Shared Camper").assertIsDisplayed().performClick()
            compose.onNodeWithText("Question for Cedar").assertIsDisplayed()
            compose.onNodeWithText("Private Pine message").assertDoesNotExist()
            compose.onNode(hasSetTextAction()).performTextInput("Reply from Cedar")
            compose.onNodeWithText("Send").performClick()
            compose.waitUntil(15_000) {
                chat.conversation(second, entrantID).any { it.text == "Reply from Cedar" }
            }
            val replies = Tasks.await(db.collection("messages")
                .whereEqualTo("entrantID", entrantID).get(Source.SERVER), 15, TimeUnit.SECONDS)
            assertTrue(replies.documents.any {
                it.getString("text") == "Reply from Cedar" && it.getString("ownerID") == ownerB &&
                    it.getString("campsiteID") == second
            })
        } finally {
            app.delete()
        }
    }

    private fun campsite(id: String, name: String, owner: String) =
        Campsite(name, owner, id, mutableListOf(), 0f, coordinates = mutableListOf())

    private fun entrant(id: String, name: String, list: String, campsiteID: String) =
        Entrant(userID = id, name = name).apply { addCampsite(list, campsiteID) }

    private fun shared(id: String, sites: List<String>, entrantIDs: List<String>) = SharedLottery(
        id, "owner-a", sites, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
        null, 5, entrantIDs, emptyList(), false, emptyMap(), ownerIDs = listOf("owner-a", "owner-b")
    )
}

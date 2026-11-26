package com.example.code

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.Entrant
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.test.onAllNodesWithText

@RunWith(AndroidJUnit4::class)
class CampsiteOwnerMessagesTest {
    @get:Rule
    val compose = createComposeRule()

    /** Compare the owner inbox with current Firestore reservations without changing any records. */
    @Test
    fun liveOwnerInboxesReflectConfirmedReservations() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("readOwnerReservations") == "true")
        val db = FirebaseFirestore.getInstance()
        val owners = Tasks.await(db.collection("CampsiteOwners").get(Source.SERVER), 30, TimeUnit.SECONDS)
        val campsites = Tasks.await(db.collection("campsites").get(Source.SERVER), 30, TimeUnit.SECONDS)
        val entrants = Tasks.await(db.collection("Entrants").get(Source.SERVER), 30, TimeUnit.SECONDS)
        val state = AppState().apply { role = "owner" }
        compose.setContent { CampsiteChatScreen(rememberNavController(), state) }
        compose.waitUntil(30_000) {
            val loadedEntrants = state.entrants.entrants.map { it.getUserID() }.toSet()
            val loadedSites = state.campsites.campsites.map { it.campsiteID }.toSet()
            entrants.documents.all { it.id in loadedEntrants } &&
                campsites.documents.filter { it.getString("name") != null }.all { it.id in loadedSites }
        }

        for (owner in owners.documents) {
            val sites = campsites.documents.filter { site ->
                (site.get("ownerID") ?: site.get("OwnerID") ?: site.get("ownerId"))
                    ?.toString()?.trim() == owner.id
            }
            val rows = sites.sortedBy { it.getString("name") }.flatMap { site ->
                entrants.documents.filter { entrant ->
                    val lists = entrant.get("campsiteLists") as? Map<*, *>
                    (lists?.get(RESERVED_CAMPSITES) as? List<*>)?.contains(site.id) == true
                }.map { entrant ->
                    entrant.getString("name").orEmpty().ifBlank {
                        entrant.getString("username") ?: entrant.id
                    }
                }.sorted()
            }
            compose.runOnIdle { state.userID = owner.id }
            if (rows.isEmpty()) {
                compose.onNodeWithText("No entrants with confirmed reservations.").assertIsDisplayed()
            } else {
                compose.onAllNodesWithText(rows.first())[0].assertIsDisplayed()
            }
            println("Owner inbox audit: ${owner.id}, ${sites.size} campsites, ${rows.size} confirmed reservations")
        }
    }

    @Test
    fun ownerCanOpenAndMessageOnlyConfirmedEntrantsAtTheirCampsites() {
        val pine = campsite("pine", "Pine Camp", "owner")
        val lake = campsite("lake", "Lake Camp", "owner")
        val other = campsite("other", "Other Camp", "other-owner")
        val testEntrants = listOf(
            entrant("alice", "Alice", RESERVED_CAMPSITES, pine.campsiteID),
            entrant("ben", "Ben", RESERVED_CAMPSITES, lake.campsiteID),
            entrant("waiting", "Waiting Entrant", WAITING_LIST, pine.campsiteID),
            entrant("past", "Past Entrant", PAST_CAMPSITES, pine.campsiteID),
            entrant("other", "Other Owner Entrant", RESERVED_CAMPSITES, other.campsiteID)
        )
        val pineMessages = mutableStateListOf(ChatMessage(text = "Pine arrival question"))
        val chat = mockk<DatabaseChat>(relaxed = true)
        every { chat.conversation("pine", "alice") } answers { pineMessages.toList() }
        every { chat.conversation("lake", "ben") } returns listOf(ChatMessage(text = "Lake arrival question"))
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
        compose.onNodeWithText("Waiting Entrant").assertDoesNotExist()
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
    }

    private fun campsite(id: String, name: String, owner: String) =
        Campsite(name, owner, id, mutableListOf(), 0f, coordinates = mutableListOf())

    private fun entrant(id: String, name: String, list: String, campsiteID: String) =
        Entrant(userID = id, name = name).apply { addCampsite(list, campsiteID) }
}

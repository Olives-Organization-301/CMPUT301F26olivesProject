package com.example.code

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.Entrant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampsiteOwnerMessagesTest {
    @get:Rule
    val compose = createComposeRule()

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

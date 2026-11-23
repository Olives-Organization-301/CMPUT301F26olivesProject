package com.example.code

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.Coordinates
import com.example.code.classes.Entrant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** Test Entrant Campsite Details Screen (joining / leaving a waiting list)
 * Uses MockK in order to Simulate the database without adding actual Data.
 * The mocks act like a tiny in-memory database: joining or leaving changes the same
 * Entrant / Campsite objects the screens load, so the result carries across screens.
 *
 * Mock data: 1 entrant, 5 campsites
 *   Entrant (John): Active = c_2   Past = c_4   Waiting = (empty)
 *   Every test joins c_1 (Banff National Park) and c_3 (Yosemite Valley), then leaves only Banff
 *
 * Filled in with Claude Opus 5.5
 * Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
 * Date: 19-Nov-2026
 */
@RunWith(AndroidJUnit4::class)
class EntrantCampsiteDetailsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Builds an AppState whose databases are MockK stand-ins, so the UI tests never touch Firestore.
     * - campsites.campsites / searchCampsite(id): the given campsites (search + details screens)
     * - entrants.searchEntrants(id): the given user (details + reservations screens)
     * - addToWaitingList / addEntrantCampsite: add the IDs to the in-memory objects, report success
     * - removeFromWaitingList / removeEntrantCampsite: remove them again, report success
     * - owners.searchCampsiteOwner(id): a fixed owner
     * - userID is set to the user's ID, as if they had just logged in
     */
    private fun fakeAppState(user: Entrant, sites: List<Campsite>): AppState {
        val campsites = mockk<DatabaseCampsites>(relaxed = true)
        every { campsites.campsites } returns sites
        every { campsites.searchCampsite(any(), any()) } answers {
            val id = firstArg<String>()
            secondArg<(Campsite?) -> Unit>()(sites.find { it.campsiteID == id })
        }
        every { campsites.addToWaitingList(any(), any(), any()) } answers {
            firstArg<Campsite>().addEntrant(secondArg())
            thirdArg<(Boolean) -> Unit>()(true)
        }
        // Called from the details screen (has the Campsite object)
        every { campsites.removeFromWaitingList(any<Campsite>(), any(), any()) } answers {
            firstArg<Campsite>().removeEntrant(secondArg())
            thirdArg<(Boolean) -> Unit>()(true)
        }
        // Called from the reservations list (only has the campsite ID)
        every { campsites.removeFromWaitingList(any<String>(), any(), any()) } answers {
            val id = firstArg<String>()
            sites.find { it.campsiteID == id }?.removeEntrant(secondArg())
            thirdArg<(Boolean) -> Unit>()(true)
        }

        val entrants = mockk<DatabaseEntrants>(relaxed = true)
        every { entrants.entrants } returns listOf(user)
        every { entrants.searchEntrants(any(), any()) } answers {
            val id = firstArg<String>()
            secondArg<(Entrant?) -> Unit>()(if (user.getUserID() == id) user else null)
        }
        every { entrants.addEntrantCampsite(any(), any(), any(), any()) } answers {
            firstArg<Entrant>().addCampsite(secondArg(), thirdArg())
            arg<(Boolean) -> Unit>(3)(true)
        }
        every { entrants.removeEntrantCampsite(any(), any(), any()) } answers {
            firstArg<Entrant>().removeCampsite(secondArg(), thirdArg())
        }

        val owners = mockk<DatabaseCampsiteOwners>(relaxed = true)
        every { owners.searchCampsiteOwner(any(), any()) } answers {
            secondArg<(CampsiteOwner?) -> Unit>()(
                CampsiteOwner("Parks Canada", "parks@example.com", firstArg())
            )
        }

        val appState = AppState(
            admins = mockk(relaxed = true),
            owners = owners,
            campsites = campsites,
            entrants = entrants,
            alerts = mockk(relaxed = true)
        )
        appState.userID = user.getUserID()
        return appState
    }

    // The logged-in entrant: already has an active and a past campsite, nothing on a waiting list
    private fun userTest() = Entrant(
        username = "jdoe",
        email = "jdoe@example.com",
        userID = "e_1",
        name = "John Doe"
    ).apply {
        addCampsite("Active_Campsites", "c_2")
        addCampsite("Past_Campsites", "c_4")
    }

    private fun testSite(id: String, name: String, country: String, month: Int) = Campsite(
        name = name,
        ownerID = "owner_1",
        campsiteID = id,
        maxOccupancy = 4,
        price = 50.0f,
        rating = 4.5f,
        capacity = 4,
        coordinates = Coordinates(country, "", 0.0, 0.0),
        startDate = LocalDateTime.of(2026, month, 1, 9, 0),
        endDate = LocalDateTime.of(2026, month, 6, 17, 0)
    )

    private fun campsite1() = testSite("c_1", "Banff National Park", "Canada", 1)  // the one we join
    private fun campsite2() = testSite("c_2", "Jasper Lake", "Canada", 2)          // user's active
    private fun campsite3() = testSite("c_3", "Yosemite Valley", "USA", 3)
    private fun campsite4() = testSite("c_4", "Waterton Lakes", "Canada", 4)       // user's past
    private fun campsite5() = testSite("c_5", "Zion Canyon", "USA", 5)

    private fun allCampsites() = listOf(campsite1(), campsite2(), campsite3(), campsite4(), campsite5())

    // ---------- Navigation helpers ----------

    // Start the app on the search screen (so ✕ / popBackStack has somewhere to go back to)
    private fun startApp(appState: AppState) {
        composeTestRule.setContent {
            CampsiteNavController(appState, startDestination = "mainSearchScreen")
        }
        composeTestRule.waitForIdle()
    }

    // From the search screen: search for the campsite and open its details.
    // (Type a partial name: the full name would also match the search box itself.)
    private fun openCampsite(search: String, name: String) {
        composeTestRule.onNodeWithText("Search for a campsite").performTextReplacement(search)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(name).performClick()
        composeTestRule.waitForIdle()
        // Details screen header shows the campsite name
        composeTestRule.onNodeWithText(name).assertIsDisplayed()
    }

    // Details screen → back to search (the ✕ in the header)
    private fun closeDetails() {
        composeTestRule.onNodeWithText("✕").performClick()
        composeTestRule.waitForIdle()
    }

    // Search screen → reservations list → Waiting_List
    private fun openWaitingList() {
        composeTestRule.onNodeWithText("List").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select Your List", substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Waiting_List").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Waiting_List").assertIsDisplayed()
    }

    // Reservations list → back to search (the header ✕ is the first ✕ on the screen)
    private fun closeReservations() {
        composeTestRule.onAllNodesWithText("✕")[0].performClick()
        composeTestRule.waitForIdle()
    }

    // ---------- Assertion helpers ----------

    // Join → the success popup is the only popup, then close it
    private fun joinAndCheckPopup(name: String) {
        composeTestRule.onNodeWithText("Join Waiting List").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("You're on the waiting list!").assertIsDisplayed()
        composeTestRule.onNodeWithText("You've joined the waiting list for $name", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Couldn't join").assertDoesNotExist()
        composeTestRule.onNodeWithText("You've left the waiting list").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("OK").assertCountEquals(1)

        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("You're on the waiting list!").assertDoesNotExist()

        // Button switched to Leave
        composeTestRule.onNodeWithText("Leave Waiting List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Waiting List").assertDoesNotExist()
    }

    // From the search screen: open a campsite, join it, go back to search
    private fun joinFromSearch(search: String, site: Campsite) {
        openCampsite(search, site.getName())
        joinAndCheckPopup(site.getName())
        closeDetails()
    }

    // On the Waiting_List page: exactly the `joined` campsites are shown, none of the others
    private fun assertOnlyOnWaitingList(joined: List<Campsite>, sites: List<Campsite>) {
        val joinedIDs = joined.map { it.campsiteID }
        joined.forEach { composeTestRule.onNodeWithText(it.getName()).assertIsDisplayed() }
        sites.filter { it.campsiteID !in joinedIDs }.forEach {
            composeTestRule.onNodeWithText(it.getName()).assertDoesNotExist()
        }
    }

    // The entrant and the campsite both still record the join (the "database" side)
    private fun assertStillJoined(user: Entrant, site: Campsite) {
        assertTrue("e_1" in site.getWaitingList())
        assertTrue(site.campsiteID in user.getCampsiteLists()["Waiting_List"]!!)
    }

    private fun assertNotJoined(user: Entrant, site: Campsite) {
        assertFalse("e_1" in site.getWaitingList())
        assertFalse(site.campsiteID in user.getCampsiteLists()["Waiting_List"]!!)
    }

    // ---------- Tests ----------
    // Every test joins two campsites: Banff (c_1) and Yosemite (c_3).
    // Banff is the one that gets left; Yosemite must never be touched.

    // Start App in Search Screen, navigate to that campsite
    // Join that campsite with the button, check if popup shows (Check if it's only that)
    // Go to ReservationList and check if it's on the waiting list
    @Test
    fun joinCampsite() {
        val user = userTest()
        val sites = allCampsites()
        val banff = sites[0]
        val yosemite = sites[2]
        val appState = fakeAppState(user, sites)
        startApp(appState)

        openCampsite("Banff", banff.getName())

        // Only the Join button: "Join Active Lottery" was removed
        composeTestRule.onNodeWithText("Join Waiting List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Active Lottery", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Leave Waiting List").assertDoesNotExist()

        joinAndCheckPopup(banff.getName())
        closeDetails()
        joinFromSearch("Yosemite", yosemite)

        // Each join told both sides of the database, exactly once
        verify(exactly = 1) { appState.campsites.addToWaitingList(banff, "e_1", any()) }
        verify(exactly = 1) { appState.entrants.addEntrantCampsite(user, "Waiting_List", "c_1", any()) }
        verify(exactly = 1) { appState.campsites.addToWaitingList(yosemite, "e_1", any()) }
        verify(exactly = 1) { appState.entrants.addEntrantCampsite(user, "Waiting_List", "c_3", any()) }
        assertStillJoined(user, banff)
        assertStillJoined(user, yosemite)

        // Both are on the waiting list, nothing else
        openWaitingList()
        assertOnlyOnWaitingList(listOf(banff, yosemite), sites)
    }

    // Start App in Search Screen, navigate to that campsite
    // Join that campsite with the button, check if popup shows
    // Go to ReservationList and check if it's on the waiting list (Check if it's only that)
    // Go to Campsite Details again and leave, make sure popup exists
    // Go to reservation list to confirm removal
    @Test
    fun joinCampsiteLeaveInDetails() {
        val user = userTest()
        val sites = allCampsites()
        val banff = sites[0]
        val yosemite = sites[2]
        val appState = fakeAppState(user, sites)
        startApp(appState)

        joinFromSearch("Banff", banff)
        joinFromSearch("Yosemite", yosemite)

        openWaitingList()
        // Only Banff + Yosemite: not the user's active (Jasper) or past (Waterton) campsites, nor Zion
        assertOnlyOnWaitingList(listOf(banff, yosemite), sites)

        closeReservations()
        openCampsite("Banff", banff.getName())

        // Still shows as joined when coming back
        composeTestRule.onNodeWithText("Leave Waiting List").performClick()
        composeTestRule.waitForIdle()

        // Leave popup shows, names Banff, and it's the only popup
        composeTestRule.onNodeWithText("You've left the waiting list").assertIsDisplayed()
        composeTestRule.onNodeWithText("You've been removed from the waiting list for ${banff.getName()}", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(yosemite.getName(), substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Couldn't leave").assertDoesNotExist()
        composeTestRule.onNodeWithText("You're on the waiting list!").assertDoesNotExist()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("You've left the waiting list").assertDoesNotExist()

        // Button switched back to Join
        composeTestRule.onNodeWithText("Join Waiting List").assertIsDisplayed()

        // Only Banff was removed
        verify(exactly = 1) { appState.campsites.removeFromWaitingList(banff, "e_1", any()) }
        verify(exactly = 1) { appState.entrants.removeEntrantCampsite(user, "Waiting_List", "c_1") }
        verify(exactly = 0) { appState.campsites.removeFromWaitingList(yosemite, any(), any()) }
        verify(exactly = 0) { appState.campsites.removeFromWaitingList(eq("c_3"), any(), any()) }
        verify(exactly = 0) { appState.entrants.removeEntrantCampsite(any(), any(), "c_3") }
        assertNotJoined(user, banff)
        assertStillJoined(user, yosemite)

        // Waiting list now has only Yosemite
        closeDetails()
        openWaitingList()
        assertOnlyOnWaitingList(listOf(yosemite), sites)

        // Yosemite's details still say Leave
        closeReservations()
        openCampsite("Yosemite", yosemite.getName())
        composeTestRule.onNodeWithText("Leave Waiting List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Waiting List").assertDoesNotExist()
    }

    // Start App in Search Screen, navigate to that campsite
    // Join that campsite with the button, check if popup shows
    // Go to ReservationList and check if it's on the waiting list
    // Leave Campsite WaitingList in the reservation list
    // Go to Campsite details and check if it's "Join Campsite" again
    @Test
    fun joinCampsiteLeaveInList() {
        val user = userTest()
        val sites = allCampsites()
        val banff = sites[0]
        val yosemite = sites[2]
        val appState = fakeAppState(user, sites)
        startApp(appState)

        joinFromSearch("Banff", banff)
        joinFromSearch("Yosemite", yosemite)

        openWaitingList()
        assertOnlyOnWaitingList(listOf(banff, yosemite), sites)

        // Three ✕ on screen: the header's, then one per card in join order (Banff, Yosemite).
        // Remove Banff's card. If the wrong one were removed, the checks below would fail.
        composeTestRule.onAllNodesWithText("✕").assertCountEquals(3)
        composeTestRule.onAllNodesWithText("✕")[1].performClick()
        composeTestRule.waitForIdle()

        // Banff is gone, Yosemite is still listed
        assertOnlyOnWaitingList(listOf(yosemite), sites)
        composeTestRule.onAllNodesWithText("✕").assertCountEquals(2)

        // Only Banff was removed, from both the entrant's list and the campsite's waitingList
        verify(exactly = 1) { appState.entrants.removeEntrantCampsite(user, "Waiting_List", "c_1") }
        verify(exactly = 1) { appState.campsites.removeFromWaitingList(eq("c_1"), eq("e_1"), any()) }
        verify(exactly = 0) { appState.entrants.removeEntrantCampsite(any(), any(), "c_3") }
        verify(exactly = 0) { appState.campsites.removeFromWaitingList(eq("c_3"), any(), any()) }
        verify(exactly = 0) { appState.campsites.removeFromWaitingList(yosemite, any(), any()) }
        assertNotJoined(user, banff)

        // Still in the undeleted list: Yosemite's waiting list still has the user on both sides
        assertStillJoined(user, yosemite)

        // Banff's details: back to "Join Waiting List"
        closeReservations()
        openCampsite("Banff", banff.getName())
        composeTestRule.onNodeWithText("Join Waiting List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leave Waiting List").assertDoesNotExist()

        // Yosemite's details: still joined
        closeDetails()
        openCampsite("Yosemite", yosemite.getName())
        composeTestRule.onNodeWithText("Leave Waiting List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Waiting List").assertDoesNotExist()

        // And the reservations list still shows it after reopening
        closeDetails()
        openWaitingList()
        assertOnlyOnWaitingList(listOf(yosemite), sites)
    }
}

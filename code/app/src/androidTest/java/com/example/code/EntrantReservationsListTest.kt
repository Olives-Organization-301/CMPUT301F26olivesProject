package com.example.code

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.Coordinates
import com.example.code.classes.Entrant
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Test Entrant Reservations List Screen
 * Uses MockK in order to Simulate the database without adding actual Data.
 *
 * Mock data: 2 entrants, 6 campsites
 *   Entrant 1 (John): Active = c_1, c_2, c_3   Past = c_4, c_5
 *   Entrant 2 (Jane): Active = c_1, c_6        Past = c_4, c_5
 *   c_6 is the disjoint campsite: only Jane has it, so John should never see it.
 */
@RunWith(AndroidJUnit4::class)
class EntrantReservationsListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Same format the screen uses for the lottery dates
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())

    // Each campsite gets a different start date so its date line is unique on screen
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

    private val c1 = testSite("c_1", "Banff National Park", "Canada", 1)
    private val c2 = testSite("c_2", "Jasper Lake", "Canada", 2)
    private val c3 = testSite("c_3", "Yosemite Valley", "USA", 3)
    private val c4 = testSite("c_4", "Waterton Lakes", "Canada", 4)
    private val c5 = testSite("c_5", "Zion Canyon", "USA", 5)
    private val c6 = testSite("c_6", "Elk Island", "Canada", 6)   // disjoint: only Jane has it
    private val allSites = listOf(c1, c2, c3, c4, c5, c6)

    private fun entrantExample() = Entrant(
        username = "jdoe",
        email = "jdoe@example.com",
        userID = "e_1",
        name = "John Doe"
    ).apply {
        addCampsite("Active_Campsites", "c_1")
        addCampsite("Active_Campsites", "c_2")
        addCampsite("Active_Campsites", "c_3")
        addCampsite("Past_Campsites", "c_4")
        addCampsite("Past_Campsites", "c_5")
    }

    private fun entrantExample2() = Entrant(
        username = "jsmith",
        email = "jsmith@example.com",
        userID = "e_2",
        name = "Jane Smith"
    ).apply {
        addCampsite("Active_Campsites", "c_1")
        addCampsite("Active_Campsites", "c_6")
        addCampsite("Past_Campsites", "c_4")
        addCampsite("Past_Campsites", "c_5")
    }

    /**
     * Builds an AppState whose databases are MockK stand-ins, so the UI tests never touch Firestore.
     * - entrants.searchEntrants(id) hands back the entrant with that ID (what the screen loads)
     * - campsites.searchCampsite(id) hands back the campsite with that ID (what each card loads)
     * - userID is set to loggedIn's ID, as if they had just logged in
     */
    private fun fakeAppState(entrantList: List<Entrant>, loggedIn: Entrant): AppState {
        val entrants = mockk<DatabaseEntrants>(relaxed = true)
        every { entrants.entrants } returns entrantList
        every { entrants.searchEntrants(any(), any()) } answers {
            val id = firstArg<String>()
            secondArg<(Entrant?) -> Unit>()(entrantList.find { it.getUserID() == id })
        }

        val campsites = mockk<DatabaseCampsites>(relaxed = true)
        every { campsites.campsites } returns allSites
        every { campsites.searchCampsite(any(), any()) } answers {
            val id = firstArg<String>()
            secondArg<(Campsite?) -> Unit>()(allSites.find { it.campsiteID == id })
        }

        val appState = AppState(
            admins = mockk(relaxed = true),
            owners = mockk(relaxed = true),
            campsites = campsites,
            entrants = entrants,
            alerts = mockk(relaxed = true)
        )
        appState.userID = loggedIn.getUserID()
        return appState
    }

    private fun openReservations(loggedIn: Entrant, others: List<Entrant>) {
        val appState = fakeAppState(listOf(loggedIn) + others, loggedIn)
        composeTestRule.setContent {
            CampsiteNavController(appState, startDestination = "reservationsListScreen")
        }
        composeTestRule.waitForIdle()
    }

    // Opens the dropdown and picks a list
    private fun selectList(list: String) {
        composeTestRule.onNodeWithText("Select Your List", substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(list).performClick()
        composeTestRule.waitForIdle()
    }

    // A campsite card is shown with its name and both lottery dates
    private fun assertCardShown(site: Campsite) {
        composeTestRule.onNodeWithText(site.getName()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Lottery Entry: ${site.getStartDate()!!.format(dateFormatter)}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Lottery End: ${site.getEndDate()!!.format(dateFormatter)}")
            .assertIsDisplayed()
    }

    private fun assertCardNotShown(site: Campsite) {
        composeTestRule.onNodeWithText(site.getName()).assertDoesNotExist()
        composeTestRule.onNodeWithText(site.campsiteID).assertDoesNotExist()
    }

    @Test
    fun viewActiveCampsites() {
        openReservations(entrantExample(), listOf(entrantExample2()))

        // Active list is the default
        composeTestRule.onNodeWithText("Active_Campsites").assertIsDisplayed()

        // John's 3 active campsites are shown
        assertCardShown(c1)
        assertCardShown(c2)
        assertCardShown(c3)

        // Past campsites and Jane's disjoint campsite are not
        assertCardNotShown(c4)
        assertCardNotShown(c5)
        assertCardNotShown(c6)
    }

    @Test
    fun viewPastCampsites() {
        openReservations(entrantExample(), listOf(entrantExample2()))

        selectList("Past_Campsites")
        composeTestRule.onNodeWithText("Past_Campsites").assertIsDisplayed()

        // John's 2 past campsites are shown
        assertCardShown(c4)
        assertCardShown(c5)

        // Active campsites and Jane's disjoint campsite are not
        assertCardNotShown(c1)
        assertCardNotShown(c2)
        assertCardNotShown(c3)
        assertCardNotShown(c6)
    }

    @Test
    fun disjointCampsiteNotShown() {
        openReservations(entrantExample(), listOf(entrantExample2()))

        // John never sees Jane's disjoint campsite in any of his lists
        assertCardNotShown(c6)
        selectList("Past_Campsites")
        assertCardNotShown(c6)
        selectList("Waiting_List")
        composeTestRule.onNodeWithText("Waiting_List").assertIsDisplayed()
        assertCardNotShown(c6)
    }

    @Test
    fun secondEntrantSeesOwnLists() {
        openReservations(entrantExample2(), listOf(entrantExample()))

        // Jane's active list: the shared campsite and her disjoint one
        assertCardShown(c1)
        assertCardShown(c6)
        // John's other active campsites are not shown to Jane
        assertCardNotShown(c2)
        assertCardNotShown(c3)

        // Jane's past list
        selectList("Past_Campsites")
        assertCardShown(c4)
        assertCardShown(c5)
        assertCardNotShown(c1)
        assertCardNotShown(c6)
    }
}

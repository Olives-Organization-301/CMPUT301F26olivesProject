package com.example.code

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.abs

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

        // Move in-between lists once lottery has started
        // Lottery started: move the campsite from one of the entrant's lists to another
        every { entrants.moveEntrantCampsite(any(), any(), any(), any(), any()) } answers {
            val e = firstArg<Entrant>()
            val from = arg<String>(1)
            val to = arg<String>(2)
            val id = arg<String>(3)
            e.removeCampsite(from, id)
            e.addNewList(to)
            e.addCampsite(to, id)
            arg<(Boolean) -> Unit>(4)(true)
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
        maxOccupancy = mutableListOf(4),
        price = 50.0f,
        rating = 4.5f,
        coordinates = mutableListOf(Coordinates(country, "", "", "")),
        lotteryStartDate = LocalDateTime.of(2026, month, 1, 9, 0),
        lotteryEndDate = LocalDateTime.of(2026, month, 6, 17, 0)
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
        composeTestRule.onNodeWithText("Waiting List").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Waiting List").assertIsDisplayed()
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

    // =====================================================================
    // Available Dates calendar (the "Dates" button)
    // Checks that every date inside a start/end pair is highlighted (light blue, black text)
    // and that dates outside every pair are not.
    //
    // Mock data: 3 start/end pairs
    //   Jul 3 – Jul 6, 2026
    //   Jul 10 – Jul 12, 2026
    //   Jul 30 – Aug 2, 2026   (crosses into the next month)
    //
    // Written with Claude Opus 5.5, Date: 20-Nov-2026
    // https://claude.ai/share/e282e67a-f3db-4225-b23d-1cadc614a63f
    // =====================================================================

    // Initialize a set of dates
    private val availableDates = listOf(
        LocalDateTime.of(2026, 7, 3, 10, 0) to LocalDateTime.of(2026, 7, 6, 12, 0),
        LocalDateTime.of(2026, 7, 10, 9, 0) to LocalDateTime.of(2026, 7, 12, 17, 0),
        LocalDateTime.of(2026, 7, 30, 14, 0) to LocalDateTime.of(2026, 8, 2, 11, 0)
    )

    // Show only the calendar on a plain white background (so pixel colours are predictable)
    private fun showCalendar(
        dates: List<Pair<LocalDateTime, LocalDateTime>> = availableDates,
        initialMonth: YearMonth? = null
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    Box {
                        if (initialMonth == null) {
                            AvailableDatesCalendar(dates)
                        } else {
                            AvailableDatesCalendar(dates, initialMonth = initialMonth)
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    // Test Calendar Date
    private fun calendarDay(date: LocalDate) = composeTestRule.onNodeWithTag("calendarDay_$date")

    // To test of blurbs are highlighted, use this function to check if a date is highlighted
    private fun assertHighlighted(date: LocalDate) =
        calendarDay(date).assert(SemanticsMatcher.expectValue(DayHighlightedKey, true))

    // To test if dates aren't highlighted, match the date highlighting with false
    private fun assertNotHighlighted(date: LocalDate) =
        calendarDay(date).assert(SemanticsMatcher.expectValue(DayHighlightedKey, false))

    // Testing if July & August is used in the test (Highlighted dates, in the same location)
    private fun july(dayOfMonth: Int) = LocalDate.of(2026, 7, dayOfMonth)
    private fun august(dayOfMonth: Int) = LocalDate.of(2026, 8, dayOfMonth)

    private fun closeTo(a: Color, b: Color, tolerance: Float = 0.03f) =
        abs(a.red - b.red) < tolerance &&
            abs(a.green - b.green) < tolerance &&
            abs(a.blue - b.blue) < tolerance

    // Colour of the day cell just above its number (inside the highlight band, away from the text)
    private fun backgroundColourOf(date: LocalDate): Color {
        val pixels = calendarDay(date).captureToImage().toPixelMap()
        return pixels[pixels.width / 2, (pixels.height * 0.1f).toInt()]
    }

    // True if the day's number has (nearly) black pixels in it
    private fun hasBlackText(date: LocalDate): Boolean {
        val pixels = calendarDay(date).captureToImage().toPixelMap()
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                val c = pixels[x, y]
                if (c.red < 0.2f && c.green < 0.2f && c.blue < 0.2f) return true
            }
        }
        return false
    }

    // Test if Calendar opens in the first pair
    @Test
    fun calendarOpensOnFirstPairMonth() {
        showCalendar()
        composeTestRule.onNodeWithText("July 2026").assertIsDisplayed()
    }

    // Test if everything in-between pairs are highlighted
    @Test
    fun everyDateInsideEachPairIsHighlighted() {
        showCalendar()
        // Start and end days count as inside the pair
        (3..6).forEach { assertHighlighted(july(it)) }
        (10..12).forEach { assertHighlighted(july(it)) }
        (30..31).forEach { assertHighlighted(july(it)) }
    }

    // Test if everything outside of the pairs are highlighted
    @Test
    fun datesOutsideThePairsAreNotHighlighted() {
        showCalendar()
        // Before the first pair, the gap between pairs, and the day before the last pair
        listOf(1, 2, 7, 8, 9, 13, 20, 29).forEach { assertNotHighlighted(july(it)) }
    }

    // Test if highlights cross over to next month
    @Test
    fun pairCrossingIntoNextMonthIsHighlightedThere() {
        showCalendar()
        composeTestRule.onNodeWithTag("calendarNextMonth").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("August 2026").assertIsDisplayed()
        assertHighlighted(august(1))
        assertHighlighted(august(2))
        assertNotHighlighted(august(3))
    }

    // Test if no pairs are highlighted in a month
    @Test
    fun noPairsMeansNothingIsHighlighted() {
        showCalendar(dates = emptyList(), initialMonth = YearMonth.of(2026, 7))
        (1..31).forEach { assertNotHighlighted(july(it)) }
    }

    // Test dates are coloured correctly
    @Test
    fun highlightedDatesAreLightBlue() {
        showCalendar()
        // Start, middle and end of a pair
        listOf(3, 4, 6).forEach {
            assertTrue("July $it should be light blue", closeTo(backgroundColourOf(july(it)), CalendarHighlight))
        }
        // Outside every pair: plain white
        listOf(2, 8).forEach {
            val colour = backgroundColourOf(july(it))
            assertFalse("July $it should not be light blue", closeTo(colour, CalendarHighlight))
            assertTrue("July $it should be white", closeTo(colour, Color.White))
        }
    }

    // Test if text is coloured correctly
    @Test
    fun highlightedDatesHaveBlackText() {
        showCalendar()
        assertTrue("July 4 should have black text", hasBlackText(july(4)))
        assertFalse("July 8 should have greyed-out text", hasBlackText(july(8)))
    }

    // Test if campsite details show campsite dates
    @Test
    fun datesButtonShowsTheCampsitesDates() {
        // Banff with the pairs above as its available dates
        val banff = campsite1().apply { setVacationStartTimes(availableDates) }
        val appState = fakeAppState(userTest(), listOf(banff))
        appState.viewingCampsiteID = banff.campsiteID

        composeTestRule.setContent {
            CampsiteDetailsScreen(rememberNavController(), appState)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Dates").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Available Dates").assertIsDisplayed()
        assertHighlighted(july(3))
        assertHighlighted(july(11))
        assertNotHighlighted(july(8))

        // Close hides the calendar again
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Available Dates").assertDoesNotExist()
    }

    // =====================================================================
    // Active lottery: on lottery day the button says "Join Active Lottery", and a campsite
    // already on the entrant's Waiting List is moved to their Active Campsites list
    // =====================================================================

    // Banff, with a lottery running from yesterday to tomorrow
    // It is a mock object variable
    private fun lotteryTodaySite() = Campsite(
        name = "Banff National Park",
        ownerID = "owner_1",
        campsiteID = "c_1",
        maxOccupancy = mutableListOf(4),
        price = 50.0f,
        rating = 4.5f,
        coordinates = mutableListOf(Coordinates("Canada", "Alberta", "Banff", "")),
        lotteryStartDate = LocalDateTime.now().minusDays(1),
        lotteryEndDate = LocalDateTime.now().plusDays(1)
    )

    // Search screen → reservations list (opens on Active Campsites)
    private fun openActiveCampsites() {
        composeTestRule.onNodeWithText("List").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Active Campsites").assertIsDisplayed()
    }

    // On the reservations list: switch to the Waiting List page
    private fun switchToWaitingList() {
        composeTestRule.onNodeWithText("Select Your List", substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Waiting List").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Waiting List").assertIsDisplayed()
    }

    // Test if you can join an active lottery if the assignment starts now
    @Test
    fun joinActiveLotteryOnLotteryDay() {
        val user = userTest()
        val banff = lotteryTodaySite()
        val appState = fakeAppState(user, listOf(banff))
        startApp(appState)
        openCampsite("Banff", banff.getName())

        // Lottery day: only the active lottery button
        composeTestRule.onNodeWithText("Join Active Lottery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Waiting List").assertDoesNotExist()

        composeTestRule.onNodeWithText("Join Active Lottery").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("You're in the active lottery!").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Leave Active Lottery").assertIsDisplayed()

        // On lottery day the entrant still goes into the campsite's single waitingList
        verify(exactly = 1) { appState.campsites.addToWaitingList(banff, "e_1", any()) }
        assertTrue("e_1" in banff.getWaitingList())

        // Reservations list: Banff is on Active Campsites...
        closeDetails()
        openActiveCampsites()
        composeTestRule.onNodeWithText(banff.getName()).assertIsDisplayed()

        // ...and not on the Waiting List
        switchToWaitingList()
        composeTestRule.onNodeWithText(banff.getName()).assertDoesNotExist()
    }

    // Test if waiting lists move to lottery day on the day
    @Test
    fun waitingListCampsiteMovesToActiveLotteryOnLotteryDay() {
        val user = userTest().apply { addCampsite("Waiting_List", "c_1") }
        val banff = lotteryTodaySite().apply { addEntrant("e_1") }
        val appState = fakeAppState(user, listOf(banff))
        startApp(appState)
        openCampsite("Banff", banff.getName())

        // Opening the campsite on lottery day moved it on the entrant's side;
        // the entrant stays on the campsite's waitingList (the lottery pool)
        verify(exactly = 1) {
            appState.entrants.moveEntrantCampsite(user, "Waiting_List", "Active_Campsites", "c_1", any())
        }
        assertTrue("e_1" in banff.getWaitingList())
        composeTestRule.onNodeWithText("Leave Active Lottery").assertIsDisplayed()

        // Reservations list: Banff is now on Active Campsites...
        closeDetails()
        openActiveCampsites()
        composeTestRule.onNodeWithText(banff.getName()).assertIsDisplayed()

        // ...and no longer on the Waiting List
        switchToWaitingList()
        composeTestRule.onNodeWithText(banff.getName()).assertDoesNotExist()
    }
}

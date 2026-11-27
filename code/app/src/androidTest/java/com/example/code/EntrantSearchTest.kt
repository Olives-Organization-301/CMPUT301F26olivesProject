package com.example.code

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.Coordinates
import com.example.code.classes.SearchFilters
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** Test Entrant Search Screen
 * Filled in with Sonnet 5.0
 * Prompt: "Can you finish this prompt for me?"
 * Conversation: https://claude.ai/share/ed96cd27-4c3b-46bf-b0dc-6139be152aba
 * Date: 13-Nov-2026
 *
 * Used Fable 5.2 in Order to
 * Uses MockK in order to Simulate the database without adding actual Data.
 * Conversation: https://claude.ai/share/490264be-ae03-4bef-98c9-fabfd8e235e8
 * Date: 16-Nov-2026
 */
@RunWith(AndroidJUnit4::class)
class EntrantSearchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLevenshteinDist() {
        // Test Equality
        val equalDist = levenshteinDist("Banff National Park", "Banff National Park")
        assertEquals(0, equalDist)

        // Any Arbitrary non-equal Number
        val nonEqualDist = levenshteinDist("Jasper National Park", "Banff National Park")
        assertEquals(5, nonEqualDist)

        // Another Arbitrary Test
        val nonEqualDist2 = levenshteinDist("Yoko State Park", "Yoho National Area")
        assertEquals(11, nonEqualDist2)

        // 1 Difference
        val diff1 = levenshteinDist("Yosemite State Park", "Gosemite State Park")
        assertEquals(1, diff1)

    }

    // Campsite has been modified, so initialization has been changed
    @Test
    fun filterTest() {
        // Fixed test data so every filter has a campsite that passes and one that fails
        fun site(name: String, country: String, occupancy: Int, start: LocalDateTime?) = Campsite(
            name = name,
            ownerID = "owner_1",
            campsiteID = "c_$name",
            maxOccupancy = mutableListOf(occupancy),
            price = 50.0f,
            rating = 4.8f,
            coordinates = mutableListOf(Coordinates(country, "", "", "")),
            lotteryStartDate = start,
            lotteryEndDate = start?.plusDays(5)
        )

        val july1 = LocalDateTime.of(2026, 7, 1, 0, 0)
        val sites = listOf(
            site("Banff", "Canada", 4, july1),
            site("Jasper", "Canada", 8, LocalDateTime.of(2026, 8, 15, 0, 0)),
            site("Yosemite", "USA", 2, LocalDateTime.of(2026, 6, 1, 0, 0)),
            site("Zion", "USA", 6, null) // no start date set
        )

        // Names of the campsites that pass the given filters
        fun names(filters: SearchFilters) = sites.filter { filters.matches(it) }.map { it.getName() }

        // No filters: everything matches
        assertEquals(listOf("Banff", "Jasper", "Yosemite", "Zion"), names(SearchFilters()))

        // Country filter
        assertEquals(listOf("Banff", "Jasper"), names(SearchFilters(country = "Canada")))
        assertEquals(listOf("Yosemite", "Zion"), names(SearchFilters(country = "USA")))
        // Case-insensitive
        assertEquals(listOf("Banff", "Jasper"), names(SearchFilters(country = "cAnAdA")))
        // Blank country is treated as no filter
        assertEquals(4, names(SearchFilters(country = "   ")).size)
        // Country with no campsites
        assertTrue(names(SearchFilters(country = "Mexico")).isEmpty())

        // Minimum guests filter (campsite occupancy must be >= minGuests)
        assertEquals(listOf("Jasper", "Zion"), names(SearchFilters(minGuests = 5)))
        // Boundary: occupancy equal to minGuests still matches
        assertEquals(listOf("Banff", "Jasper", "Zion"), names(SearchFilters(minGuests = 4)))
        assertEquals(listOf("Jasper"), names(SearchFilters(minGuests = 8)))
        // Larger than every campsite
        assertTrue(names(SearchFilters(minGuests = 9)).isEmpty())

        // Maximum guests filter (campsite occupancy must be <= maxGuests)
        assertEquals(listOf("Banff", "Yosemite"), names(SearchFilters(maxGuests = 5)))
        // Boundary: occupancy equal to maxGuests still matches
        assertEquals(listOf("Banff", "Yosemite"), names(SearchFilters(maxGuests = 4)))
        assertEquals(listOf("Yosemite"), names(SearchFilters(maxGuests = 2)))
        // Smaller than every campsite
        assertTrue(names(SearchFilters(maxGuests = 1)).isEmpty())

        // Minimum and maximum guests together
        assertEquals(listOf("Banff", "Zion"), names(SearchFilters(minGuests = 4, maxGuests = 6)))
        assertEquals(listOf("Jasper"), names(SearchFilters(minGuests = 8, maxGuests = 8)))
        // Impossible range (min > max) matches nothing
        assertTrue(names(SearchFilters(minGuests = 6, maxGuests = 4)).isEmpty())

        // From-date filter (campsite start date must be on or after fromDate)
        // Boundary: start date equal to fromDate still matches.
        // Zion has no start date, so the date filter never excludes it.
        assertEquals(listOf("Banff", "Jasper", "Zion"), names(SearchFilters(fromDate = july1)))
        assertEquals(listOf("Jasper", "Zion"), names(SearchFilters(fromDate = july1.plusDays(1))))
        // Earlier than every start date: everything matches
        assertEquals(4, names(SearchFilters(fromDate = LocalDateTime.of(2026, 1, 1, 0, 0))).size)
        // Later than every start date: only the campsite with no date is left
        assertEquals(listOf("Zion"), names(SearchFilters(fromDate = LocalDateTime.of(2027, 1, 1, 0, 0))))

        // All filters combined
        val combined = SearchFilters(
            fromDate = july1.plusDays(1),
            country = "Canada",
            minGuests = 5,
            maxGuests = 8
        )
        assertEquals(listOf("Jasper"), names(combined))
        // Failing a single filter is enough to be excluded
        assertFalse(combined.copy(country = "USA").matches(sites[1]))
        assertFalse(combined.copy(maxGuests = 7).matches(sites[1]))
        assertFalse(combined.copy(minGuests = 9).matches(sites[1]))
        assertFalse(combined.copy(fromDate = LocalDateTime.of(2026, 9, 1, 0, 0)).matches(sites[1]))
    }

    /**
     * Builds an AppState whose databases are MockK stand-ins, so the UI tests never touch Firestore.
     * - campsites.campsites returns the given list (what the search screen shows)
     * - campsites.searchCampsite(id) hands back the campsite with that ID (what the details screen loads)
     * - owners.searchCampsiteOwner(id) hands back a fixed owner
     */
    private fun fakeAppState(sites: List<Campsite>): AppState {
        val campsites = mockk<DatabaseCampsites>(relaxed = true)
        every { campsites.campsites } returns sites
        every { campsites.searchCampsite(any(), any()) } answers {
            val id = firstArg<String>()
            secondArg<(Campsite?) -> Unit>()(sites.find { it.campsiteID == id })
        }

        val owners = mockk<DatabaseCampsiteOwners>(relaxed = true)
        every { owners.searchCampsiteOwner(any(), any()) } answers {
            secondArg<(CampsiteOwner?) -> Unit>()(
                CampsiteOwner("Parks Canada", "parks@example.com", firstArg())
            )
        }

        return AppState(
            admins = mockk(relaxed = true),
            owners = owners,
            campsites = campsites,
            entrants = mockk(relaxed = true),
            alerts = mockk(relaxed = true)
        )
    }

    // Campsite has been modified, so initialization has been changed
    private fun testSite(id: String, name: String, country: String, occupancy: Int) = Campsite(
        name = name,
        ownerID = "owner_1",
        campsiteID = id,
        maxOccupancy = mutableListOf(occupancy),
        price = 50.0f,
        rating = 4.8f,
        coordinates = mutableListOf(Coordinates(country, "", "", "")),
        lotteryStartDate = LocalDateTime.of(2026, 7, 1, 0, 0),
        lotteryEndDate = LocalDateTime.of(2026, 7, 6, 0, 0)
    )

    @Test
    fun searchTest() {
        val appState = fakeAppState(
            listOf(
                testSite("c_1", "Banff Springs", "Canada", 4),
                testSite("c_2", "Jasper Lake", "Canada", 8)
            )
        )
        composeTestRule.setContent {
            CampsiteNavController(appState, startDestination = "mainSearchScreen")
        }

        // Both campsites are listed before anything is typed
        composeTestRule.onNodeWithText("Banff Springs").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jasper Lake").assertIsDisplayed()

        // Empty search: "Jasper Lake" is the closer match (shorter name), so it is listed first
        var banffTop = composeTestRule.onNodeWithText("Banff Springs").getUnclippedBoundsInRoot().top
        var jasperTop = composeTestRule.onNodeWithText("Jasper Lake").getUnclippedBoundsInRoot().top
        assertTrue(jasperTop < banffTop)

        // Searching "Banff" moves "Banff Springs" to the top of the list.
        // (Type a partial name: the full name would also match the search box itself.)
        composeTestRule.onNodeWithText("Search for a campsite").performTextInput("Banff")
        composeTestRule.waitForIdle()

        banffTop = composeTestRule.onNodeWithText("Banff Springs").getUnclippedBoundsInRoot().top
        jasperTop = composeTestRule.onNodeWithText("Jasper Lake").getUnclippedBoundsInRoot().top
        assertTrue(banffTop < jasperTop)
    }

    @Test
    fun clickToCampsiteDetails() {
        val appState = fakeAppState(
            listOf(
                testSite("c_1", "Banff National Park", "Canada", 4),
                testSite("c_2", "Yosemite Valley", "USA", 2)
            )
        )
        composeTestRule.setContent {
            CampsiteNavController(appState, startDestination = "mainSearchScreen")
        }

        // Search for the campsite and open it
        composeTestRule.onNodeWithText("Search for a campsite").performTextInput("Banff")
        composeTestRule.onNodeWithText("Banff National Park").performClick()
        composeTestRule.waitForIdle()

        // The search screen recorded which campsite was opened
        assertEquals("c_1", appState.viewingCampsiteID)

        // The details screen shows that campsite's information
        composeTestRule.onNodeWithText("Banff National Park").assertIsDisplayed()
        composeTestRule.onNodeWithText("Parks Canada").assertIsDisplayed()
        composeTestRule.onNodeWithText("Registration opens: 2026-07-01").assertIsDisplayed()
        composeTestRule.onNodeWithText("Registration closes: 2026-07-06").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rating: 4.8/5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max Occupancy: 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Location: Canada").assertIsDisplayed()

        // The other campsite's details are not shown
        composeTestRule.onNodeWithText("Yosemite Valley").assertDoesNotExist()
    }
}

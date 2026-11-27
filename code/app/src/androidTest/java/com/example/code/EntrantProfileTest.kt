package com.example.code

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.Entrant
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test Entrant Profile Screen
 * Uses MockK in order to Simulate the database without adding actual Data.
 * Tests: Viewing the Profile, Viewing Policy, Editing Profile User Settings, Deleting a Profile
 */
@RunWith(AndroidJUnit4::class)
class EntrantProfileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun stubImageWrites() {
        mockkStatic("com.example.code.EntrantProfileScreenKt")
        justRun { saveImageRecordToFirestore(any(), any(), any(), any(), any()) }
        justRun { deleteEntrantImagesFromFirestore(any()) }
    }

    @After
    fun restoreImageWrites() {
        unmockkStatic("com.example.code.EntrantProfileScreenKt")
    }

    /**
     * Builds an AppState whose databases are MockK stand-ins, so the UI tests never touch Firestore.
     * - entrants.entrants returns the fake "database" list
     * - entrants.searchEntrants(id) hands back the entrant with that ID (what the profile screen loads)
     * - entrants.deleteEntrant(e) removes that entrant from the fake list
     * - entrants.updateEntrant does nothing (relaxed); check it with verify { }
     * - userID is set to loggedIn's ID, as if they had just logged in
     */
    private fun fakeAppState(
        allEntrants: List<Entrant>,
        loggedIn: Entrant,
        messages: List<ChatMessage> = emptyList(),
    ): AppState {
        val db = allEntrants.toMutableList()

        val entrants = mockk<DatabaseEntrants>(relaxed = true)
        every { entrants.entrants } answers { db.toList() }
        every { entrants.searchEntrants(any(), any()) } answers {
            val id = firstArg<String>()
            secondArg<(Entrant?) -> Unit>()(db.find { it.getUserID() == id })
        }
        every { entrants.deleteEntrant(any()) } answers {
            val target = firstArg<Entrant>()
            db.removeAll { it.getUserID() == target.getUserID() }
        }

        val campsites = mockk<DatabaseCampsites>(relaxed = true)
        every { campsites.campsites } returns listOf(
            campsite("banff-site", "Banff National Park"),
            campsite("jasper-site", "Jasper Lake"),
            campsite("yoho-site", "Yoho Valley")
        )
        val chat = mockk<DatabaseChat>(relaxed = true)
        every { chat.messages } returns messages
        val appState = AppState(
            admins = mockk(relaxed = true),
            owners = mockk(relaxed = true),
            campsites = campsites,
            entrants = entrants,
            alerts = mockk(relaxed = true),
            chat = chat
        )
        appState.userID = loggedIn.getUserID()
        return appState
    }

    private fun campsite(id: String, name: String): Campsite = mockk {
        every { campsiteID } returns id
        every { getName() } returns name
    }

    // Main entrant: used for viewing, policy and editing
    private fun johnDoe() = Entrant(
        username = "jdoe",
        email = "jdoe@example.com",
        userID = "e_1",
        phoneNumber = "780-555-0123",
        description = "I like camping.",
        name = "John Doe",
        dateOfBirth = "2001-04-23",
        profileImage = ""   // No profile picture (blank)
    ).apply {
        addCampsite(PAST_CAMPSITES, "banff-site")
        addCampsite(PAST_CAMPSITES, "jasper-site")
    }

    // Second entrant: the one that gets deleted
    private fun janeSmith() = Entrant(
        username = "jsmith",
        email = "jsmith@example.com",
        userID = "e_2",
        phoneNumber = "587-555-0199",
        description = "Delete me.",
        name = "Jane Smith",
        dateOfBirth = "1999-12-31",
        profileImage = ""   // No profile picture (blank)
    ).apply {
        addCampsite(PAST_CAMPSITES, "yoho-site")
    }

    private fun openProfile(appState: AppState) {
        composeTestRule.setContent {
            CampsiteNavController(appState, startDestination = "profileScreen")
        }
        composeTestRule.waitForIdle()
    }

    // Finds an editable text field in the Edit dialog by its label
    // (the label alone could also match text on the profile screen behind the dialog)
    private fun field(label: String) =
        composeTestRule.onNode(hasText(label) and hasSetTextAction())

    @Test
    fun viewProfileTest() {
        val john = johnDoe()
        val jane = janeSmith()
        openProfile(fakeAppState(listOf(john, jane), loggedIn = john))

        // Finished loading
        composeTestRule.onNodeWithText("Loading…").assertDoesNotExist()

        // Basic details
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("@jdoe").assertIsDisplayed()
        composeTestRule.onNodeWithText("2001-04-23").assertIsDisplayed()
        composeTestRule.onNodeWithText("jdoe@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("780-555-0123").assertIsDisplayed()

        // Close button
        composeTestRule.onNodeWithText("✕").assertIsDisplayed()

        // About Me
        composeTestRule.onNodeWithText("About Me").assertIsDisplayed()
        composeTestRule.onNodeWithText("I like camping.").assertIsDisplayed()

        // Recent Campsites
        composeTestRule.onNodeWithText("Recent Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banff National Park").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jasper Lake").assertIsDisplayed()

        // Buttons
        composeTestRule.onNodeWithText("Policy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit Profile").assertIsDisplayed()

        // The other entrant's information is not shown
        composeTestRule.onNodeWithText("Jane Smith").assertDoesNotExist()
        composeTestRule.onNodeWithText("@jsmith").assertDoesNotExist()
        composeTestRule.onNodeWithText("Yoho Valley").assertDoesNotExist()
    }

    /** Codex prompt: "ok show the campsite name and booked stay dates and make test cases for those" */
    @Test
    fun recentCampsitesShowNamesAndBookedDatesForTheSignedInEntrant() {
        val john = johnDoe()
        val booked = ChatMessage(
            campsiteID = "banff-site",
            entrantID = john.getUserID(),
            type = RESERVATION_CONFIRMED,
            locationIndex = 0,
            stayDates = listOf("2025-06-03", "2025-06-01", "2025-06-03"),
            sentAt = 1
        )
        val cancelled = booked.copy(locationIndex = 1, stayDates = listOf("2025-06-04"))
        val cancellation = cancelled.copy(type = RESERVATION_DECLINED, sentAt = 2)
        val otherEntrant = booked.copy(entrantID = "e_2", stayDates = listOf("2025-06-05"), sentAt = 3)
        openProfile(fakeAppState(listOf(john), john, listOf(booked, cancelled, cancellation, otherEntrant)))

        composeTestRule.onNodeWithText("Banff National Park").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booked stay dates: 2025-06-01, 2025-06-03").assertIsDisplayed()
        composeTestRule.onNodeWithText("banff-site").assertDoesNotExist()
        composeTestRule.onNodeWithText("Jasper Lake").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booked stay dates unavailable").assertIsDisplayed()
    }

    @Test
    fun viewPolicyTest() {
        val john = johnDoe()
        openProfile(fakeAppState(listOf(john, janeSmith()), loggedIn = john))

        // Policy is not open yet
        composeTestRule.onNodeWithText("User Policy").assertDoesNotExist()

        composeTestRule.onNodeWithText("Policy").performClick()
        composeTestRule.waitForIdle()

        // Dialog title and every section (scroll to each since the dialog scrolls)
        composeTestRule.onNodeWithText("User Policy").assertIsDisplayed()
        listOf(
            "1. Accounts",
            "2. Reservations",
            "3. Respect the Campsite",
            "4. Conduct",
            "5. Your Data",
            "6. Changes"
        ).forEach { section ->
            composeTestRule.onNodeWithText(section).performScrollTo().assertIsDisplayed()
        }

        // Closing the dialog goes back to the profile
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("User Policy").assertDoesNotExist()
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
    }

    @Test
    fun editProfileTest() {
        val john = johnDoe()
        val jane = janeSmith()
        val appState = fakeAppState(listOf(john, jane), loggedIn = john)
        openProfile(appState)

        composeTestRule.onNodeWithText("Edit Profile").performClick()
        composeTestRule.waitForIdle()

        // Dialog starts with the current values
        field("Name").assert(hasText("John Doe"))
        field("Username").assert(hasText("jdoe"))
        field("Date of Birth").assert(hasText("2001-04-23"))
        field("Email").assert(hasText("jdoe@example.com"))
        field("Phone Number").assert(hasText("780-555-0123"))
        field("About Me").assert(hasText("I like camping."))

        // Change every field
        fun replace(label: String, text: String) {
            field(label).performScrollTo()
            field(label).performTextClearance()
            field(label).performTextInput(text)
        }
        replace("Name", "Johnny Doe")
        replace("Username", "jdoe2")
        replace("Date of Birth", "2002-05-24")
        replace("Email", "johnny@example.com")
        replace("Phone Number", "780-555-9999")
        replace("About Me", "I love camping.")

        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Saved to the database exactly once
        verify(exactly = 1) { appState.entrants.updateEntrant(john) }
        verify(exactly = 0) { appState.entrants.deleteEntrant(any()) }

        // The entrant object was updated
        assertEquals("Johnny Doe", john.name)
        assertEquals("jdoe2", john.username)
        assertEquals("2002-05-24", john.dateOfBirth)
        assertEquals("johnny@example.com", john.email)
        assertEquals("780-555-9999", john.phoneNumber)
        assertEquals("I love camping.", john.description)

        // The profile screen shows the new values, and the old ones are gone
        composeTestRule.onNodeWithText("Johnny Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("@jdoe2").assertIsDisplayed()
        composeTestRule.onNodeWithText("2002-05-24").assertIsDisplayed()
        composeTestRule.onNodeWithText("johnny@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("780-555-9999").assertIsDisplayed()
        composeTestRule.onNodeWithText("I love camping.").assertIsDisplayed()
        composeTestRule.onNodeWithText("John Doe").assertDoesNotExist()
        composeTestRule.onNodeWithText("@jdoe").assertDoesNotExist()

        // The other entrant was not touched
        assertEquals("Jane Smith", jane.name)
        assertEquals("jsmith", jane.username)
    }

    @Test
    fun deleteProfileTest() {
        val john = johnDoe()
        val jane = janeSmith()
        val appState = fakeAppState(listOf(john, jane), loggedIn = jane)
        openProfile(appState)

        // Logged in as Jane
        composeTestRule.onNodeWithText("Jane Smith").assertIsDisplayed()
        assertEquals(2, appState.entrants.entrants.size)

        composeTestRule.onNodeWithText("Edit Profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete Profile").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Jane was deleted from the database, John was not
        verify(exactly = 1) { appState.entrants.deleteEntrant(jane) }
        verify(exactly = 0) { appState.entrants.deleteEntrant(john) }
        val remaining = appState.entrants.entrants
        assertEquals(1, remaining.size)
        assertTrue(remaining.any { it.getUserID() == "e_1" })
        assertFalse(remaining.any { it.getUserID() == "e_2" })

        // Logged out and sent back to the login screen
        assertNull(appState.userID)
        composeTestRule.onNodeWithText("Log in").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign up").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Smith").assertDoesNotExist()
    }

    // US.01.08.02
    // Test if it's blank initially, then add first default picture
    // Once first default picture shows up, update it to the second default picture
    // After that, delete the profile picture and test that it's blank
    // Select the built-in avatars through their thumbnails.
    @Test
    fun testProfilePicture() {
        val john = johnDoe()
        val appState = fakeAppState(listOf(john, janeSmith()), loggedIn = john)

        // Fake Firestore: remember the profileImage string each time updateEntrant saves it
        val firestore = mutableMapOf<String, String>()
        every { appState.entrants.updateEntrant(any()) } answers {
            val saved = firstArg<Entrant>()
            firestore[saved.getUserID()] = saved.profileImage
        }
        openProfile(appState)

        // Picture images are drawn with this description; the blank placeholder has none
        fun pictures() = composeTestRule.onAllNodesWithContentDescription("Profile picture")
        fun openEdit() {
            composeTestRule.onNodeWithText("Edit Profile").performClick()
            composeTestRule.waitForIdle()
        }
        fun selectAvatar(description: String) {
            composeTestRule.onNodeWithContentDescription(description).performScrollTo().performClick()
            composeTestRule.waitForIdle()
        }
        // Save is one of the dialog's own buttons, outside the scrolling content, so don't scroll to it
        fun save() {
            composeTestRule.onNodeWithText("Save").performClick()
            composeTestRule.waitForIdle()
        }

        // 1. Blank initially
        assertEquals("", john.profileImage)
        pictures().assertCountEquals(0)

        openEdit()
        composeTestRule.onNodeWithText("Choose from Gallery").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove").assertDoesNotExist()

        // 2. Add the first default picture
        selectAvatar("Test Profile Avatar")
        composeTestRule.onNodeWithText("Remove").performScrollTo().assertIsDisplayed()
        // Nothing is saved until Save is pressed
        assertEquals("", john.profileImage)
        assertTrue(firestore.isEmpty())
        save()

        assertEquals(TEST_PFP_PROFILE, john.profileImage)
        assertEquals(TEST_PFP_PROFILE, firestore["e_1"])
        pictures().assertCountEquals(1)   // shown on the profile screen

        // 3. Update it to the second default picture
        openEdit()
        // The saved picture is loaded back into the dialog
        composeTestRule.onNodeWithText("Remove").performScrollTo().assertIsDisplayed()
        selectAvatar("Test Gift Avatar")
        save()

        assertEquals(TEST_PFP_GIFT, john.profileImage)
        assertEquals(TEST_PFP_GIFT, firestore["e_1"])
        pictures().assertCountEquals(1)

        // 4. Remove it: blank again
        openEdit()
        composeTestRule.onNodeWithText("Remove").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Remove").assertDoesNotExist()
        save()

        assertEquals("", john.profileImage)
        assertEquals("", firestore["e_1"])
        pictures().assertCountEquals(0)

        // One save per change, and the other entrant was never saved
        verify(exactly = 3) { appState.entrants.updateEntrant(john) }
        assertFalse("e_2" in firestore)
    }
}

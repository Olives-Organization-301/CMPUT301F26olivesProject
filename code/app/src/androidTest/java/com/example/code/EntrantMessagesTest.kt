package com.example.code

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.Coordinates
import com.example.code.classes.Entrant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** Test Entrant Messages Screen
 * Uses MockK in order to simulate the database without adding actual data.
 *
 * Mock data: 1 entrant (e_1), 5 campsites they're messaging in.
 * The lottery results are set by hand (not random), then sendLotteryMessages() sends the same
 * automated messages the real lottery would.
 *   c_1 Banff National Park: 2 locations, lost both
 *   c_2 Jasper Lake:         2 locations, won both
 *   c_3 Yosemite Valley:     2 locations, won one (location 2)
 *   c_4 Waterton Lakes:      1 location,  won
 *   c_5 Zion Canyon:         1 location,  lost
 * Every campsite's dates are December 10 - 14, 2026.
 *
 * Every test also checks the phone's real notifications (NotificationManager.activeNotifications):
 * e_1 should get one notification per lottery result: 6 in total
 * (c_1 lost, c_2 won x2, c_3 won, c_4 won, c_5 lost), and none for ordinary chat messages.
 * https://claude.ai/share/c834d783-5930-4800-8f67-da339fafedfd
 */
@RunWith(AndroidJUnit4::class)
class EntrantMessagesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val user = "e_1"
    private val other = "e_other"   // whoever won the spots e_1 didn't

    // One notification per lottery result (see the class comment)
    private val totalResults = 6

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val notificationManager get() = context.getSystemService(NotificationManager::class.java)

    @Before
    fun allowNotifications() {
        // Android 13+: grant the permission the profile settings would normally ask for
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        notificationManager.cancelAll()
    }

    @After
    fun clearNotifications() {
        notificationManager.cancelAll()
    }

    // Campsite where e_1 is on the waiting list and the lottery is already drawn
    // winners: location index -> who won it
    private fun drawnSite(id: String, name: String, winners: List<String>) = Campsite(
        name = name,
        ownerID = "owner_$id",
        campsiteID = id,
        maxOccupancy = MutableList(winners.size) { 1 },
        price = 50.0f,
        coordinates = MutableList(winners.size) { Coordinates("Canada", "Alberta", name, "") },
        lotteryStartDate = LocalDateTime.of(2026, 11, 1, 9, 0),
        lotteryEndDate = LocalDateTime.of(2026, 11, 7, 17, 0),
        vacationStartTimes = mutableListOf(
            LocalDateTime.of(2026, 12, 10, 0, 0) to LocalDateTime.of(2026, 12, 14, 0, 0)
        )
    ).apply {
        addEntrant(user)
        addEntrant(other)
        setLocationWinners(winners.mapIndexed { i, id -> i.toString() to listOf(id) }.toMap())
        setFinalList(winners.distinct())
    }

    private val c1 = drawnSite("c_1", "Banff National Park", listOf(other, other))
    private val c2 = drawnSite("c_2", "Jasper Lake", listOf(user, user))
    private val c3 = drawnSite("c_3", "Yosemite Valley", listOf(other, user))
    private val c4 = drawnSite("c_4", "Waterton Lakes", listOf(user))
    private val c5 = drawnSite("c_5", "Zion Canyon", listOf(other))
    private val allSites = listOf(c1, c2, c3, c4, c5)

    // The entrant was in all 5 lotteries
    private fun entrantExample() = Entrant(
        username = "jdoe",
        email = "jdoe@example.com",
        userID = user,
        name = "John Doe"
    ).apply {
        allSites.forEach { addCampsite(ACTIVE_CAMPSITES, it.campsiteID) }
    }

    /**
     * Builds an AppState whose databases are MockK stand-ins, so the UI tests never touch Firestore.
     * - chat keeps its messages in a plain list (the fake "messages" collection);
     *   addMessage / conversation / conversationsFor work on that list like the real ones
     * - entrants.searchEntrants(id) hands back the entrant
     * - entrants.moveEntrantCampsite / addEntrantCampsite really move the campsite in the
     *   entrant's lists (used when a spot is reserved)
     * - campsites.searchCampsite(id) hands back the campsite with that ID
     * - every campsite's lottery result has been sent as automated messages
     */
    private fun fakeAppState(entrant: Entrant): AppState {
        val messages = mutableStateListOf<ChatMessage>()
        val chat = mockk<DatabaseChat>(relaxed = true)
        every { chat.messages } returns messages
        every { chat.addMessage(any()) } answers {
            val m = firstArg<ChatMessage>()
            if (m.messageID.isBlank()) m.messageID = java.util.UUID.randomUUID().toString()
            messages.add(m)
        }
        every { chat.conversation(any(), any()) } answers {
            messages.filter { it.campsiteID == firstArg<String>() && it.entrantID == secondArg<String>() }
                .sortedBy { it.sentAt }
        }
        every { chat.conversationsFor(any()) } answers {
            messages.filter { it.entrantID == firstArg<String>() }
                .groupBy { it.campsiteID }
                .mapValues { (_, msgs) -> msgs.sortedBy { it.sentAt } }
        }

        val entrants = mockk<DatabaseEntrants>(relaxed = true)
        every { entrants.entrants } returns listOf(entrant)
        every { entrants.searchEntrants(any(), any()) } answers {
            secondArg<(Entrant?) -> Unit>()(if (firstArg<String>() == entrant.getUserID()) entrant else null)
        }
        every { entrants.moveEntrantCampsite(any(), any(), any(), any(), any()) } answers {
            val e = firstArg<Entrant>()
            val id = arg<String>(3)
            e.removeCampsite(arg(1), id)
            e.addNewList(arg(2))
            e.addCampsite(arg(2), id)
            arg<(Boolean) -> Unit>(4)(true)
        }
        every { entrants.addEntrantCampsite(any(), any(), any(), any()) } answers {
            val e = firstArg<Entrant>()
            e.addNewList(secondArg())
            e.addCampsite(secondArg(), thirdArg())
            arg<(Boolean) -> Unit>(3)(true)
        }

        val campsites = mockk<DatabaseCampsites>(relaxed = true)
        every { campsites.campsites } returns allSites
        every { campsites.searchCampsite(any(), any()) } answers {
            secondArg<(Campsite?) -> Unit>()(allSites.find { it.campsiteID == firstArg<String>() })
        }

        val appState = AppState(
            admins = mockk(relaxed = true),
            owners = mockk(relaxed = true),
            campsites = campsites,
            entrants = entrants,
            alerts = mockk(relaxed = true),
            chat = chat
        )
        appState.userID = entrant.getUserID()
        appState.role = "entrant"   // lottery notifications are only for entrants

        // Same automated messages drawLottery() sends
        allSites.forEach { sendLotteryMessages(appState, it) }
        return appState
    }

    private fun openMessages(appState: AppState) {
        composeTestRule.setContent {
            // Same as MainActivity: watches for lottery results and posts the phone notifications
            LotteryNotificationEffect(appState)
            CampsiteNavController(appState, startDestination = "entrantMessages")
        }
        composeTestRule.waitForIdle()
    }

    /** A notification the phone is currently showing for this app */
    private data class PostedNotification(val title: String, val text: String)

    private fun postedNotifications(): List<PostedNotification> =
        notificationManager.activeNotifications
            // Android bundles 4+ notifications under a summary; only count the real ones
            .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .mapNotNull { sbn ->
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?: return@mapNotNull null
                PostedNotification(title, extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty())
            }

    // Notifications are posted asynchronously, so wait until the phone shows all of them
    private fun waitForLotteryNotifications() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) { postedNotifications().size >= totalResults }
        assertEquals(totalResults, postedNotifications().size)
    }

    private fun wonNotifications(site: Campsite) =
        postedNotifications().filter { it.title == "You won the lottery for ${site.getName()}!" }

    private fun lostNotifications(site: Campsite) =
        postedNotifications().filter { it.title == "Lottery results for ${site.getName()}" }

    // No notification mentions [text] (e.g. an ordinary chat message)
    private fun assertNoNotificationFor(text: String) {
        assertTrue(postedNotifications().none { text in it.title || text in it.text })
    }

    // From the conversation list, open the chat with a campsite
    private fun openChat(site: Campsite) {
        composeTestRule.onNodeWithText(site.getName()).performClick()
        composeTestRule.waitForIdle()
    }

    // From a chat, go back to the conversation list
    private fun backToList() {
        composeTestRule.onNodeWithText("‹ Back").performClick()
        composeTestRule.waitForIdle()
    }

    private fun wonText(location: Int, site: Campsite) =
        "You won a place in location $location at ${site.getName()}"

    private fun lostText(site: Campsite) = "I'm Sorry, you lost the lottery for ${site.getName()}."

    private fun sendChat(text: String) {
        composeTestRule.onNode(hasSetTextAction()).performTextInput(text)
        composeTestRule.onNodeWithText("Send").performClick()
        composeTestRule.waitForIdle()
    }

    // US 01.03.01
    // User goes to see campsite 4, and sees that they won its one location
    // User goes to see campsite 3, and sees that they won one of its two locations
    @Test
    fun testWinningNotification() {
        openMessages(fakeAppState(entrantExample()))
        waitForLotteryNotifications()

        // Every campsite has a conversation
        allSites.forEach { composeTestRule.onNodeWithText(it.getName()).assertIsDisplayed() }

        // Campsite 4: won its only location
        openChat(c4)
        composeTestRule.onNodeWithText("AUTOMATED MESSAGE").assertIsDisplayed()
        composeTestRule.onNodeWithText(wonText(1, c4), substring = true).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Select Dates").assertCountEquals(1)
        composeTestRule.onNodeWithText(lostText(c4)).assertDoesNotExist()
        // ...and the phone showed one "you won" notification for it
        val c4Won = wonNotifications(c4)
        assertEquals(1, c4Won.size)
        assertTrue(wonText(1, c4) in c4Won.single().text)
        assertTrue(lostNotifications(c4).isEmpty())
        backToList()

        // Campsite 3: won location 2 only
        openChat(c3)
        composeTestRule.onNodeWithText(wonText(2, c3), substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(wonText(1, c3), substring = true).assertDoesNotExist()
        composeTestRule.onAllNodesWithText("Select Dates").assertCountEquals(1)
        composeTestRule.onNodeWithText(lostText(c3)).assertDoesNotExist()
        // ...and the phone showed one notification, for location 2
        val c3Won = wonNotifications(c3)
        assertEquals(1, c3Won.size)
        assertTrue(wonText(2, c3) in c3Won.single().text)
        assertFalse(wonText(1, c3) in c3Won.single().text)
        assertTrue(lostNotifications(c3).isEmpty())
    }

    // US 01.03.02
    // User goes to see campsite 5, and sees that they lost
    // Chats "Damn", and that's it
    @Test
    fun testLosingNotification() {
        val appState = fakeAppState(entrantExample())
        openMessages(appState)
        waitForLotteryNotifications()

        openChat(c5)
        composeTestRule.onNodeWithText("AUTOMATED MESSAGE").assertIsDisplayed()
        composeTestRule.onNodeWithText(lostText(c5)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Select Dates").assertDoesNotExist()
        // ...and the phone showed one "lost" notification for it
        val c5Lost = lostNotifications(c5)
        assertEquals(1, c5Lost.size)
        assertEquals(lostText(c5), c5Lost.single().text)
        assertTrue(wonNotifications(c5).isEmpty())

        sendChat("Damn")
        composeTestRule.onNodeWithText("Damn").assertIsDisplayed()
        verify(exactly = 1) {
            appState.chat.addMessage(match {
                it.text == "Damn" && !it.fromOwner && !it.automated &&
                    it.campsiteID == "c_5" && it.entrantID == user
            })
        }
        // The entrant's own message doesn't notify anyone on this phone
        assertNoNotificationFor("Damn")
        assertEquals(totalResults, postedNotifications().size)
        backToList()

        // Campsite 1: lost both locations, but gets one "lost" message (not one per location)
        openChat(c1)
        composeTestRule.onAllNodesWithText(lostText(c1)).assertCountEquals(1)
        composeTestRule.onNodeWithText("Select Dates").assertDoesNotExist()
        // One notification too, not one per location
        assertEquals(1, lostNotifications(c1).size)
        assertTrue(wonNotifications(c1).isEmpty())
    }

    // US 01.04.01
    // User goes to campsite 2 and sees that they won two locations
    // User can select dates for one spot out of those
    @Test
    fun testAvailableDates() {
        val entrant = entrantExample()
        openMessages(fakeAppState(entrant))
        waitForLotteryNotifications()

        // The phone showed a "you won" notification for each of c_2's locations
        val c2Won = wonNotifications(c2)
        assertEquals(2, c2Won.size)
        assertTrue(c2Won.any { wonText(1, c2) in it.text })
        assertTrue(c2Won.any { wonText(2, c2) in it.text })
        assertTrue(lostNotifications(c2).isEmpty())

        openChat(c2)
        composeTestRule.onNodeWithText(wonText(1, c2), substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(wonText(2, c2), substring = true).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Select Dates").assertCountEquals(2)

        // Location 1's calendar
        composeTestRule.onAllNodesWithText("Select Dates")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select Dates: Location 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 day(s) selected").assertIsDisplayed()

        // A day outside the campsite's dates can't be picked
        composeTestRule.onNodeWithTag("reserveDay_2026-12-01").performClick()
        composeTestRule.onNodeWithText("0 day(s) selected").assertIsDisplayed()

        // Days inside the dates can
        composeTestRule.onNodeWithTag("reserveDay_2026-12-11").performClick()
        composeTestRule.onNodeWithTag("reserveDay_2026-12-12").performClick()
        composeTestRule.onNodeWithText("2 day(s) selected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm").performClick()
        composeTestRule.waitForIdle()

        // Location 1 is picked, location 2 is still open
        composeTestRule.onNodeWithText("Select Dates: Location 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("You picked 2 day(s).").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Change Dates").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Select Dates").assertCountEquals(1)

        // Reserving moved the campsite into the entrant's Reserved Campsites
        assertTrue("c_2" in entrant.getCampsiteLists()[RESERVED_CAMPSITES].orEmpty())
        assertFalse("c_2" in entrant.getCampsiteLists()[ACTIVE_CAMPSITES].orEmpty())
    }

    // US 01.14.01
    // Make sure User is able to message in the campsite
    // TODO: Make sure Campsite Owner sees it (Don't do it for now)
    @Test
    fun messageCampsiteOwner() {
        val appState = fakeAppState(entrantExample())
        openMessages(appState)
        waitForLotteryNotifications()

        openChat(c4)
        sendChat("Is the site dog friendly?")

        // Shown in the chat as the entrant's own message
        composeTestRule.onNodeWithText("Is the site dog friendly?").assertIsDisplayed()
        // The text box was cleared
        composeTestRule.onNodeWithText("Type a message...").assertIsDisplayed()

        // Sent once, addressed to the campsite's owner
        verify(exactly = 1) {
            appState.chat.addMessage(match {
                it.text == "Is the site dog friendly?" && !it.fromOwner &&
                    it.campsiteID == "c_4" && it.entrantID == user && it.ownerID == "owner_c_4"
            })
        }
        // ...and is now part of the conversation the owner would load
        assertTrue(appState.chat.conversation("c_4", user).any { it.text == "Is the site dog friendly?" })
        // Ordinary chat messages don't create notifications; only the lottery results are showing
        assertNoNotificationFor("Is the site dog friendly?")
        assertEquals(totalResults, postedNotifications().size)
    }
}

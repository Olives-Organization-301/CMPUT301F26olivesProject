package com.example.code

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.rememberNavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.ChatMessage
import com.example.code.classes.Entrant
import com.example.code.classes.Review
import com.example.code.classes.ReviewReply
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.ListenerRegistration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Codex prompt: Check draw deadlines, final stay dates, and review/reply reloads.
 * The lottery listing and waitlist cases cover the follow-up prompts on [LotteryDialog].
 */
class CampsiteLifecycleTest {
    @Test
    fun ownerCanEditWaitlistCapacityFromTheLotteryMenu() {
        val now = lotteryNow()
        val site = Campsite("Pine Camp", "owner", "pine", mutableListOf(1), 10f,
            coordinates = mutableListOf(), lotteryStartDate = now.minusDays(1), lotteryEndDate = now.plusDays(2))
        site.setWaitingList(listOf("first", "second"))
        val campsites = mockk<DatabaseCampsites>(relaxed = true)
        every { campsites.campsites } returns listOf(site)
        every { campsites.getCampsitesForOwner("owner", any()) } answers {
            secondArg<(List<Campsite>) -> Unit>()(listOf(site))
        }
        every { campsites.updateLotteryListing("pine", "owner", any(), any(), 3) } returns Tasks.forResult(true)
        val owners = mockk<DatabaseCampsiteOwners>(relaxed = true)
        every { owners.searchCampsiteOwner("owner", any()) } answers {
            secondArg<(CampsiteOwner?) -> Unit>()(null)
        }
        val state = AppState(
            admins = mockk(relaxed = true), owners = owners, campsites = campsites,
            entrants = mockk(relaxed = true), alerts = mockk(relaxed = true), chat = mockk(relaxed = true)
        ).apply {
            userID = "owner"
            role = "owner"
        }

        compose.setContent { CampsiteOwnerMainScreen(rememberNavController(), state) }
        compose.onNodeWithText("Menu").performClick()
        compose.onNodeWithText("Lottery").performClick()
        compose.onNodeWithText("Waitlist capacity").performTextReplacement("1")
        compose.onNodeWithText("Save lottery").assertIsNotEnabled()
        compose.onNodeWithText("Waitlist capacity").performTextReplacement("3")
        compose.onNodeWithText("Save lottery").performClick()

        compose.runOnIdle {
            verify(exactly = 1) { campsites.updateLotteryListing("pine", "owner", any(), any(), 3) }
        }
    }
    @Test
    fun waitlistCapacityAllowsOneLastPlaceAndAnIdempotentRetry() {
        val close = LocalDateTime.of(2026, 12, 9, 12, 0)
        val site = Campsite("Pine", "owner", "pine", mutableListOf(1), 10f,
            coordinates = mutableListOf(), lotteryEndDate = close)
        val now = close.minusMinutes(1)
        site.waitlistCapacity = 1

        assertEquals(WaitlistJoinResult.JOINED, site.waitlistJoinResult("first", now))
        site.addEntrant("first")
        assertEquals(WaitlistJoinResult.FULL, site.waitlistJoinResult("second", now))
        assertEquals(WaitlistJoinResult.JOINED, site.waitlistJoinResult("first", now))
        assertEquals(WaitlistJoinResult.CLOSED, site.waitlistJoinResult("second", close))
        site.waitlistCapacity = null
        assertEquals(WaitlistJoinResult.JOINED, site.waitlistJoinResult("second", now))
    }

    @Test
    fun listingEditsRejectInvalidDatesAndLimitsBelowCurrentEnrollment() {
        val start = LocalDateTime.of(2026, 12, 8, 12, 0)
        val end = start.plusDays(2)
        val site = Campsite("Pine", "owner", "pine", mutableListOf(1), 10f,
            coordinates = mutableListOf(), lotteryEndDate = end)
        site.setWaitingList(listOf("first", "second"))

        assertFalse(site.canEditLottery(start, end, 1, start))
        assertFalse(site.canEditLottery(start, end, 0, start))
        assertFalse(site.canEditLottery(end, start, null, start))
        assertFalse(site.canEditLottery(start, end.plusDays(1), 3, end))
        assertTrue(site.canEditLottery(start, end, 2, start))
        assertTrue(site.canEditLottery(start, end, null, start))
    }
    @get:Rule
    val compose = createComposeRule()

    private val close = LocalDateTime.of(2026, 12, 4, 23, 59)
    private val campsite = Campsite(
        name = "Lake Trail", ownerID = "owner", campsiteID = "site",
        maxOccupancy = mutableListOf(3), price = 30f, coordinates = mutableListOf(),
        lotteryStartDate = close.minusDays(1), lotteryEndDate = close
    )

    @Test
    fun lotteryOpensDrawAtCloseAndBuildsOneResultPerEntrant() {
        assertFalse(campsite.isReadyToDraw(close.minusSeconds(1)))
        assertTrue(campsite.isReadyToDraw(close))
        campsite.setWaitingList((1..5).map { "entrant-$it" })
        campsite.generateFinalList()

        val messages = lotteryResultMessages(campsite, 1000L)
        assertEquals(3, messages.count { it.type == LOTTERY_WON })
        assertEquals(2, messages.count { it.type == LOTTERY_LOST })
        assertEquals(5, messages.map { it.entrantID }.distinct().size)
        assertFalse(campsite.isReadyToDraw(close.plusDays(1)))
    }

    @Test
    fun reservationStaysCurrentThroughTheLastConfirmedDay() {
        val today = LocalDate.of(2026, 12, 9)
        val first = ChatMessage(type = RESERVATION_CONFIRMED, locationIndex = 0, sentAt = 1,
            stayDates = listOf(today.minusDays(1).toString()))
        val second = ChatMessage(type = RESERVATION_CONFIRMED, locationIndex = 1, sentAt = 2,
            stayDates = listOf(today.toString()))
        assertFalse(reservationHasEnded(listOf(first, second), today))
        assertTrue(reservationHasEnded(listOf(first, second), today.plusDays(1)))
        assertFalse(reservationHasEnded(listOf(first.copy(stayDates = emptyList())), today))
        val declined = second.copy(type = RESERVATION_DECLINED, sentAt = 3, stayDates = emptyList())
        assertTrue(reservationHasEnded(listOf(first, second, declined), today))
    }

    @Test
    fun ownerDashboardShowsReservationHistoryForTheSelectedCampsite() {
        val campsites = mockk<DatabaseCampsites>(relaxed = true)
        every { campsites.campsites } returns listOf(campsite)
        every { campsites.getCampsitesForOwner("owner", any()) } answers {
            secondArg<(List<Campsite>) -> Unit>()(listOf(campsite))
        }
        val pastGuest = Entrant(userID = "past", name = "Past Guest").apply {
            addCampsite(PAST_CAMPSITES, "site")
        }
        val otherGuest = Entrant(userID = "other", name = "Other Guest").apply {
            addCampsite(PAST_CAMPSITES, "other-site")
        }
        val state = AppState(
            admins = mockk(relaxed = true),
            owners = mockk(relaxed = true),
            campsites = campsites,
            entrants = mockk<DatabaseEntrants>(relaxed = true).also {
                every { it.entrants } returns listOf(pastGuest, otherGuest)
            },
            alerts = mockk(relaxed = true),
            chat = mockk(relaxed = true)
        ).apply {
            userID = "owner"
            role = "owner"
        }
        every { state.chat.messages } returns listOf(
            ChatMessage(campsiteID = "site", ownerID = "owner", entrantID = "past",
                type = RESERVATION_CONFIRMED, locationIndex = 0,
                stayDates = listOf("2026-11-01")),
            ChatMessage(campsiteID = "other-site", ownerID = "owner", entrantID = "other",
                type = RESERVATION_CONFIRMED, locationIndex = 0,
                stayDates = listOf("2026-11-01"))
        )
        every { state.owners.searchCampsiteOwner("owner", any()) } answers {
            secondArg<(CampsiteOwner?) -> Unit>()(null)
        }

        compose.setContent { CampsiteOwnerMainScreen(rememberNavController(), state) }
        compose.onNodeWithText("Menu").performClick()
        compose.onNodeWithText("Reservations").performClick()
        compose.onNodeWithText("Past Guest").assertIsDisplayed()
        compose.onNodeWithText("Other Guest").assertDoesNotExist()
    }

    @Test
    fun signedInEntrantArchivesOnlyTheExpiredStay() {
        val entrant = Entrant(userID = "entrant").apply {
            addCampsite(RESERVED_CAMPSITES, "expired")
            addCampsite(RESERVED_CAMPSITES, "today")
        }
        val entrants = mockk<DatabaseEntrants>(relaxed = true)
        every { entrants.entrants } returns listOf(entrant)
        val chat = mockk<DatabaseChat>()
        val today = LocalDate.now()
        every { chat.messages } returns listOf(
            ChatMessage(campsiteID = "expired", entrantID = "entrant", type = RESERVATION_CONFIRMED,
                locationIndex = 0, sentAt = 12L, stayDates = listOf(today.minusDays(1).toString())),
            ChatMessage(campsiteID = "today", entrantID = "entrant", type = RESERVATION_CONFIRMED,
                locationIndex = 0, sentAt = 13L, stayDates = listOf(today.toString()))
        )
        val state = AppState(
            admins = mockk(), owners = mockk(), campsites = mockk(),
            entrants = entrants, alerts = mockk(), chat = chat
        ).apply {
            userID = "entrant"
            role = "entrant"
        }

        compose.setContent { ReservationHistoryEffect(state) }
        compose.runOnIdle {
            verify(exactly = 1) { entrants.archiveReservation(any(), any(), any()) }
            verify { entrants.archiveReservation(entrant, "expired", 12L) }
        }
    }

    @Test
    fun postedReviewAndReplyAppearAfterReopeningReviews() {
        val database = mockk<DatabaseCampsites>()
        every { database.campsites } returns listOf(campsite)
        every { database.refreshCampsiteRating("site") } returns Tasks.forResult<Void>(null)
        val reviewListener = mockk<ListenerRegistration>(relaxed = true)
        val replyListener = mockk<ListenerRegistration>(relaxed = true)
        val storedReviews = mutableListOf<Review>()
        val storedReplies = mutableListOf<ReviewReply>()
        var emitReviews: (List<Review>) -> Unit = {}
        var emitReplies: (List<ReviewReply>) -> Unit = {}
        every { database.observeReviews("site", any()) } answers {
            emitReviews = secondArg()
            emitReviews(storedReviews.toList())
            reviewListener
        }
        every { database.observeReviewReplies(any(), any()) } answers {
            emitReplies = secondArg()
            emitReplies(storedReplies.toList())
            replyListener
        }
        every { database.postReview(any()) } answers {
            storedReviews.add(firstArg())
            emitReviews(storedReviews.toList())
            Tasks.forResult(true)
        }
        every { database.postReviewReply(any()) } answers {
            storedReplies.add(firstArg())
            emitReplies(storedReplies.toList())
            Tasks.forResult(true)
        }

        val entrant = Entrant(name = "Demo Entrant", userID = "entrant")
        val showing = mutableStateOf(true)
        compose.setContent {
            if (showing.value) CampsiteReviewsTab(campsite, entrant, database, Color.Black)
        }
        compose.onAllNodes(androidx.compose.ui.test.hasText("☆"))[0].performClick()
        compose.onNodeWithText("Write a review").performTextInput("Quiet location")
        compose.onNodeWithText("Post review").performClick()
        compose.onNodeWithText("Quiet location").assertIsDisplayed()
        compose.onNodeWithText("Reply").performClick()
        compose.onNodeWithText("Write a reply").performTextInput("Agreed")
        compose.onNodeWithText("Post reply").performClick()
        compose.waitForIdle()

        compose.runOnIdle { showing.value = false }
        compose.waitForIdle()
        compose.runOnIdle { showing.value = true }
        compose.onNodeWithText("Quiet location").assertIsDisplayed()
        compose.onNodeWithText("Demo Entrant: Agreed").assertIsDisplayed()
        assertEquals(2, storedReviews.single().rating)
    }
}

package com.example.code

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
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

/** Codex prompt: Check draw deadlines, final stay dates, and review/reply reloads. */
class CampsiteLifecycleTest {
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
    fun ownerDashboardDrawsOnlyItsClosedLotteries() {
        fun site(id: String, owner: String, end: LocalDateTime) = Campsite(
            id, owner, id, mutableListOf(1), 10f, coordinates = mutableListOf(),
            lotteryEndDate = end
        )
        val now = LocalDateTime.now()
        val database = mockk<DatabaseCampsites>(relaxed = true)
        every { database.campsites } returns listOf(
            site("closed", "owner", now.minusMinutes(1)),
            site("open", "owner", now.plusMinutes(1)),
            site("other", "another-owner", now.minusMinutes(1))
        )
        every { database.drawClosedLottery("closed", "owner") } returns Tasks.forResult(true)
        val state = AppState(
            admins = mockk(relaxed = true), owners = mockk(relaxed = true),
            campsites = database, entrants = mockk(relaxed = true),
            alerts = mockk(relaxed = true), chat = mockk(relaxed = true)
        ).apply {
            userID = "owner"
            role = "owner"
        }

        compose.setContent { CampsiteOwnerMainScreen(rememberNavController(), state) }
        compose.runOnIdle {
            verify(exactly = 1) { database.drawClosedLottery(any(), any()) }
            verify { database.drawClosedLottery("closed", "owner") }
        }
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

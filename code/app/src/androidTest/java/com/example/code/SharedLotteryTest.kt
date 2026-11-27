package com.example.code

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.CampsiteOwner
import com.google.android.gms.tasks.Tasks
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

/** Tests the registration window, shared signup, and winner choice requested on [SharedLotteryRepository]. */
class SharedLotteryTest {
    @get:Rule
    val compose = createComposeRule()

    private val opens = LocalDateTime.of(2026, 12, 1, 8, 0)
    private val closes = opens.plusDays(3)

    @Test
    fun joinsAreAcceptedAtOpeningAndRejectedAtClosing() {
        assertEquals(WaitlistJoinResult.NOT_OPEN, registrationGate(opens, closes, false, opens.minusNanos(1)))
        assertEquals(WaitlistJoinResult.JOINED, registrationGate(opens, closes, false, opens))
        assertEquals(WaitlistJoinResult.JOINED, registrationGate(opens, closes, false, closes.minusNanos(1)))
        assertEquals(WaitlistJoinResult.CLOSED, registrationGate(opens, closes, false, closes))
    }

    @Test
    fun bothCampsitesShareOneEntryAndCapacity() {
        val lottery = fixture().copy(drawn = false, capacity = 1, entrants = listOf("first"))
        assertEquals(WaitlistJoinResult.JOINED, lottery.admission("first", opens))
        assertEquals(WaitlistJoinResult.FULL, lottery.admission("second", opens))
        assertEquals(WaitlistJoinResult.CLOSED, lottery.admission("first", closes))
    }

    @Test
    fun bookedNightsAndWinnerClaimsCannotBeUsedTwice() {
        val dates = listOf("2026-12-09")
        val lottery = fixture()
        assertTrue(lottery.canBook("first", "pine", 0, dates))
        val booked = lottery.copy(bookings = mapOf(
            "first" to SharedLotteryBooking("pine", 0, dates, RESERVATION_CONFIRMED)
        ))
        assertFalse(booked.canBook("second", "pine", 0, dates))
        assertTrue(booked.canBook("second", "cedar", 0, dates))
        assertTrue(booked.canBook("second", "pine", 1, dates))
        assertFalse(booked.canBook("first", "cedar", 0, dates))
        assertFalse(booked.canBook("non-winner", "cedar", 0, dates))
        assertFalse(booked.canBook("second", "unlisted", 0, dates))
        val cancelled = booked.copy(bookings = booked.bookings.mapValues {
            it.value.copy(status = RESERVATION_DECLINED)
        })
        assertTrue(cancelled.canBook("second", "pine", 0, dates))
    }

    @Test
    fun ownerCanPublishTwoCampsitesAsOneLottery() {
        val soon = lotteryNow().toLocalDate().plusDays(2)
        val pine = campsite("pine", "Pine Camp", soon)
        val cedar = campsite("cedar", "Cedar Camp", soon)
        val repo = mockk<SharedLotteryRepository>()
        every {
            repo.save(null, "owner", listOf("pine", "cedar"), soon.atStartOfDay(),
                soon.plusDays(3).atTime(23, 59, 59), null, 5)
        } returns Tasks.forResult(true)
        val state = state(listOf(pine, cedar), repo).apply {
            userID = "owner"
            role = "owner"
        }
        compose.setContent { CampsiteOwnerMainScreen(rememberNavController(), state) }
        compose.onAllNodes(hasText("Menu"))[0].performClick()
        compose.onNodeWithText("Lottery").performClick()
        compose.onNodeWithTag("lotterySite_cedar").performClick()
        compose.onNodeWithText("Save lottery").performClick()
        compose.runOnIdle {
            verify(exactly = 1) {
                repo.save(null, "owner", listOf("pine", "cedar"), any(), any(), null, 5)
            }
        }
    }

    @Test
    fun winnerMustAgreeToSelectedCampsiteRulesBeforeBooking() {
        val date = lotteryNow().toLocalDate().plusDays(1)
        val pine = campsite("pine", "Pine Camp", date).apply {
            sharedLotteryID = "shared"
            setRules(listOf("Rules for Pine Camp"))
        }
        val cedar = campsite("cedar", "Cedar Camp", date).apply {
            sharedLotteryID = "shared"
            setRules(listOf("Rules for Cedar Camp"))
        }
        val repo = mockk<SharedLotteryRepository>()
        every {
            repo.confirm("shared", "first", "cedar", 0, listOf(date), listOf("Rules for Cedar Camp"))
        } returns Tasks.forResult(true)
        val state = state(listOf(pine, cedar), repo)
        compose.setContent { SharedLotteryReservationDialog(state, fixture(), "first") {} }
        compose.onNodeWithTag("sharedReservation_cedar_0").performClick()
        compose.onNodeWithTag("reserveDay_$date").performClick()
        compose.onNodeWithText("Confirm reservation").performClick()
        compose.onNodeWithText("Rules for Cedar Camp").assertExists()
        compose.onNodeWithText("Decline").performClick()
        compose.runOnIdle { verify(exactly = 0) { repo.confirm(any(), any(), any(), any(), any(), any()) } }
        compose.onNodeWithText("Confirm reservation").performClick()
        compose.onNodeWithText("Agree").performClick()
        compose.runOnIdle {
            verify(exactly = 1) {
                repo.confirm("shared", "first", "cedar", 0, listOf(date), listOf("Rules for Cedar Camp"))
            }
        }
    }

    private fun fixture() = SharedLottery(
        "shared", "owner", listOf("pine", "cedar"), opens, closes, null, 2,
        listOf("first", "second"), listOf("first", "second"), true, emptyMap()
    )

    private fun campsite(id: String, name: String, start: LocalDate) = Campsite(
        name, "owner", id, mutableListOf(4), 50f, coordinates = mutableListOf(),
        lotteryStartDate = start.atStartOfDay(), lotteryEndDate = start.plusDays(3).atTime(23, 59, 59),
        vacationStartTimes = mutableListOf(start.atStartOfDay() to start.plusDays(3).atTime(23, 59, 59))
    )

    private fun state(sites: List<Campsite>, repo: SharedLotteryRepository): AppState {
        val campsites = mockk<DatabaseCampsites>(relaxed = true)
        every { campsites.campsites } returns sites
        every { campsites.sharedLotteries } returns repo
        every { campsites.getCampsitesForOwner("owner", any()) } answers {
            secondArg<(List<Campsite>) -> Unit>()(sites)
        }
        val owners = mockk<DatabaseCampsiteOwners>(relaxed = true)
        every { owners.searchCampsiteOwner("owner", any()) } answers {
            secondArg<(CampsiteOwner?) -> Unit>()(null)
        }
        return AppState(
            admins = mockk(relaxed = true), owners = owners, campsites = campsites,
            entrants = mockk(relaxed = true), alerts = mockk(relaxed = true), chat = mockk(relaxed = true),
        )
    }
}

package com.example.code

import com.example.code.classes.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

/** # Owner Reservation View Tests
 * Tests for US 02.06.01 (booked dates), US 02.07.01 (active reservations),
 * and US 02.07.03 (cancelled reservations).
 *
 * Prototype was gained from a Conversation with Android Studio's agent.
 * Prompt: "Create tests for latest owner reservation decisions and booked date aggregation."
 */
class OwnerReservationViewsTest {
    @Test
    fun onlyLatestDecisionPerEntrantAndLocationIsUsed() {
        val messages = listOf(
            ChatMessage(entrantID = "e1", locationIndex = 0, type = RESERVATION_CONFIRMED, sentAt = 1, stayDates = listOf("2026-08-02")),
            ChatMessage(entrantID = "e1", locationIndex = 0, type = RESERVATION_DECLINED, sentAt = 2),
            ChatMessage(entrantID = "e2", locationIndex = 0, type = RESERVATION_CONFIRMED, sentAt = 3, stayDates = listOf("2026-08-01"))
        )
        val latest = ownerReservationDecisions(messages)
        assertEquals(setOf("e1" to RESERVATION_DECLINED, "e2" to RESERVATION_CONFIRMED),
            latest.map { it.entrantID to it.type }.toSet())
    }

    @Test
    fun bookedDatesAreUniqueAndSortedFromLatestConfirmedReservations() {
        val messages = listOf(
            ChatMessage(entrantID = "e1", locationIndex = 0, type = RESERVATION_CONFIRMED, sentAt = 1, stayDates = listOf("2026-08-02", "2026-08-01")),
            ChatMessage(entrantID = "e1", locationIndex = 0, type = RESERVATION_CONFIRMED, sentAt = 2, stayDates = listOf("2026-08-03")),
            ChatMessage(entrantID = "e2", locationIndex = 1, type = RESERVATION_CONFIRMED, sentAt = 3, stayDates = listOf("2026-08-02"))
        )
        assertEquals(listOf("2026-08-02", "2026-08-03"), ownerBookedDates(messages))
    }
}
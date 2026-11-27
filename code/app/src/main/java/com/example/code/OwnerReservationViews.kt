package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.code.classes.ChatMessage
import java.time.LocalDate

/** # Owner Reservation Views
 * Implementation of US 02.06.01, US 02.07.01, US 02.07.03, and US 02.08.01.
 * Prototype was gained from a Conversation with Android Studio's agent.
 * Prompt: "Add owner views for booked dates and active/cancelled reservations, with reservation rejection."
 */

/** Latest reservation decision per campsite, entrant, and location. */
internal fun ownerReservationDecisions(messages: List<ChatMessage>): List<ChatMessage> =
    messages.filter { it.locationIndex >= 0 && it.type in setOf(RESERVATION_CONFIRMED, RESERVATION_DECLINED) }
        .groupBy { Triple(it.campsiteID, it.entrantID, it.locationIndex) }
        .values.mapNotNull { decisions -> decisions.maxByOrNull { it.sentAt } }

/**
 * Lists the owner's latest confirmed reservations across their campsites. A reservation
 * is past when all its booked dates have ended. Today is still active.
 *
 * Codex prompt: "so for campsite owner, when list is clicked, I want there to be options
 * for past reservations and active reservations"
 */
@RequiresApi(Build.VERSION_CODES.O)
internal fun ownerReservationList(
    messages: List<ChatMessage>,
    ownerID: String,
    today: LocalDate,
    past: Boolean,
): List<ChatMessage> =
    ownerReservationDecisions(messages.filter { it.ownerID == ownerID })
        .filter { it.type == RESERVATION_CONFIRMED }
        .filter { reservationHasEnded(listOf(it), today) == past }
        .sortedByDescending { it.reservationDates().maxOrNull() }

/** Unique booked date strings from confirmed reservations. */
internal fun ownerBookedDates(messages: List<ChatMessage>): List<String> =
    ownerReservationDecisions(messages)
        .filter { it.type == RESERVATION_CONFIRMED }
        .flatMap { it.stayDates }
        .distinct()
        .sorted()

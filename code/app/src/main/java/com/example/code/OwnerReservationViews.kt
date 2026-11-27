package com.example.code

import com.example.code.classes.ChatMessage

/** # Owner Reservation Views
 * Implementation of US 02.06.01, US 02.07.01, US 02.07.03, and US 02.08.01.
 * Prototype was gained from a Conversation with Android Studio's agent.
 * Prompt: "Add owner views for booked dates and active/cancelled reservations, with reservation rejection."
 */

/** Latest reservation decision per entrant and campsite location. */
internal fun ownerReservationDecisions(messages: List<ChatMessage>): List<ChatMessage> =
    messages.filter { it.locationIndex >= 0 && it.type in setOf(RESERVATION_CONFIRMED, RESERVATION_DECLINED) }
        .groupBy { it.entrantID to it.locationIndex }
        .values.mapNotNull { decisions -> decisions.maxByOrNull { it.sentAt } }

/** Unique booked date strings from confirmed reservations. */
internal fun ownerBookedDates(messages: List<ChatMessage>): List<String> =
    ownerReservationDecisions(messages)
        .filter { it.type == RESERVATION_CONFIRMED }
        .flatMap { it.stayDates }
        .distinct()
        .sorted()

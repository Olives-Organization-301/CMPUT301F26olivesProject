package com.example.code.classes

import java.time.LocalDateTime

/** Reservations object that saves a User's Reservations.
 *  TODO: Make a database for this
 *  @param reservationID    Identify Reservation
 *  @param campsiteID       Corresponding Campsite
 *  @param entrantID        Corresponding Entrant
 *  @param reservationDates Key:   Integer as index of location,
 *                          Value: List of Dates they're subscribed to
 */
class Reservations(
    reservationID:      String,
    campsiteID:         String,
    entrantID:          String,
    reservationDates:   MutableMap<Int, MutableList<LocalDateTime>>
) {

}
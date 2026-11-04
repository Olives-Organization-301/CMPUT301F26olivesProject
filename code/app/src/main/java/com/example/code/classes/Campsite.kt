package com.example.code.classes

import java.time.LocalDateTime
import kotlin.random.Random

/**
 *
 */
class Campsite(
    private val name: String,
    private val owner: CampsiteOwner,
    private var maxOccupancy: Int,
    private var price: Float,
    private var rating: Float,
    private var capacity: String,
    private val campsiteID: String,
    private var coordinates: Coordinates,
    private var startDate: LocalDateTime,
    private var endDate: LocalDateTime,
) {
    private var description: String = ""

    // Listy-list
    var waitingList = mutableListOf<Entrant>()
    private var finalList = mutableListOf<Entrant>()
    private var rules = mutableListOf<String>()

    fun getCoordinates(): Coordinates = coordinates

    fun getName(): String = name

    fun addEntrant(entrant: Entrant) {
        waitingList.add(entrant)
    }

    fun getStartDate(): LocalDateTime = startDate
    fun getEndDate(): LocalDateTime = endDate

    fun generateFinalList() {
        var i = maxOccupancy
        while (i != 0) {
            val randomInd = Random.nextInt(0, waitingList.lastIndex)
            finalList.add(waitingList[randomInd])
            waitingList.removeAt(waitingList.lastIndex)
            i -= 1
        }
    }





}
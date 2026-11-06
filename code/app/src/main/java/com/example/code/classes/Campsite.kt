package com.example.code.classes

import java.time.LocalDateTime
import kotlin.random.Random

/** Representation of A Campsite class
 *
 */
class Campsite(
    private val name: String = "",
    private val ownerID: String = "",
    val campsiteID: String = "",
    private var maxOccupancy: Int = 1,
    private var price: Float = 0f,
    private var rating: Float = 0f,
    private var capacity: Int = 1,
    private var coordinates: Coordinates = Coordinates("", "", 0.0, 0.0),
    private var startDate: LocalDateTime = LocalDateTime.now(),
    private var endDate: LocalDateTime = LocalDateTime.now(),
) {
    private var description: String = ""
    private var waitingList = mutableListOf<String>()
    private var activeLotteryList = mutableListOf<String>()
    private var finalList = mutableListOf<String>() // Use Entrant IDs instead
    private var rules = mutableListOf<String>()

    fun getCoordinates(): Coordinates = coordinates
    fun getName(): String = name
    fun getMaxOccupancy(): Int = maxOccupancy
    fun getStartDate(): LocalDateTime = startDate
    fun getEndDate(): LocalDateTime = endDate
    fun getOwnerID(): String = ownerID
    fun getPrice(): Float = price
    fun getRating(): Float = rating
    fun getCapacity(): Int = capacity
    fun getDescription(): String = description
    fun getFinalList(): List<String> = finalList
    fun getRules(): List<String> = rules

    fun addEntrant(entrantID: String) {
        waitingList.add(entrantID)
    }

    fun addActiveLotteryEntrant(entrantID: String) {
        activeLotteryList.add(entrantID)
    }

    fun entrantInList(entrantID: String): Boolean {
        return entrantID in waitingList || entrantID in activeLotteryList
    }

    fun removeEntrant(entrantID: String) {
        if (entrantID in waitingList) {
            waitingList.remove(entrantID)
        }
        else if (entrantID in activeLotteryList) {
            activeLotteryList.remove(entrantID)
        }
    }

    fun generateFinalList() {
        var capacity = maxOccupancy

        //if more capacity than entrants, all entrants make the final list
        if(waitingList.size <= capacity){
            finalList = waitingList.toMutableList() // use .toMutableList() to create a copy instead of an alias
            waitingList.clear()
        } else {
            while (capacity != 0) {
                // use .size instead of .lastInt because the until value in kotlin ranges is exclusive
                val randomInd = Random.nextInt(0, waitingList.size)

                // add random entrant to final list, and remove from waitlist
                finalList.add(waitingList[randomInd])
                waitingList.removeAt(randomInd)
                capacity -= 1
            }
        }
    }
}
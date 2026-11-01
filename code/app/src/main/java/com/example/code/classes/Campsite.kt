package com.example.code.classes

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
    private var coordinates: Coordinates
) {
    private var description: String = ""

    // Listy-list
    var waitingList = mutableListOf<Entrant>()
    private var finalList = mutableListOf<Entrant>()
    private var rules = mutableListOf<String>()

    fun getCoords(): Coordinates = coordinates

    fun addEntrant(entrant: Entrant) {
        waitingList.add(entrant)
    }

    fun generateFinalList() {

    }





}
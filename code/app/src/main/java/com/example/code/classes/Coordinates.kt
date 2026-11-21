package com.example.code.classes

/**
 * The Geolocation of a Campsite
 * Used for Campsite
 * @param country: The country the campsite belongs to
 * @param city: The closest city to the campsite
 * @param region: The State, Province, Region the campsite belongs to
 * @param address: The Address of the Campsite
 */
class Coordinates(
    private val country: String,
    private val region: String,
    private val city: String,
    private val address: String,
) {
    // A Bunch of Getter Functions
    fun getCountry(): String = country
    fun getRegion(): String = region
    fun getAddress(): String = address
    fun getCity(): String = city

    // "Country, Province, City", skipping any parts that are blank
    fun getLocationText(): String =
        listOf(country, region, city).filter { it.isNotBlank() }.joinToString(", ")

    // Export coordinates as a Map
    fun toMap(): Map<String, Any?> = mapOf(
        "country" to country,
        "region" to region,
        "city" to city,
        "address" to address
    )

}
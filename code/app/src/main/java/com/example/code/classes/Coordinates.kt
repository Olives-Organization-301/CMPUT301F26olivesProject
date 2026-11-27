package com.example.code.classes

/**
 * The Geolocation of a Campsite
 * Used for Campsite
 * @param country: The country the campsite belongs to
 * @param city: The closest city to the campsite
 * @param region: The State, Province, Region the campsite belongs to
 * @param address: The Address of the Campsite
 *
 * Implementation of the map-location portion of US 02.01.02.
 * Prompt: "US 02.01.02 As a campsite owner, I want to provide campsite details including name,
 * location, description (amenities), maximum occupancy, map location, optional image, and tags."
 * Implemented with Android Studio AI Agent.
 */
class Coordinates(
    private val country: String,
    private val region: String,
    private val city: String,
    private val address: String,
    private val latitude: Double? = null,
    private val longitude: Double? = null,
) {
    // A Bunch of Getter Functions
    fun getCountry(): String = country
    fun getRegion(): String = region
    fun getAddress(): String = address
    fun getLatitude(): Double? = latitude
    fun getLongitude(): Double? = longitude
    fun getCity(): String = city

    // "Country, Province, City", skipping any parts that are blank
    fun getLocationText(): String =
        listOf(country, region, city).filter { it.isNotBlank() }.joinToString(", ")

    // Export coordinates as a Map
    fun toMap(): Map<String, Any?> = mapOf(
        "country" to country,
        "region" to region,
        "city" to city,
        "address" to address,
        "latitude" to latitude,
        "longitude" to longitude
    )

}
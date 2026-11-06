package com.example.code.classes

/** Handle the Coordinates of a Campsite.
 * Used for Campsite
 * @param country: The country
 * @param firstSubdivision: A Province, Region or State
 * TODO Add a City location as well
 * @param longitude: The Longitude based on Decimal Degrees
 * @param latitude: The Latitude based on Decimal Degrees
 */
class Coordinates(
    private val country: String,
    private val firstSubdivision: String,
    private val longitude: Double,
    private val latitude: Double,
) {

    //
    fun getCountry(): String = country
    fun getFirstSubdivision(): String = firstSubdivision
    fun getLongitude(): Double = longitude
    fun getLatitude(): Double = latitude

}
package com.example.code.classes

class Coordinates(
    private val country: String,
    private val firstSubdivision: String,
    private val longitude: Double,
    private val latitude: Double,
) {

    fun getCountry(): String = country
    fun getFirstSubdivision(): String = firstSubdivision
    fun getLongitude(): Double = longitude
    fun getLatitude(): Double = latitude

}
package com.example.code

import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Campsite
import com.example.code.classes.Coordinates
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.time.LocalDateTime
import kotlin.random.Random

class DatabaseCampsites {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("campsites")
    private val _campsites = mutableStateListOf<Campsite>()

    val campsites: List<Campsite> get() = _campsites

    // Generate a unique numeric string ID between 1 and 1,000,000 without repetition
    fun generateUniqueCampsiteID(onResult: (String) -> Unit) {
        val randomId = Random.nextInt(1, 1000000).toString()
        campsitesRef.document(randomId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // ID already exists, recursively generate a new one
                    generateUniqueCampsiteID(onResult)
                } else {
                    onResult(randomId)
                }
            }
            .addOnFailureListener {
                onResult(randomId)
            }
    }

    fun addCampsite(campsite: Campsite) {
        try {
            if (!_campsites.any { it.campsiteID == campsite.campsiteID }) {
                _campsites.add(campsite)
            }
            campsitesRef.document(campsite.campsiteID).set(campsite.toMap())
        } catch (e: Exception) {
            // Catch exception
        }
    }

    fun updateCampsiteName(campsite: Campsite, newName: String) {
        try {
            val index = _campsites.indexOfFirst { it.campsiteID == campsite.campsiteID }
            if (index != -1) {
                _campsites[index] = campsite
            }
            campsitesRef.document(campsite.campsiteID).update("name", newName)
        } catch (e: Exception) {
            // Catch exception
        }
    }

    fun updateCampsiteRating(campsite: Campsite, newRating: Float) {
        try {
            val index = _campsites.indexOfFirst { it.campsiteID == campsite.campsiteID }
            if (index != -1) {
                campsite.setRating(newRating)
                _campsites[index] = campsite
            }
            campsitesRef.document(campsite.campsiteID).update("rating", newRating)
        } catch (e: Exception) {
            // Catch exception
        }
    }

    fun deleteCampsite(campsite: Campsite) {
        try {
            _campsites.removeAll { it.campsiteID == campsite.campsiteID }
            campsitesRef.document(campsite.campsiteID).delete()
        } catch (e: Exception) {
            // Catch exception
        }
    }

    fun searchCampsite(campsiteID: String, onResult: (Campsite?) -> Unit) {
        campsitesRef.document(campsiteID).get()
            .addOnSuccessListener { doc ->
                try {
                    if (doc.exists()) {
                        val name = doc.getString("name") ?: "Campsite"
                        val ownerID = doc.getString("ownerID") ?: doc.getString("OwnerID") ?: ""
                        val maxOcc = doc.getLong("maxOccupancy")?.toInt() ?: 4
                        val price = doc.getDouble("price")?.toFloat() ?: 50.0f
                        val rating = if (doc.contains("rating") && doc.get("rating") != null) doc.getDouble("rating")?.toFloat() else null
                        val capacity = doc.getLong("capacity")?.toInt() ?: 4
                        val coordMap = doc.get("coordinates") as? Map<*, *>
                        val country = coordMap?.get("country") as? String ?: "Canada"
                        val sub = coordMap?.get("firstSubdivision") as? String ?: "Location"
                        val lon = (coordMap?.get("longitude") as? Number)?.toDouble() ?: -113.49
                        val lat = (coordMap?.get("latitude") as? Number)?.toDouble() ?: 53.54
                        val coords = Coordinates(country, sub, lon, lat)

                        val start = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDateTime.now() else null
                        val end = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDateTime.now().plusDays(7) else null

                        val campsite = Campsite(
                            name = name,
                            ownerID = ownerID,
                            campsiteID = doc.id,
                            maxOccupancy = maxOcc,
                            price = price,
                            rating = rating,
                            capacity = capacity,
                            coordinates = coords,
                            startDate = start,
                            endDate = end,
                        )
                        onResult(campsite)
                    } else {
                        onResult(null)
                    }
                } catch (e: Exception) {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // Check ownerID matching supporting both String and Int types in Firestore
    fun getCampsitesForOwner(ownerID: String, onResult: (List<Campsite>) -> Unit) {
        campsitesRef.get()
            .addOnSuccessListener { snapshot ->
                val targetStr = ownerID.trim()
                val targetInt = ownerID.trim().toIntOrNull()

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val docOwnerObj = doc.get("ownerID") ?: doc.get("OwnerID")
                        val docOwnerStr = docOwnerObj?.toString()?.trim() ?: ""
                        val docOwnerInt = docOwnerObj?.let {
                            when (it) {
                                is Number -> it.toInt()
                                is String -> it.toIntOrNull()
                                else -> null
                            }
                        }

                        val matches = docOwnerStr == targetStr || (targetInt != null && docOwnerInt == targetInt) || docOwnerStr.isBlank()

                        if (matches) {
                            val name = doc.getString("name") ?: "Campsite"
                            val maxOcc = doc.getLong("maxOccupancy")?.toInt() ?: 4
                            val price = doc.getDouble("price")?.toFloat() ?: 50.0f
                            val rating = if (doc.contains("rating") && doc.get("rating") != null) doc.getDouble("rating")?.toFloat() else null
                            val capacity = doc.getLong("capacity")?.toInt() ?: 4
                            val coordMap = doc.get("coordinates") as? Map<*, *>
                            val country = coordMap?.get("country") as? String ?: "Canada"
                            val sub = coordMap?.get("firstSubdivision") as? String ?: "Location"
                            val lon = (coordMap?.get("longitude") as? Number)?.toDouble() ?: -113.49
                            val lat = (coordMap?.get("latitude") as? Number)?.toDouble() ?: 53.54
                            val coords = Coordinates(country, sub, lon, lat)

                            val start = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDateTime.now() else null
                            val end = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDateTime.now().plusDays(7) else null

                            Campsite(
                                name = name,
                                ownerID = targetStr,
                                campsiteID = doc.id,
                                maxOccupancy = maxOcc,
                                price = price,
                                rating = rating,
                                capacity = capacity,
                                coordinates = coords,
                                startDate = start,
                                endDate = end,
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}

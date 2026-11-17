package com.example.code

import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Campsite
import com.example.code.classes.Coordinates
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import java.time.LocalDateTime
import kotlin.random.Random

/** Database of Campsites
 * represents every Campsite cached by the system, regardless of owner
 * Since every campsite class has its owner, use
 *
 *
 * one of the prompts used : now when User Database is clicked, I want both collections "Entrants" and "
 * CampsiteOwners" displayed as how the logic for displaying collection "campsites" were in Admin
 */
class DatabaseCampsites {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("campsites")
    private val _campsites = mutableStateListOf<Campsite>()

    val campsites: List<Campsite> get() = _campsites

    init {
        // Direct real-time listener to sync all campsites from Firestore "campsites" collection
        campsitesRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                android.util.Log.e("DB", "campsites listener failed", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener

            val list = snapshots.documents.mapNotNull { doc ->
                parseCampsiteDocument(doc)
            }
            _campsites.clear()
            _campsites.addAll(list)
            android.util.Log.d("DB", "campsites synchronized: ${_campsites.size}")
        }
    }

    private fun parseCampsiteDocument(doc: DocumentSnapshot): Campsite? {
        return try {
            if (!doc.exists()) return null

            val name = doc.getString("name")
                ?: doc.getString("Name")
                ?: doc.getString("campsiteName")
                ?: doc.id

            val ownerID = doc.get("ownerID")?.toString()
                ?: doc.get("OwnerID")?.toString()
                ?: doc.get("ownerId")?.toString()
                ?: doc.get("owner_id")?.toString()
                ?: ""

            val maxOcc = (doc.get("maxOccupancy") as? Number)?.toInt()
                ?: doc.get("maxOccupancy")?.toString()?.toIntOrNull()
                ?: 4

            val priceObj = doc.get("price") ?: doc.get("Price")
            val price = (priceObj as? Number)?.toFloat()
                ?: priceObj?.toString()?.toFloatOrNull()
                ?: 50.0f

            val ratingObj = doc.get("rating") ?: doc.get("Rating")
            val rating = (ratingObj as? Number)?.toFloat()
                ?: ratingObj?.toString()?.toFloatOrNull()

            val capObj = doc.get("capacity") ?: doc.get("Capacity")
            val capacity = (capObj as? Number)?.toInt()
                ?: capObj?.toString()?.toIntOrNull()
                ?: 4

            val coordMap = doc.get("coordinates") as? Map<*, *>
            val country = coordMap?.get("country")?.toString()
                ?: doc.getString("country")
                ?: "Canada"

            val sub = coordMap?.get("firstSubdivision")?.toString()
                ?: doc.getString("location")
                ?: doc.getString("firstSubdivision")
                ?: doc.getString("city")
                ?: ""

            val lon = (coordMap?.get("longitude") as? Number)?.toDouble()
                ?: doc.get("longitude")?.toString()?.toDoubleOrNull()
                ?: -113.49

            val lat = (coordMap?.get("latitude") as? Number)?.toDouble()
                ?: doc.get("latitude")?.toString()?.toDoubleOrNull()
                ?: 53.54

            val coords = Coordinates(country, sub, lon, lat)

            val start = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDateTime.now() else null
            val end = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDateTime.now().plusDays(7) else null

            Campsite(
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
        } catch (e: Exception) {
            android.util.Log.e("DB", "Failed to parse document ${doc.id}", e)
            null
        }
    }

    // Fetch all campsites from the firebase collection "campsites"
    fun fetchAllCampsites(onResult: (List<Campsite>) -> Unit) {
        campsitesRef.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    parseCampsiteDocument(doc)
                }
                _campsites.clear()
                _campsites.addAll(list)
                android.util.Log.d("DB", "fetchAllCampsites loaded ${list.size} campsites")
                onResult(list)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "fetchAllCampsites failed", e)
                onResult(emptyList())
            }
    }

    // Generate a unique numeric string ID between 1 and 1,000,000 without repetition
    fun generateUniqueCampsiteID(onResult: (String) -> Unit) {
        val randomId = Random.nextInt(1, 1000000).toString()
        campsitesRef.document(randomId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
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
                onResult(parseCampsiteDocument(doc))
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
                        val docOwnerObj = doc.get("ownerID") ?: doc.get("OwnerID") ?: doc.get("ownerId")
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
                            parseCampsiteDocument(doc)
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

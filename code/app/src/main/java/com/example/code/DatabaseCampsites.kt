package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Campsite
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class DatabaseCampsites {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("Campsites")
    private val _campsites = mutableStateListOf<Campsite>()

    val campsites: List<Campsite> get() = _campsites

    fun addCampsite(campsite: Campsite) {
        campsitesRef.document(campsite.campsiteID).set(campsite)
    }

    fun updateCampsiteName(campsite: Campsite, newName: String) {
        campsitesRef.document(campsite.campsiteID).update("name", newName)
    }

    fun deleteCampsite(campsite: Campsite) {
        campsitesRef.document(campsite.campsiteID).delete()
    }

    fun searchCampsite(campsiteID: String, onResult: (Campsite?) -> Unit) {
        campsitesRef.document(campsiteID).get()
            .addOnSuccessListener { doc ->
                // toObject returns null if the document doesn't exist
                onResult(doc.toObject(Campsite::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}
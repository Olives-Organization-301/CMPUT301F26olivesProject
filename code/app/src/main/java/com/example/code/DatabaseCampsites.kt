package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Campsite
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class DatabaseCampsites(ownerId: String) {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("CampsiteOwners")
        .document(ownerId)
        .collection("campsites")
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
}
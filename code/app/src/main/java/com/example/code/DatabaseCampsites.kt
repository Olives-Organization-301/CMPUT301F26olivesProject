package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Campsite
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class DatabaseCampsites() {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("campsites")
    private val _campsites = mutableStateListOf<Campsite>()

    val campsites: List<Campsite> get() = _campsites

    fun addCampsite(campsite: Campsite) {
        _campsites.add(campsite)
        campsitesRef.document(campsite.campsiteID).set(campsite)
    }

    fun updateCampsiteName(campsite: Campsite, newName: String) {
        val index = _campsites.indexOfFirst { it.campsiteID == campsite.campsiteID }
        if (index != -1) {
            _campsites[index] = campsite
        }
        campsitesRef.document(campsite.campsiteID).update("name", newName)
    }

    fun deleteCampsite(campsite: Campsite) {
        _campsites.removeAll { it.campsiteID == campsite.campsiteID }
        campsitesRef.document(campsite.campsiteID).delete()
    }

    fun searchCampsite(campsiteID: String, onResult: (Campsite?) -> Unit) {
        campsitesRef.document(campsiteID).get()
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(Campsite::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    init {
        campsitesRef.addSnapshotListener { snapshots, error ->
            if(error != null){
                android.util.Log.e("DB", "campsite listener failed", error)
                return@addSnapshotListener
            }
            if(snapshots == null) return@addSnapshotListener
            _campsites.clear()
            _campsites.addAll(snapshots.toObjects(Campsite::class.java))
            android.util.Log.d("DB", "Campsites loaded: ${_campsites.size}")
        }
    }
}

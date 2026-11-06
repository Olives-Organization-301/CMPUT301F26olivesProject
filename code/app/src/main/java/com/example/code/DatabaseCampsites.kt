package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Campsite
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/** Database of Campsites
 * represents every Campsite cached by the system, regardless of owner
 * Since every campsite class has its owner, use
 */
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

    fun getCampsitesForOwner(ownerID: String, onResult: (List<Campsite>) -> Unit) {
        campsitesRef.whereEqualTo("ownerID", ownerID).get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Campsite::class.java)
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}

package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Campsite
import com.example.code.classes.Entrant
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

class DatabaseEntrants() {
    private val db = Firebase.firestore
    private val entrantsRef = db.collection("Entrants")
    private val _entrants = mutableStateListOf<Entrant>()

    val entrants: List<Entrant> get() = _entrants

    fun addEntrant(entrant: Entrant) {
        entrantsRef.document(entrant.getUserID()).set(entrant)
    }

    fun updateEntrantUsername(entrant: Entrant, newUsername: String) {
        entrant.username = newUsername
        entrantsRef.document(entrant.getUserID()).set(entrant)
    }

    fun deleteEntrant(entrant: Entrant) {
        entrantsRef.document(entrant.getUserID()).delete()
    }

    fun searchEntrant(campsiteID: String, onResult: (Entrant?) -> Unit) {
        entrantsRef.document(campsiteID).get()
            .addOnSuccessListener { doc ->
                // toObject returns null if the document doesn't exist
                onResult(doc.toObject(Entrant::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun updateEntrant(entrant: Entrant) {
        entrantsRef.document(entrant.getUserID()).set(entrant)
    }

    fun removeEntrantCampsite(entrant: Entrant, listKey: String, campsiteID: String) {
        entrant.removeCampsite(listKey, campsiteID)   // keep the in-memory copy in sync
        entrantsRef.document(entrant.getUserID())
            .update(FieldPath.of("campsiteLists", listKey), FieldValue.arrayRemove(campsiteID))
    }
}
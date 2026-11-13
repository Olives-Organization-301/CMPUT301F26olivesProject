package com.example.code

import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Entrant
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

/** Database of Entrants, representing the Entrants' screens
 * one of the prompts used : now when User Database is clicked, I want both collections "Entrants" and "
 *  * CampsiteOwners" displayed as how the logic for displaying collection "campsites" were in Admin
 */
class DatabaseEntrants {
    private val db = Firebase.firestore
    private val entrantsRef = db.collection("Entrants")
    private val _entrants = mutableStateListOf<Entrant>()

    val entrants: List<Entrant> get() = _entrants

    init {
        entrantsRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                android.util.Log.e("DB", "entrants listener failed", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener
            val list = snapshots.documents.map { doc ->
                Entrant(
                    username = doc.getString("username") ?: doc.getString("name") ?: doc.id,
                    email = doc.getString("email") ?: "",
                    userID = doc.id
                )
            }
            _entrants.clear()
            _entrants.addAll(list)
            android.util.Log.d("DB", "Entrants loaded: ${_entrants.size}")
        }
    }

    fun fetchAllEntrants(onResult: (List<Entrant>) -> Unit) {
        entrantsRef.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    Entrant(
                        username = doc.getString("username") ?: doc.getString("name") ?: doc.id,
                        email = doc.getString("email") ?: "",
                        userID = doc.id
                    )
                }
                _entrants.clear()
                _entrants.addAll(list)
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun addEntrant(entrant: Entrant) {
        if (!_entrants.any { it.getUserID() == entrant.getUserID() }) {
            _entrants.add(entrant)
        }
        entrantsRef.document(entrant.getUserID()).set(entrant)
    }

    fun updateEntrantUsername(entrant: Entrant, newUsername: String) {
        entrant.username = newUsername
        val index = _entrants.indexOfFirst { it.getUserID() == entrant.getUserID() }
        if (index != -1) {
            _entrants[index] = entrant
        }
        entrantsRef.document(entrant.getUserID()).set(entrant)
    }

    fun deleteEntrant(entrant: Entrant) {
        _entrants.removeAll { it.getUserID() == entrant.getUserID() }
        entrantsRef.document(entrant.getUserID()).delete()
    }

    fun searchEntrants(entrantID: String, onResult: (Entrant?) -> Unit) {
        entrantsRef.document(entrantID).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val entrant = Entrant(
                        username = doc.getString("username") ?: doc.getString("name") ?: doc.id,
                        email = doc.getString("email") ?: "",
                        userID = doc.id
                    )
                    onResult(entrant)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun searchEntrantsByEmail(email: String, onResult: (Entrant?) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        entrantsRef.get()
            .addOnSuccessListener { snapshot ->
                val matchingDoc = snapshot.documents.find { doc ->
                    val docEmail = doc.getString("email") ?: doc.getString("Email") ?: ""
                    docEmail.trim().lowercase() == cleanEmail
                }

                if (matchingDoc != null) {
                    val entrant = Entrant(
                        username = matchingDoc.getString("username") ?: matchingDoc.getString("name") ?: cleanEmail.substringBefore("@"),
                        email = cleanEmail,
                        userID = matchingDoc.id
                    )
                    onResult(entrant)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun updateEntrant(entrant: Entrant) {
        entrantsRef.document(entrant.getUserID()).set(entrant)
    }

    fun removeEntrantCampsite(entrant: Entrant, listKey: String, campsiteID: String) {
        entrant.removeCampsite(listKey, campsiteID)
        entrantsRef.document(entrant.getUserID())
            .update(FieldPath.of("campsiteLists", listKey), FieldValue.arrayRemove(campsiteID))
    }

    init {
        entrantsRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                android.util.Log.e("DB", "entrants listener failed", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener
            _entrants.clear()
            _entrants.addAll(snapshots.documents.map { doc ->
                Entrant(
                    doc.getString("username") ?: "",
                    doc.getString("email") ?: "",
                    doc.getString("userID") ?: doc.id
                )
            })
            android.util.Log.d("DB", "Entrants loaded: ${_entrants.size}")
        }
    }
}

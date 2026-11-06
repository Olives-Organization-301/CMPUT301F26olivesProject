package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Entrant
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentId
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
}
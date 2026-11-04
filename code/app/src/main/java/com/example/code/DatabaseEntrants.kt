package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.example.code.classes.Entrant


class DatabaseEntrants {
    private val db = Firebase.firestore
    private val entrantsRef = db.collection("entrants")
    private val _entrants = mutableStateListOf<Entrant>()

    val entrants: List<Entrant> get() = _entrants

    fun addEntrant(entrant: Entrant) {
        entrantsRef.document(entrant.getUserID()).set(entrant)
    }

    fun updateEntrantUsername(entrant: Entrant, newUsername: String) {
        entrantsRef.document(entrant.username).set(newUsername)
    }

    fun deleteEntrant(entrant: Entrant) {
        entrantsRef.document(entrant.getUserID()).delete()
    }

}
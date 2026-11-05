package com.example.code
import com.google.firebase.firestore.DocumentId

/**
 * Repository for the "Entrants" Firestore collection.
 *
 * To use it, create an instance and call add, update, delete
 * The Entrant model is stored in each document. The id field is filled
 * automatically with the document ID.
 */

// Class was built with the help of Claude

class DatabaseEntrants : FirestoreRepository<DatabaseEntrants.Entrant>("Entrants", DatabaseEntrants.Entrant::class.java) {

    data class Entrant(
        @DocumentId val id: String = "",
        val name: String = "",
        val email: String = "",
        val password: String = ""
    )
}
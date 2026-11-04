package com.example.code
import com.google.firebase.firestore.DocumentId

/**
 * Repository for the "Campsites" Firestore collection.
 *
 * To use it, create an instance and call add, update, delete
 * The Campsite model is stored in each document. The id field is filled
 * automatically with the document ID.
 */

// Class was built with the help of Claude

class DatabaseCampsites : FirestoreRepository<DatabaseCampsites.Campsite>("Campsites", DatabaseCampsites.Campsite::class.java) {

    data class Campsite(
        @DocumentId var id: String = "",
        var name: String = "",
        var location: String = "",
        var ownerId: String = "",
        var pricePerNight: Double = 0.0
    )
}
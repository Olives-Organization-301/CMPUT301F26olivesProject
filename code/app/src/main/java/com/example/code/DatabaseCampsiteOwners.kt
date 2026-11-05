package com.example.code
import com.google.firebase.firestore.DocumentId

/**
 * Repository for the "CampsiteOwners" Firestore collection.
 *
 * To use it, create an instance and call add, update, delete
 * The CampsiteOwner model is stored in each document. The id field is filled
 * automatically with the document ID.
 */
// Class was built with the help of Claude
class DatabaseCampsiteOwners : FirestoreRepository<DatabaseCampsiteOwners.CampsiteOwner>("CampsiteOwners", DatabaseCampsiteOwners.CampsiteOwner::class.java) {

    data class CampsiteOwner(
        @DocumentId val id: String = "",
        val name: String = "",
        val email: String = "",
        val password: String = "",
        val phone: String = ""
    )
}
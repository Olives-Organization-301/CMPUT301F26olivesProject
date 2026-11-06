package com.example.code
import com.google.firebase.firestore.DocumentId

/**
 * Repository for the "Admins" Firestore collection.
 *
 * To use it, create an instance and call add, update, delete
 * The Admin model is stored in each document. The id field is filled
 * automatically with the document ID.
 */
// Class was built with the help of Claude : https://claude.ai/chat/6b55aa79-e6e7-49f3-bdb6-21e1a4a4dd95

class DatabaseAdmins : FirestoreRepository<DatabaseAdmins.Admin>("Admins", DatabaseAdmins.Admin::class.java) {

    data class Admin(
        @DocumentId val id: String = "",
        val name: String = "",
        val email: String = "",
        val password: String = ""
    )
}
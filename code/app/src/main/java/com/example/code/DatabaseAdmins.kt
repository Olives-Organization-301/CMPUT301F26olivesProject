package com.example.code

import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Administrator
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/** A main database representing every Admin the system has
 * Used it to access, edit and set admins
 * added search function which check emails for log in (change this later to make it more secure)
 */
class DatabaseAdmins {
    private val db = Firebase.firestore
    private val adminsRef = db.collection("Admin")
    private val _admins = mutableStateListOf<Administrator>()

    val admins: List<Administrator> get() = _admins

    fun addAdmin(admin: Administrator) {
        if (!_admins.any { it.getUserID() == admin.getUserID() }) {
            _admins.add(admin)
        }
        adminsRef.document(admin.getUserID()).set(admin)
    }

    fun updateAdminUsername(admin: Administrator, newUsername: String) {
        admin.username = newUsername
        val index = _admins.indexOfFirst { it.getUserID() == admin.getUserID() }
        if (index != -1) {
            _admins[index] = admin
        }
        adminsRef.document(admin.getUserID()).set(admin)
    }

    fun deleteAdmin(admin: Administrator) {
        _admins.removeAll { it.getUserID() == admin.getUserID() }
        adminsRef.document(admin.getUserID()).delete()
    }

    fun searchAdmin(adminID: String, onResult: (Administrator?) -> Unit) {
        adminsRef.document(adminID).get()
            .addOnSuccessListener { doc ->
                val admin = doc.toObject(Administrator::class.java)
                if (admin != null) {
                    admin.setUserID(doc.id)
                }
                onResult(admin)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun searchAdminByEmail(email: String, onResult: (Administrator?) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        adminsRef.get()
            .addOnSuccessListener { snapshot ->
                val matchingDoc = snapshot.documents.find { doc ->
                    val docEmail = doc.getString("email") ?: doc.getString("Email") ?: ""
                    docEmail.trim().lowercase() == cleanEmail
                }

                if (matchingDoc != null) {
                    val admin = matchingDoc.toObject(Administrator::class.java)
                    if (admin != null) {
                        admin.setUserID(matchingDoc.id)
                    } else {
                        val fallback = Administrator(
                            username = matchingDoc.getString("username") ?: cleanEmail.substringBefore("@"),
                            email = cleanEmail,
                            userID = matchingDoc.id
                        )
                        onResult(fallback)
                        return@addOnSuccessListener
                    }
                    onResult(admin)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}

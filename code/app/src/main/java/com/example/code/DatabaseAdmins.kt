package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Administrator
import com.example.code.classes.Campsite
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/** A main database representing every Admin the system has
 * Used it to access, edit and set admins
 */
class DatabaseAdmins {
    private val db = Firebase.firestore
    private val adminsRef = db.collection("Admin")
    private val _admins = mutableStateListOf<Administrator>()

    val admins: List<Administrator> get() = _admins

    fun addAdmin(admin: Administrator) {
        adminsRef.document(admin.getUserID()).set(admin)
    }

    fun updateAdminUsername(admin: Administrator, newUsername: String) {
        admin.username = newUsername
        adminsRef.document(admin.getUserID()).set(admin)
    }

    fun deleteAdmin(admin: Administrator) {
        adminsRef.document(admin.getUserID()).delete()
    }

    fun searchAdmin(adminID: String, onResult: (Administrator?) -> Unit) {
        adminsRef.document(adminID).get()
            .addOnSuccessListener { doc ->
                // toObject returns null if the document doesn't exist
                onResult(doc.toObject(Administrator::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}
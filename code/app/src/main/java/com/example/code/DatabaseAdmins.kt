package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Administrator
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class DatabaseAdmins {
    private val db = Firebase.firestore
    private val adminsRef = db.collection("Admin")
    private val _admins = mutableStateListOf<Administrator>()

    val admins : List<Administrator> get() = _admins

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
}
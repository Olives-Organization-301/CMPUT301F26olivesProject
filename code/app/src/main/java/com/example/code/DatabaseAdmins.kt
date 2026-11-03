package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.example.code.classes.Administrator

class DatabaseAdmins {
    private val db = Firebase.firestore
    private val entrantsRef = db.collection("administrators")
    private val _campsites = mutableListOf<Administrator>()
}
package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.example.code.classes.CampsiteOwner

class DatabaseCampsiteOwners {
    private val db = Firebase.firestore
    private val entrantsRef = db.collection("campsiteOwners")
    private val _campsites = mutableListOf<CampsiteOwner>()
}
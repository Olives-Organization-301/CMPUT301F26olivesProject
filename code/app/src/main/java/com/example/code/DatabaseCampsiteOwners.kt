package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.CampsiteOwner
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class DatabaseCampsiteOwners {
    private val db = Firebase.firestore
    private val campsiteOwnersRef = db.collection("CampsiteOwners")
    private val _campsiteOwners = mutableStateListOf<CampsiteOwner>()

    val entrants: List<CampsiteOwner> get() = _campsiteOwners

    fun addCampsiteOwner(campsiteOwner : CampsiteOwner) {
        campsiteOwnersRef.document(campsiteOwner .getUserID()).set(campsiteOwner )
    }

    fun updateCampsiteOwnerUsername(campsiteOwner : CampsiteOwner, newUsername: String) {
        campsiteOwnersRef.document(campsiteOwner.getUserID()).update("username", newUsername)
    }

    fun deleteCampsiteOwner(campsiteOwner : CampsiteOwner) {
        campsiteOwnersRef.document(campsiteOwner .getUserID()).delete()
    }

    fun searchCampsiteOwner(ownerID: String, onResult: (CampsiteOwner?) -> Unit) {
        campsiteOwnersRef.document(ownerID).get()
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(CampsiteOwner::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}
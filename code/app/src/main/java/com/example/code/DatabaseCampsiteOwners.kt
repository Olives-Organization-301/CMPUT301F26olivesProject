package com.example.code

import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.CampsiteOwner
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class DatabaseCampsiteOwners {
    private val db = Firebase.firestore
    private val campsiteOwnersRef = db.collection("CampsiteOwners")
    private val _campsiteOwners = mutableStateListOf<CampsiteOwner>()

    val campsiteOwners: List<CampsiteOwner> get() = _campsiteOwners

    fun addCampsiteOwner(campsiteOwner: CampsiteOwner) {
        if (!_campsiteOwners.any { it.getUserID() == campsiteOwner.getUserID() }) {
            _campsiteOwners.add(campsiteOwner)
        }
        campsiteOwnersRef.document(campsiteOwner.getUserID()).set(campsiteOwner)
    }

    fun updateCampsiteOwnerUsername(campsiteOwner: CampsiteOwner, newUsername: String) {
        campsiteOwner.username = newUsername
        val index = _campsiteOwners.indexOfFirst { it.getUserID() == campsiteOwner.getUserID() }
        if (index != -1) {
            _campsiteOwners[index] = campsiteOwner
        }
        campsiteOwnersRef.document(campsiteOwner.getUserID()).set(campsiteOwner)
    }

    fun deleteCampsiteOwner(campsiteOwner: CampsiteOwner) {
        _campsiteOwners.removeAll { it.getUserID() == campsiteOwner.getUserID() }
        campsiteOwnersRef.document(campsiteOwner.getUserID()).delete()
    }

    fun searchCampsiteOwner(ownerID: String, onResult: (CampsiteOwner?) -> Unit) {
        campsiteOwnersRef.document(ownerID).get()
            .addOnSuccessListener { doc ->
                val owner = doc.toObject(CampsiteOwner::class.java)
                if (owner != null) {
                    owner.setUserID(doc.id)
                }
                onResult(owner)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun searchCampsiteOwnerByEmail(email: String, onResult: (CampsiteOwner?) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        campsiteOwnersRef.get()
            .addOnSuccessListener { snapshot ->
                val matchingDoc = snapshot.documents.find { doc ->
                    val docEmail = doc.getString("email") ?: doc.getString("Email") ?: ""
                    docEmail.trim().lowercase() == cleanEmail
                }

                if (matchingDoc != null) {
                    val owner = matchingDoc.toObject(CampsiteOwner::class.java)
                    if (owner != null) {
                        owner.setUserID(matchingDoc.id)
                    } else {
                        val fallback = CampsiteOwner(
                            username = matchingDoc.getString("username") ?: cleanEmail.substringBefore("@"),
                            email = cleanEmail,
                            userID = matchingDoc.id
                        )
                        onResult(fallback)
                        return@addOnSuccessListener
                    }
                    onResult(owner)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}

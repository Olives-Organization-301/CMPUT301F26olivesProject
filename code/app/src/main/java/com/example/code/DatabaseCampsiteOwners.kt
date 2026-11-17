package com.example.code

import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.CampsiteOwner
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/** Database used to let campsite owners persist in the database
 */
class DatabaseCampsiteOwners {
    private val db = Firebase.firestore
    private val campsiteOwnersRef = db.collection("CampsiteOwners")
    private val _campsiteOwners = mutableStateListOf<CampsiteOwner>()

    val campsiteOwners: List<CampsiteOwner> get() = _campsiteOwners

    init {
        campsiteOwnersRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                android.util.Log.e("DB", "owners listener failed", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener
            val list = snapshots.documents.map { doc ->
                CampsiteOwner(
                    username = doc.getString("username") ?: doc.getString("name") ?: doc.id,
                    email = doc.getString("email") ?: "",
                    userID = doc.id
                )
            }
            _campsiteOwners.clear()
            _campsiteOwners.addAll(list)
            android.util.Log.d("DB", "Owners loaded: ${_campsiteOwners.size}")
        }
    }

    fun fetchAllOwners(onResult: (List<CampsiteOwner>) -> Unit) {
        campsiteOwnersRef.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    CampsiteOwner(
                        username = doc.getString("username") ?: doc.getString("name") ?: doc.id,
                        email = doc.getString("email") ?: "",
                        userID = doc.id
                    )
                }
                _campsiteOwners.clear()
                _campsiteOwners.addAll(list)
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

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

    // Search for campsite owner by document ID
    fun searchCampsiteOwner(ownerID: String, onResult: (CampsiteOwner?) -> Unit) {
        campsiteOwnersRef.document(ownerID).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val owner = doc.toObject(CampsiteOwner::class.java)
                    if (owner != null) {
                        owner.setUserID(doc.id)
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

package com.example.code
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.CampsiteOwner
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/** Database used to let campsite owners persist in the database
 *
 */
class DatabaseCampsiteOwners {
    private val db = Firebase.firestore
    private val campsiteOwnersRef = db.collection("CampsiteOwners")
    private val _campsiteOwners = mutableStateListOf<CampsiteOwner>()

    val campsiteOwners: List<CampsiteOwner> get() = _campsiteOwners

    fun addCampsiteOwner(campsiteOwner : CampsiteOwner) {
        campsiteOwnersRef.document(campsiteOwner .getUserID()).set(campsiteOwner )
    }

    // Update Campsite Owner
    fun updateCampsiteOwnerUsername(campsiteOwner : CampsiteOwner, newUsername: String) {
        campsiteOwner.username = newUsername
        campsiteOwnersRef.document(campsiteOwner.getUserID()).set(campsiteOwner)
    }

    // Delete Campsite Owner
    fun deleteCampsiteOwner(campsiteOwner : CampsiteOwner) {
        campsiteOwnersRef.document(campsiteOwner .getUserID()).delete()
    }

    // Search for campsite Owners
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
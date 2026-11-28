package com.example.code

import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Alert
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.example.code.classes.Entrant
import com.example.code.classes.CampsiteOwner


class DatabaseAlerts() {
    private val db = Firebase.firestore
    private val alertsRef = db.collection("alerts")
    private val _alerts = mutableStateListOf<Alert>()
    val alerts: List<Alert> get() = _alerts

    init {
        // Real-time listener syncing every alert from Firestore
        alertsRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                android.util.Log.e("DB", "alerts listener failed", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener
            val list = snapshots.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Alert::class.java)
                } catch (e: Exception) {
                    android.util.Log.e("DB", "could not read alert ${doc.id}", e)
                    null
                }
            }
            _alerts.clear()
            _alerts.addAll(list)
        }
    }

    fun addAlert(alert: Alert){
        _alerts.add(alert)
        alertsRef.document(alert.alertID).set(alert)
    }

    fun deleteAlert(alert: Alert){
        _alerts.removeAll { it.alertID == alert.alertID }
        alertsRef.document(alert.alertID).delete()
    }

    /** # Owner Sent Notifications
     * Implementation of US 02.10.02: campsite owner views notifications they sent.
     * Prototype was gained from a Conversation with Android Studio's agent.
     * Prompt: "Show a campsite owner all alerts sent from their account."
     */
    fun fetchSentBy(ownerName: String, onResult: (List<Alert>) -> Unit) {
        alertsRef.whereEqualTo("sender", ownerName).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.documents.mapNotNull { it.toObject(Alert::class.java) })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun searchAlert(alertId: String, onResult: (Alert?) -> Unit) {
        alertsRef.document(alertId).get()
            .addOnSuccessListener { doc ->
                // toObject returns null if the document doesn't exist
                onResult(doc.toObject(Alert::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}


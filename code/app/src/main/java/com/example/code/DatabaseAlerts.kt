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

    fun addAlert(alert: Alert){
        _alerts.add(alert)
        alertsRef.document(alert.alertID).set(alert)
    }

    fun deleteAlert(alert: Alert){
        _alerts.removeAll { it.alertID == alert.alertID }
        alertsRef.document(alert.alertID).delete()
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


package com.example.code

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Entrant
import com.example.code.classes.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

/** Database of Entrants, representing the Entrants' screens
 */
class DatabaseEntrants(private val db: FirebaseFirestore = Firebase.firestore) {
    private val entrantsRef = db.collection("Entrants")
    private val _entrants = mutableStateListOf<Entrant>()

    val entrants: List<Entrant> get() = _entrants
    internal var reservationArchiveVersion by mutableIntStateOf(0)
        private set

    private fun docToEntrant(doc: DocumentSnapshot): Entrant {
        val entrant = Entrant(
            username = doc.getString("username") ?: doc.getString("name") ?: doc.id,
            email = doc.getString("email") ?: doc.getString("Email") ?: "",
            userID = doc.id,
            phoneNumber = doc.getString("phoneNumber") ?: "",
            description = doc.getString("description") ?: "",
            name = doc.getString("name") ?: "",
            dateOfBirth = doc.getString("dateOfBirth") ?: "",
            wantsNotifications = doc.getBoolean("wantsNotifications") ?: true,
            profileImage = "" // Loaded strictly via scoped images query to ensure device/entrant isolation
        )
        val lists = doc.get("campsiteLists") as? Map<*, *>
        lists?.forEach { (key, value) ->
            if (key is String) {
                entrant.campsiteLists[key] =
                    (value as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
            }
        }
        return entrant
    }

    init {
        entrantsRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                android.util.Log.e("DB", "entrants listener failed", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener
            val list = snapshots.documents.map { doc ->
                docToEntrant(doc)
            }
            _entrants.clear()
            _entrants.addAll(list)
            android.util.Log.d("DB", "Entrants loaded: ${_entrants.size}")
        }
    }

    fun fetchAllEntrants(onResult: (List<Entrant>) -> Unit) {
        entrantsRef.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    docToEntrant(doc)
                }
                _entrants.clear()
                _entrants.addAll(list)
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun addEntrant(entrant: Entrant) {
        if (!_entrants.any { it.getUserID() == entrant.getUserID() }) {
            _entrants.add(entrant)
        }
        entrantsRef.document(entrant.getUserID()).set(entrant)
    }

    fun updateEntrantUsername(entrant: Entrant, newUsername: String) {
        entrant.username = newUsername
        val index = _entrants.indexOfFirst { it.getUserID() == entrant.getUserID() }
        if (index != -1) {
            _entrants[index] = entrant
        }
        entrantsRef.document(entrant.getUserID()).update("username", newUsername)
    }

    fun deleteEntrant(entrant: Entrant) {
        _entrants.removeAll { it.getUserID() == entrant.getUserID() }
        entrantsRef.document(entrant.getUserID()).delete()
    }

    // Strictly search entrant and load profile image scoped ONLY to this entrantID from images collection
    fun searchEntrants(entrantID: String, onResult: (Entrant?) -> Unit) {
        entrantsRef.document(entrantID).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val entrant = docToEntrant(doc)
                    db.collection("images")
                        .whereEqualTo("entrantID", entrantID)
                        .get()
                        .addOnSuccessListener { imgSnap ->
                            val latestImgDoc = imgSnap.documents.maxByOrNull { it.getLong("timestamp") ?: 0L }
                            val pfpData = latestImgDoc?.getString("imageData") ?: latestImgDoc?.getString("profileImage")
                            if (!pfpData.isNullOrBlank() && latestImgDoc?.getString("entrantID") == entrantID) {
                                entrant.profileImage = pfpData
                            } else {
                                entrant.profileImage = ""
                            }
                            onResult(entrant)
                        }
                        .addOnFailureListener {
                            entrant.profileImage = ""
                            onResult(entrant)
                        }
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun searchEntrantsByEmail(email: String, onResult: (Entrant?) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        entrantsRef.get()
            .addOnSuccessListener { snapshot ->
                val matchingDoc = snapshot.documents.find { doc ->
                    val docEmail = doc.getString("email") ?: doc.getString("Email") ?: ""
                    docEmail.trim().lowercase() == cleanEmail
                }

                if (matchingDoc != null) {
                    val entrant = docToEntrant(matchingDoc).also { if (it.email.isBlank()) it.email = cleanEmail }
                    searchEntrants(entrant.getUserID()) { fullyLoaded ->
                        onResult(fullyLoaded ?: entrant)
                    }
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun updateEntrant(entrant: Entrant) {
        val index = _entrants.indexOfFirst { it.getUserID() == entrant.getUserID() }
        if (index != -1) {
            _entrants[index] = entrant
        }
        entrantsRef.document(entrant.getUserID())
            .update(
                mapOf(
                    "username" to entrant.username,
                    "email" to entrant.email,
                    "name" to entrant.name,
                    "dateOfBirth" to entrant.dateOfBirth,
                    "phoneNumber" to entrant.phoneNumber,
                    "description" to entrant.description,
                    "wantsNotifications" to entrant.wantsNotifications
                )
            )
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "updateEntrant failed for ${entrant.getUserID()}", e)
            }
    }

    fun addEntrantCampsite(
        entrant: Entrant,
        listKey: String,
        campsiteID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        entrant.addCampsite(listKey, campsiteID)
        entrantsRef.document(entrant.getUserID())
            .update(FieldPath.of("campsiteLists", listKey), FieldValue.arrayUnion(campsiteID))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "addEntrantCampsite failed for ${entrant.getUserID()}", e)
                onComplete(false)
            }
    }

    fun moveEntrantCampsite(
        entrant: Entrant,
        fromKey: String,
        toKey: String,
        campsiteID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        entrant.removeCampsite(fromKey, campsiteID)
        entrant.addNewList(toKey)
        entrant.addCampsite(toKey, campsiteID)

        entrantsRef.document(entrant.getUserID())
            .update(
                FieldPath.of("campsiteLists", fromKey), FieldValue.arrayRemove(campsiteID),
                FieldPath.of("campsiteLists", toKey), FieldValue.arrayUnion(campsiteID)
            )
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "moveEntrantCampsite failed for ${entrant.getUserID()}", e)
                onComplete(false)
            }
    }

    /** US 02.11.01. Reject stale acknowledgements and save the acknowledgement with the booking. */
    internal fun confirmStay(
        entrant: Entrant,
        message: ChatMessage,
        acknowledgedRules: List<String>,
        onComplete: (Boolean) -> Unit,
    ) {
        val campsiteID = message.campsiteID
        val campsiteRef = db.collection("campsites").document(campsiteID)
        val ref = entrantsRef.document(entrant.getUserID())
        val messageRef = db.collection("messages").document()
        val revision = FieldPath.of("reservationRevision", campsiteID)
        val dates = FieldPath.of("reservationDates", campsiteID, message.locationIndex.toString())
        val rulesAtBooking = FieldPath.of("reservationAcknowledgedRules", campsiteID, message.locationIndex.toString())
        db.runTransaction { transaction ->
            val campsite = transaction.get(campsiteRef)
            val publishedRules = (campsite.get("rules") as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (!campsite.exists() || publishedRules != acknowledgedRules) return@runTransaction false

            val snapshot = transaction.get(ref)
            val previous = snapshot.get(dates) as? List<*>
            val previousRules = snapshot.get(rulesAtBooking) as? List<*>
            val previousRevision = (snapshot.get(revision) as? Number)?.toLong() ?: 0L
            val reserved = snapshot.get(FieldPath.of("campsiteLists", RESERVED_CAMPSITES)) as? List<*>
            if (message.stayDates != previous || acknowledgedRules != previousRules ||
                reserved?.contains(campsiteID) != true) {
                val sentAt = maxOf(message.sentAt, previousRevision + 1)
                transaction.update(ref,
                    dates, message.stayDates,
                    rulesAtBooking, acknowledgedRules,
                    revision, sentAt,
                    FieldPath.of("campsiteLists", ACTIVE_CAMPSITES), FieldValue.arrayRemove(campsiteID),
                    FieldPath.of("campsiteLists", WAITING_LIST), FieldValue.arrayRemove(campsiteID),
                    FieldPath.of("campsiteLists", PAST_CAMPSITES), FieldValue.arrayRemove(campsiteID),
                    FieldPath.of("campsiteLists", RESERVED_CAMPSITES), FieldValue.arrayUnion(campsiteID)
                )
                transaction.set(messageRef, message.copy(
                    messageID = messageRef.id, sentAt = sentAt, rulesAcknowledged = true
                ))
            }
            true
        }.addOnSuccessListener { saved ->
            if (!saved) {
                onComplete(false)
                return@addOnSuccessListener
            }
            entrant.removeCampsite(ACTIVE_CAMPSITES, campsiteID)
            entrant.removeCampsite(WAITING_LIST, campsiteID)
            entrant.removeCampsite(PAST_CAMPSITES, campsiteID)
            entrant.addCampsite(RESERVED_CAMPSITES, campsiteID)
            onComplete(true)
        }.addOnFailureListener { error ->
            android.util.Log.e("DB", "Could not confirm reservation for $campsiteID", error)
            onComplete(false)
        }
    }

    internal fun archiveReservation(entrant: Entrant, campsiteID: String, revision: Long) {
        val ref = entrantsRef.document(entrant.getUserID())
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val reserved = snapshot.get(FieldPath.of("campsiteLists", RESERVED_CAMPSITES)) as? List<*>
            val currentRevision = (snapshot.get(FieldPath.of("reservationRevision", campsiteID)) as? Number)?.toLong()
            if (reserved?.contains(campsiteID) != true || currentRevision != revision) return@runTransaction false
            transaction.update(ref,
                FieldPath.of("campsiteLists", RESERVED_CAMPSITES), FieldValue.arrayRemove(campsiteID),
                FieldPath.of("campsiteLists", PAST_CAMPSITES), FieldValue.arrayUnion(campsiteID)
            )
            true
        }.addOnSuccessListener { moved ->
            if (moved) {
                entrant.removeCampsite(RESERVED_CAMPSITES, campsiteID)
                entrant.addCampsite(PAST_CAMPSITES, campsiteID)
                reservationArchiveVersion++
            }
        }.addOnFailureListener { error ->
            android.util.Log.e("DB", "Could not archive reservation for $campsiteID", error)
        }
    }

    fun removeEntrantCampsite(entrant: Entrant, listKey: String, campsiteID: String) {
        entrant.removeCampsite(listKey, campsiteID)
        entrantsRef.document(entrant.getUserID())
            .update(FieldPath.of("campsiteLists", listKey), FieldValue.arrayRemove(campsiteID))
    }
}

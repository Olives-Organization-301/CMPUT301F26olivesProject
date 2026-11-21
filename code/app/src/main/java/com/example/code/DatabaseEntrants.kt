package com.example.code

import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Entrant
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

/** Database of Entrants, representing the Entrants' screens
 * one of the prompts used : now when User Database is clicked, I want both collections "Entrants" and "
 *  * CampsiteOwners" displayed as how the logic for displaying collection "campsites" were in Admin
 */
class DatabaseEntrants {
    private val db = Firebase.firestore
    private val entrantsRef = db.collection("Entrants")
    private val _entrants = mutableStateListOf<Entrant>()

    val entrants: List<Entrant> get() = _entrants

    /** Builds a full Entrant Object from a Firestore document, including the profile fields
     *  (name, dateOfBirth, phoneNumber, description) and the campsite lists.
     *  Used Opus 5.5 in order to create an Entrant Object (For App usage)
     *  Prompt: "Why isn't Profile information persisting?"
     *  Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
     *  Date: Nov 20, 2026
     */
    private fun docToEntrant(doc: DocumentSnapshot): Entrant {
        // Get Doc Strings in order to fill out profile information
        val entrant = Entrant(
            username = doc.getString("username") ?: doc.getString("name") ?: doc.id,
            email = doc.getString("email") ?: doc.getString("Email") ?: "",
            userID = doc.id,
            phoneNumber = doc.getString("phoneNumber") ?: "",
            description = doc.getString("description") ?: "",
            name = doc.getString("name") ?: "",
            dateOfBirth = doc.getString("dateOfBirth") ?: "",
            wantsNotifications = doc.getBoolean("wantsNotifications") ?: true
        )
        // Get List of Campsites Entrant is getting it
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
                docToEntrant(doc) // Turn document into entrant
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
                    docToEntrant(doc) // Turn document into entrant object
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
        entrantsRef.document(entrant.getUserID()).set(entrant)
    }

    fun deleteEntrant(entrant: Entrant) {
        _entrants.removeAll { it.getUserID() == entrant.getUserID() }
        entrantsRef.document(entrant.getUserID()).delete()
    }

    fun searchEntrants(entrantID: String, onResult: (Entrant?) -> Unit) {
        entrantsRef.document(entrantID).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val entrant = docToEntrant(doc) // turn document into entrant
                    onResult(entrant)
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
                    onResult(entrant)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /** Saves the editable profile fields of an entrant.
     *  Uses update() on just these fields (instead of set() on the whole object) so the
     *  entrant's campsiteLists and anything else in the document are never overwritten.
     *  Usage: EntrantProfileScreen and Test
     *  Used Opus 5.5 in order to update entrant's profile
     *  Prompt: "Why isn't Profile information persisting?"
     *  Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
     *  Date: Nov 20, 2026
     */
    fun updateEntrant(entrant: Entrant) {
        // Find entrant from the Database
        val index = _entrants.indexOfFirst { it.getUserID() == entrant.getUserID() }
        if (index != -1) {
            _entrants[index] = entrant
        }
        // Update information
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

    /** Adds a campsite ID to one of the entrant's lists (e.g. "Waiting_List") in Firestore.
     *  Only touches campsiteLists.<listKey>, using arrayUnion so it's atomic and duplicate-free.
     *  Used Opus 5.5 in order to add entrants to the campsite's database
     *  Prompt: "Why isn't campsites persisting?"
     *  Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
     *  Date: Nov 20, 2026
     */
    fun addEntrantCampsite(
        entrant: Entrant,
        listKey: String,
        campsiteID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        // Add the campsite into the entrant object
        entrant.addCampsite(listKey, campsiteID)
        // Update the Firebase Document so Entrant's campsite is joined
        entrantsRef.document(entrant.getUserID())
            .update(FieldPath.of("campsiteLists", listKey), FieldValue.arrayUnion(campsiteID))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "addEntrantCampsite failed for ${entrant.getUserID()}", e)
                onComplete(false)
            }
    }

    /** Move Entrant's Campsite list from Waiting List to Active List
     * Prompt: "How do I move Entrants to active campsite when the lottery starts?"
     * Conversation: https://claude.ai/share/e282e67a-f3db-4225-b23d-1cadc614a63f (Cite better later)
     * */
    fun moveEntrantCampsite(
        entrant: Entrant,
        fromKey: String,
        toKey: String,
        campsiteID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        // Remove from waiting list
        entrant.removeCampsite(fromKey, campsiteID)

        // Add to active waiting list
        entrant.addNewList(toKey)
        entrant.addCampsite(toKey, campsiteID)

        // Document the Change in Firebase
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

    fun removeEntrantCampsite(entrant: Entrant, listKey: String, campsiteID: String) {
        entrant.removeCampsite(listKey, campsiteID)
        entrantsRef.document(entrant.getUserID())
            .update(FieldPath.of("campsiteLists", listKey), FieldValue.arrayRemove(campsiteID))
    }
}

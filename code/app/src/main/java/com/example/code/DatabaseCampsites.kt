package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Campsite
import com.example.code.classes.Coordinates
import com.google.firebase.Firebase
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import java.time.LocalDateTime
import kotlin.random.Random

/** Database of Campsites
 * represents every Campsite cached by the system, regardless of owner
 * Since every campsite class has its owner, use
 *
 *
 * one of the prompts used : now when User Database is clicked, I want both collections "Entrants" and "
 * CampsiteOwners" displayed as how the logic for displaying collection "campsites" were in Admin
 */

// From Gabe; I edited Campsite Details in order to change the dates of the campsites, so it requires this API (Sorry)
@RequiresApi(Build.VERSION_CODES.O)
class DatabaseCampsites {
    private val db = Firebase.firestore
    private val campsitesRef = db.collection("campsites")
    private val _campsites = mutableStateListOf<Campsite>()

    val campsites: List<Campsite> get() = _campsites

    init {
        // Direct real-time listener to sync all campsites from Firestore "campsites" collection
        campsitesRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                android.util.Log.e("DB", "campsites listener failed", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener

            val list = snapshots.documents.mapNotNull { doc ->
                parseCampsiteDocument(doc)
            }
            _campsites.clear()
            _campsites.addAll(list)
            android.util.Log.d("DB", "campsites synchronized: ${_campsites.size}")
        }
    }

    /** Builds one location from a map in a campsite's "coordinates" list.
     *  For older documents (a single map, or fields at the top level) [doc] supplies the fallbacks.
     */
    private fun coordinatesFromMap(coordMap: Map<*, *>?, doc: DocumentSnapshot?): Coordinates {
        val country = coordMap?.get("country")?.toString()
            ?: doc?.getString("country")
            ?: "Canada"

        val region = coordMap?.get("region")?.toString()
            ?: doc?.getString("region")
            ?: ""

        // Older documents stored the location under "firstSubdivision"
        val city = coordMap?.get("city")?.toString()
            ?: coordMap?.get("firstSubdivision")?.toString()
            ?: doc?.getString("city")
            ?: doc?.getString("location")
            ?: doc?.getString("firstSubdivision")
            ?: ""

        val address = coordMap?.get("address")?.toString()
            ?: doc?.getString("address")
            ?: ""

        return Coordinates(country, region, city, address)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseCampsiteDocument(doc: DocumentSnapshot): Campsite? {
        return try {
            if (!doc.exists()) return null

            val name = doc.getString("name")
                ?: doc.getString("Name")
                ?: doc.getString("campsiteName")
                ?: doc.id

            val ownerID = doc.get("ownerID")?.toString()
                ?: doc.get("OwnerID")?.toString()
                ?: doc.get("ownerId")?.toString()
                ?: doc.get("owner_id")?.toString()
                ?: ""

            // maxOccupancy is a list of per-day capacities; older documents stored a single number
            val maxOccObj = doc.get("maxOccupancy")
            val maxOcc: MutableList<Int> = when (maxOccObj) {
                is List<*> -> maxOccObj.mapNotNull { (it as? Number)?.toInt() ?: it?.toString()?.toIntOrNull() }
                is Number -> listOf(maxOccObj.toInt())
                else -> listOfNotNull(maxOccObj?.toString()?.toIntOrNull())
            }.ifEmpty { listOf(4) }.toMutableList()

            val priceObj = doc.get("price") ?: doc.get("Price")
            val price = (priceObj as? Number)?.toFloat()
                ?: priceObj?.toString()?.toFloatOrNull()
                ?: 50.0f

            val ratingObj = doc.get("rating") ?: doc.get("Rating")
            val rating = (ratingObj as? Number)?.toFloat()
                ?: ratingObj?.toString()?.toFloatOrNull()

            // "coordinates" is a list of locations; older documents stored a single map
            val coordsField = doc.get("coordinates")
            val coords: MutableList<Coordinates> = when (coordsField) {
                is List<*> -> coordsField.mapNotNull { entry ->
                    (entry as? Map<*, *>)?.let { coordinatesFromMap(it, null) }
                }
                else -> listOf(coordinatesFromMap(coordsField as? Map<*, *>, doc))
            }.toMutableList()

            val vacationTimes = (doc.get("vacationStartTimes") as? List<*>)?.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                try {
                    LocalDateTime.parse(map["start"].toString()) to LocalDateTime.parse(map["end"].toString())
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            // Lottery dates are stored as ISO-8601 strings
            var start = doc.getString("lotteryStartDate")?.let {
                try { LocalDateTime.parse(it) } catch (e: Exception) { null }
            }
            var end = doc.getString("lotteryEndDate")?.let {
                try { LocalDateTime.parse(it) } catch (e: Exception) { null }
            }

            // Older campsites never saved their lottery dates. Give them the same default as a new
            // campsite (a 7-day lottery starting now) and save it once, so the dates stop changing.
            if (start == null && end == null) {
                start = LocalDateTime.now().withNano(0)
                end = start.plusDays(7)
                campsitesRef.document(doc.id)
                    .update(
                        mapOf(
                            "lotteryStartDate" to start.toString(),
                            "lotteryEndDate" to end.toString()
                        )
                    )
                    .addOnFailureListener { e ->
                        android.util.Log.e("DB", "Saving default lottery dates failed for ${doc.id}", e)
                    }
            }

            Campsite(
                name = name,
                ownerID = ownerID,
                campsiteID = doc.id,
                maxOccupancy = maxOcc,
                price = price,
                rating = rating,
                coordinates = coords,
                thumbnailImage = doc.getString("thumbnailImage"),
                detailsImage = doc.getString("detailsImage"),
                lotteryStartDate = start,
                lotteryEndDate = end,
                vacationStartTimes = vacationTimes.toMutableList(),
            ).also { campsite ->
                val waiting = (doc.get("waitingList") as? List<*>)?.filterIsInstance<String>()
                if (waiting != null) campsite.setWaitingList(waiting)
                val active = (doc.get("activeLotteryList") as? List<*>)?.filterIsInstance<String>()
                if (active != null) campsite.setActiveLotteryList(active)
                val finalIDs = (doc.get("finalList") as? List<*>)?.filterIsInstance<String>()
                if (finalIDs != null) campsite.setFinalList(finalIDs)
            }
        } catch (e: Exception) {
            android.util.Log.e("DB", "Failed to parse document ${doc.id}", e)
            null
        }
    }

    // Fetch all campsites from the firebase collection "campsites"
    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchAllCampsites(onResult: (List<Campsite>) -> Unit) {
        campsitesRef.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    parseCampsiteDocument(doc)
                }
                _campsites.clear()
                _campsites.addAll(list)
                android.util.Log.d("DB", "fetchAllCampsites loaded ${list.size} campsites")
                onResult(list)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "fetchAllCampsites failed", e)
                onResult(emptyList())
            }
    }

    // Generate a unique numeric string ID between 1 and 1,000,000 without repetition
    fun generateUniqueCampsiteID(onResult: (String) -> Unit) {
        val randomId = Random.nextInt(1, 1000000).toString()
        campsitesRef.document(randomId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    generateUniqueCampsiteID(onResult)
                } else {
                    onResult(randomId)
                }
            }
            .addOnFailureListener {
                onResult(randomId)
            }
    }

    fun addCampsite(campsite: Campsite) {
        try {
            if (!_campsites.any { it.campsiteID == campsite.campsiteID }) {
                _campsites.add(campsite)
            }
            campsitesRef.document(campsite.campsiteID).set(campsite.toMap())
        } catch (e: Exception) {
            // Catch exception
        }
    }

    fun updateCampsiteName(campsite: Campsite, newName: String) {
        try {
            val index = _campsites.indexOfFirst { it.campsiteID == campsite.campsiteID }
            if (index != -1) {
                _campsites[index] = campsite
            }
            campsitesRef.document(campsite.campsiteID).update("name", newName)
        } catch (e: Exception) {
            // Catch exception
        }
    }

    fun updateCampsiteRating(campsite: Campsite, newRating: Float) {
        try {
            val index = _campsites.indexOfFirst { it.campsiteID == campsite.campsiteID }
            if (index != -1) {
                campsite.setRating(newRating)
                _campsites[index] = campsite
            }
            campsitesRef.document(campsite.campsiteID).update("rating", newRating)
        } catch (e: Exception) {
            // Catch exception
        }
    }

    /** Adds an entrant's ID to the campsite's "waitingList" array in Firestore.
     *  arrayUnion is atomic and never adds duplicates, so two people joining at once is safe.
     *  Used Opus 5.5 in order to add people into the waiting list
     *  Prompt: "Why isn't my Entrant being added to a campsite?"
     *  Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
     *  Date: Nov 20, 2026
     */
    fun addToWaitingList(
        campsite: Campsite,
        entrantID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        campsite.addEntrant(entrantID)
        campsitesRef.document(campsite.campsiteID)
            .update("waitingList", FieldValue.arrayUnion(entrantID))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "addToWaitingList failed for ${campsite.campsiteID}", e)
                onComplete(false)
            }
    }

    /** Removes an entrant's ID from the campsite's "waitingList" array in Firestore.
     *  Used Opus 5.5 in order to update entrant's profile
     *  Prompt: "Why isn't my entrant being removed from a campsite?"
     *  Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
     *  Date: Nov 20, 2026
     */
    fun removeFromWaitingList(
        campsite: Campsite,
        entrantID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        campsite.removeEntrant(entrantID)
        removeFromWaitingList(campsite.campsiteID, entrantID, onComplete)
    }

    /** Same as above, but only needs the campsite's ID (for when the Campsite object isn't loaded). */
    fun removeFromWaitingList(
        campsiteID: String,
        entrantID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        _campsites.firstOrNull { it.campsiteID == campsiteID }?.removeEntrant(entrantID)
        campsitesRef.document(campsiteID)
            .update("waitingList", FieldValue.arrayRemove(entrantID))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "removeFromWaitingList failed for $campsiteID", e)
                onComplete(false)
            }
    }

    /** Adds an entrant straight to the campsite's "activeLotteryList"
     * (joining while the lottery is running).
     */
    fun addToActiveLottery(
        campsite: Campsite,
        entrantID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        // Add active lottery to the campsite
        campsite.addActiveLotteryEntrant(entrantID)
        // Document the change, with exceptions
        campsitesRef.document(campsite.campsiteID)
            .update("activeLotteryList", FieldValue.arrayUnion(entrantID))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "addToActiveLottery failed for ${campsite.campsiteID}", e)
                onComplete(false)
            }
    }

    /** Removes an entrant's ID from the campsite's "activeLotteryList". */
    fun removeFromActiveLottery(
        campsite: Campsite,
        entrantID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        campsite.removeEntrant(entrantID)
        removeFromActiveLottery(campsite.campsiteID, entrantID, onComplete)
    }

    /** Same as above, but only needs the campsite's ID (for when the Campsite object isn't loaded). */
    fun removeFromActiveLottery(
        campsiteID: String,
        entrantID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        _campsites.firstOrNull { it.campsiteID == campsiteID }?.removeEntrant(entrantID)
        campsitesRef.document(campsiteID)
            .update("activeLotteryList", FieldValue.arrayRemove(entrantID))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "removeFromActiveLottery failed for $campsiteID", e)
                onComplete(false)
            }
    }

    /** Moves an entrant from the campsite's "waitingList" to its "activeLotteryList" once the lottery starts. */
    fun moveToActiveLottery(
        campsite: Campsite,
        entrantID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        campsite.moveToActiveLottery(entrantID)
        campsitesRef.document(campsite.campsiteID)
            .update(
                mapOf(
                    "waitingList" to FieldValue.arrayRemove(entrantID),
                    "activeLotteryList" to FieldValue.arrayUnion(entrantID)
                )
            )
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "moveToActiveLottery failed for ${campsite.campsiteID}", e)
                onComplete(false)
            }
    }

    /** Removes an entrant's ID from the waitingList, activeLotteryList and finalList of every
     *  campsite that contains it. Called when an entrant deletes their profile so no campsite
     *  keeps a dangling ID.
     */
    fun removeEntrantFromAllCampsites(entrantID: String, onComplete: (Boolean) -> Unit = {}) {
        _campsites.forEach { it.removeEntrant(entrantID) }

        // One query per list field (Firestore allows only one array-contains per query)
        val listFields = listOf("waitingList", "activeLotteryList", "finalList")
        val queries = listFields.map { field ->
            campsitesRef.whereArrayContains(field, entrantID).get()
        }

        Tasks.whenAllSuccess<QuerySnapshot>(queries)
            .addOnSuccessListener { snapshots ->
                // Campsite doc -> the list fields that actually contain this entrant
                // This for loop deleted entrant ID from the firebase after deletion
                val updates = mutableMapOf<DocumentReference, MutableMap<String, Any>>()
                snapshots.forEachIndexed { i, snapshot ->
                    snapshot.documents.forEach { doc ->
                        updates.getOrPut(doc.reference) { mutableMapOf() }[listFields[i]] =
                            FieldValue.arrayRemove(entrantID)
                    }
                }
                if (updates.isEmpty()) {
                    onComplete(true)
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                updates.forEach { (ref, fields) -> batch.update(ref, fields) }
                batch.commit()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { e ->
                        android.util.Log.e("DB", "removeEntrantFromAllCampsites failed for $entrantID", e)
                        onComplete(false)
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "removeEntrantFromAllCampsites query failed for $entrantID", e)
                onComplete(false)
            }
    }

    fun deleteCampsite(campsite: Campsite) {
        try {
            _campsites.removeAll { it.campsiteID == campsite.campsiteID }
            campsitesRef.document(campsite.campsiteID).delete()
        } catch (e: Exception) {
            // Catch exception
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun searchCampsite(campsiteID: String, onResult: (Campsite?) -> Unit) {
        campsitesRef.document(campsiteID).get()
            .addOnSuccessListener { doc ->
                onResult(parseCampsiteDocument(doc))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // Check ownerID matching supporting both String and Int types in Firestore
    fun getCampsitesForOwner(ownerID: String, onResult: (List<Campsite>) -> Unit) {
        campsitesRef.get()
            .addOnSuccessListener { snapshot ->
                val targetStr = ownerID.trim()
                val targetInt = ownerID.trim().toIntOrNull()

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val docOwnerObj = doc.get("ownerID") ?: doc.get("OwnerID") ?: doc.get("ownerId")
                        val docOwnerStr = docOwnerObj?.toString()?.trim() ?: ""
                        val docOwnerInt = docOwnerObj?.let {
                            when (it) {
                                is Number -> it.toInt()
                                is String -> it.toIntOrNull()
                                else -> null
                            }
                        }

                        val matches = docOwnerStr == targetStr || (targetInt != null && docOwnerInt == targetInt) || docOwnerStr.isBlank()

                        if (matches) {
                            parseCampsiteDocument(doc)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}

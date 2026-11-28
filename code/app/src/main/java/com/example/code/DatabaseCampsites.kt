package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.Campsite
import com.example.code.classes.Coordinates
import com.example.code.classes.Review
import com.example.code.classes.ReviewReply
import com.google.firebase.Firebase
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
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
class DatabaseCampsites(private val db: FirebaseFirestore = Firebase.firestore) {
    private val campsitesRef = db.collection("campsites")
    private val imagesRef = db.collection("images")
    private val _campsites = mutableStateListOf<Campsite>()
    internal val sharedLotteries by lazy { SharedLotteryRepository(db) }
    internal val lotteryDraws by lazy { LotteryDrawRepository(db, ::parseCampsiteDocument, sharedLotteries::parse) }

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

        val latitude = (coordMap?.get("latitude") as? Number)?.toDouble()
        val longitude = (coordMap?.get("longitude") as? Number)?.toDouble()
        return Coordinates(country, region, city, address, latitude, longitude)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseCampsiteDocument(doc: DocumentSnapshot, migrateLegacyFields: Boolean = true): Campsite? {
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
            if (migrateLegacyFields && start == null && end == null) {
                start = lotteryNow().withNano(0)
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
                thumbnailImage = doc.get("thumbnailImage") as? String,
                detailsImage = doc.get("detailsImage") as? String,
                lotteryStartDate = start,
                lotteryEndDate = end,
                vacationStartTimes = vacationTimes.toMutableList(),
            ).also { campsite ->
                val waiting = (doc.get("waitingList") as? List<*>)?.filterIsInstance<String>().orEmpty()
                // Older campsites kept lottery-day joiners in a separate "activeLotteryList".
                // Fold them into the waiting list and drop the old field once, so it stops existing.
                val legacyActive = (doc.get("activeLotteryList") as? List<*>)?.filterIsInstance<String>()
                campsite.setWaitingList((waiting + legacyActive.orEmpty()).distinct())
                campsite.waitlistCapacity = (doc.get("waitlistCapacity") as? Number)?.toInt()?.takeIf { it > 0 }
                campsite.lotteryDrawnAt = (doc.get("lotteryDrawnAt") as? Number)?.toLong()
                campsite.lotteryNotificationsPending = doc.getBoolean("lotteryNotificationsPending") == true
                campsite.sharedLotteryID = doc.getString("lotteryID")?.takeIf { it.isNotBlank() }
                if (legacyActive != null && migrateLegacyFields) {
                    val update = mutableMapOf<String, Any>("activeLotteryList" to FieldValue.delete())
                    if (legacyActive.isNotEmpty()) {
                        update["waitingList"] = FieldValue.arrayUnion(*legacyActive.toTypedArray())
                    }
                    campsitesRef.document(doc.id).update(update)
                        .addOnFailureListener { e ->
                            android.util.Log.e("DB", "Merging activeLotteryList failed for ${doc.id}", e)
                        }
                }
                val finalIDs = (doc.get("finalList") as? List<*>)?.filterIsInstance<String>()
                if (finalIDs != null) campsite.setFinalList(finalIDs)
                // Map the Winners of the lottery
                val winners = (doc.get("locationWinners") as? Map<*, *>)
                    ?.mapNotNull { (key, value) ->
                        val ids = (value as? List<*>)?.filterIsInstance<String>()
                        if (key is String && ids != null) key to ids else null
                    }?.toMap()
                if (winners != null) campsite.setLocationWinners(winners)
                doc.getString("description")?.let { campsite.setDescription(it) }
                (doc.get("tags") as? List<*>)?.filterIsInstance<String>()?.let { campsite.setTags(it) }
                val rulesList = (doc.get("rules") as? List<*>)?.filterIsInstance<String>()
                if (rulesList != null) campsite.setRules(rulesList)
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

    /**
     * Creates a campsite and its optional photo. When [saveRulesAsOwnerDefault] is true,
     * the same batch saves the published rules as the owner's template for new listings.
     * See [AddEditCampsite] for the Codex prompt that introduced the template.
     */
    fun addCampsite(campsite: Campsite, saveRulesAsOwnerDefault: Boolean = false): Task<Void> {
        if (_campsites.none { it.campsiteID == campsite.campsiteID }) {
            _campsites.add(campsite)
        }
        val batch = db.batch()
        val campsiteID = campsite.campsiteID
        batch.set(campsitesRef.document(campsiteID), campsite.toMap())
        if (saveRulesAsOwnerDefault) {
            batch.set(
                db.collection("CampsiteOwners").document(campsite.getOwnerID()),
                mapOf("defaultCampsiteRules" to campsite.getRules()),
                SetOptions.merge()
            )
        }
        campsite.detailsPhoto()?.takeIf { it.isNotBlank() }?.let { photo ->
            batch.set(
                campsiteImageRef(campsiteID),
                campsiteImageRecord(campsiteID, campsite.getOwnerID(), campsite.getName(), photo)
            )
        }
        return batch.commit()
            .addOnFailureListener { _campsites.remove(campsite) }
    }

    /** # Database Campsites
     *  Saves the owner's edits to an existing listing, including its published rules.
     *  The template for new listings is saved by [addCampsite].
     *
     *  Prompt: "US 02.01.02 As a campsite owner, I want to provide campsite details including
     *  name, location, description (amenities), maximum occupancy, map location, optional image, and tags."
     *  Implemented with Android Studio AI Agent.
     */
    fun updateCampsiteDetails(
        campsiteID: String,
        name: String,
        location: String,
        description: String,
        maxOccupancy: Int,
        latitude: Double?,
        longitude: Double?,
        tags: List<String>,
        rules: List<String>,
        availableStayRange: Pair<LocalDateTime, LocalDateTime>? = null,
    ): Task<Void> {
        val fields = mutableMapOf<String, Any>(
            "name" to name,
            "description" to description,
            "maxOccupancy" to listOf(maxOccupancy),
            "tags" to tags,
            "rules" to rules,
            "coordinates" to listOf(
                Coordinates("Canada", "", location, location, latitude, longitude).toMap()
            )
        )
        if (availableStayRange != null) {
            fields["vacationStartTimes"] = listOf(mapOf(
                "start" to availableStayRange.first.toString(),
                "end" to availableStayRange.second.toString()
            ))
        }
        return campsitesRef.document(campsiteID).update(fields)
    }

    /** A null image leaves both fields untouched. An empty image removes the saved photo. */
    fun updateCampsiteListing(campsiteID: String, name: String, image: String?): Task<Void> {
        val campsiteRef = campsitesRef.document(campsiteID)
        val imageRef = campsiteImageRef(campsiteID)
        return db.runTransaction<Void> { transaction ->
            val campsite = transaction.get(campsiteRef)
            val fields = mutableMapOf<String, Any?>("name" to name)
            if (image != null) {
                fields["detailsImage"] = image.ifBlank { null }
                fields["thumbnailImage"] = image.ifBlank { null }
            }
            val photo = image ?: (campsite.get("detailsImage") as? String)
                ?.takeIf { it.isNotBlank() } ?: (campsite.get("thumbnailImage") as? String)
            transaction.update(campsiteRef, fields)
            if (!photo.isNullOrBlank()) {
                val ownerID = (campsite.get("ownerID") ?: campsite.get("OwnerID")
                    ?: campsite.get("ownerId") ?: campsite.get("owner_id"))?.toString().orEmpty()
                transaction.set(imageRef, campsiteImageRecord(campsiteID, ownerID, name, photo))
            } else if (image != null) {
                transaction.delete(imageRef)
            }
            null
        }
    }

    private fun campsiteImageRef(campsiteID: String) = imagesRef.document("campsite_$campsiteID")

    private fun campsiteImageRecord(campsiteID: String, ownerID: String, name: String, photo: String) =
        mapOf(
            "campsiteID" to campsiteID,
            "campsiteownerID" to ownerID,
            "campsiteName" to name,
            "campsiteImage" to photo,
            "imageData" to photo,
            "timestamp" to System.currentTimeMillis()
        )

    //claude.ai used for creating the removeCampsiteImage function and it's accompanying methods
    //In the main kotlin functions.
    //chat here: https://claude.ai/share/cdf52d32-f73c-4fc2-b19e-b7015e307051

    fun removeCampsiteImage(campsiteID: String): Task<Void>{
        val batch = db.batch()
        batch.update(
            campsitesRef.document(campsiteID),
            mapOf("detailsImage" to null, "thumbnailImage" to null)
        )
        batch.delete(campsiteImageRef(campsiteID))
        return batch.commit()
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
    internal fun addToWaitingList(
        campsite: Campsite,
        entrantID: String,
        onComplete: (WaitlistJoinResult) -> Unit = {}
    ) {
        val ref = campsitesRef.document(campsite.campsiteID)
        db.runTransaction { transaction ->
            val current = parseCampsiteDocument(transaction.get(ref), migrateLegacyFields = false)
                ?: return@runTransaction WaitlistJoinResult.FAILED
            if (current.sharedLotteryID != null) return@runTransaction WaitlistJoinResult.FAILED
            val result = current.waitlistJoinResult(entrantID, lotteryNow())
            if (result == WaitlistJoinResult.JOINED && !current.entrantInList(entrantID)) {
                transaction.update(ref, "waitingList", FieldValue.arrayUnion(entrantID))
            }
            result
        }.addOnSuccessListener { result ->
            if (result == WaitlistJoinResult.JOINED) campsite.addEntrant(entrantID)
            onComplete(result)
        }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "addToWaitingList failed for ${campsite.campsiteID}", e)
                onComplete(WaitlistJoinResult.FAILED)
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
        val groupID = campsite.sharedLotteryID
        if (groupID != null) {
            sharedLotteries.updateEntry(groupID, entrantID, join = false).addOnCompleteListener {
                onComplete(it.isSuccessful && it.result == WaitlistJoinResult.JOINED)
            }
            return
        }
        removeFromWaitingList(campsite.campsiteID, entrantID) { removed ->
            if (removed) campsite.removeEntrant(entrantID)
            onComplete(removed)
        }
    }

    /** Same as above, but only needs the campsite's ID (for when the Campsite object isn't loaded). */
    fun removeFromWaitingList(
        campsiteID: String,
        entrantID: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val groupID = _campsites.firstOrNull { it.campsiteID == campsiteID }?.sharedLotteryID
        if (groupID != null) {
            sharedLotteries.updateEntry(groupID, entrantID, join = false).addOnCompleteListener {
                onComplete(it.isSuccessful && it.result == WaitlistJoinResult.JOINED)
            }
            return
        }
        val ref = campsitesRef.document(campsiteID)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val current = parseCampsiteDocument(snapshot, migrateLegacyFields = false)
                ?: return@runTransaction false
            if (current.sharedLotteryID != null ||
                registrationGate(current.getStartDate(), current.getEndDate(), current.isLotteryDrawn(), lotteryNow()) != WaitlistJoinResult.JOINED) {
                return@runTransaction false
            }
            val updates = mutableMapOf<String, Any>("waitingList" to FieldValue.arrayRemove(entrantID))
            if (snapshot.contains("activeLotteryList")) {
                updates["activeLotteryList"] = FieldValue.arrayRemove(entrantID)
            }
            transaction.update(ref, updates)
            true
        }.addOnSuccessListener { removed ->
            if (removed) _campsites.firstOrNull { it.campsiteID == campsiteID }?.removeEntrant(entrantID)
            onComplete(removed)
        }
            .addOnFailureListener { e ->
                android.util.Log.e("DB", "removeFromWaitingList failed for $campsiteID", e)
                onComplete(false)
            }
    }

    /** Removes an entrant's ID from the waitingList and finalList of every
     *  campsite that contains it. Called when an entrant deletes their profile so no campsite
     *  keeps a dangling ID.
     */
    fun removeEntrantFromAllCampsites(entrantID: String, onComplete: (Boolean) -> Unit = {}) {
        _campsites.forEach { it.removeEntrant(entrantID) }

        // One query per list field (Firestore allows only one array-contains per query)
        val listFields = listOf("waitingList", "finalList")
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
            db.batch()
                .delete(campsitesRef.document(campsite.campsiteID))
                .delete(campsiteImageRef(campsite.campsiteID))
                .commit()
        } catch (e: Exception) {
            // Catch exception
        }
    }

    /** Saves the dates and optional cap together after checking the live waitlist. */
    internal fun updateLotteryListing(
        campsiteID: String,
        ownerID: String,
        start: LocalDateTime,
        end: LocalDateTime,
        capacity: Int?,
    ): Task<Boolean> {
        val ref = campsitesRef.document(campsiteID)
        return db.runTransaction { transaction ->
            val campsite = parseCampsiteDocument(transaction.get(ref), migrateLegacyFields = false)
                ?: return@runTransaction false
            if (campsite.getOwnerID() != ownerID || campsite.sharedLotteryID != null ||
                !campsite.canEditLottery(start, end, capacity, lotteryNow())) {
                return@runTransaction false
            }
            transaction.update(ref, mapOf(
                "lotteryStartDate" to start.toString(),
                "lotteryEndDate" to end.toString(),
                "waitlistCapacity" to capacity
            ))
            true
        }
    }

    private fun reviews(campsiteID: String) = campsitesRef.document(campsiteID).collection("reviews")

    internal fun observeReviews(campsiteID: String, onResult: (List<Review>?) -> Unit) =
        reviews(campsiteID).orderBy("createdAt").addSnapshotListener { snapshot, error ->
            onResult(if (error != null) null else snapshot?.documents?.map { doc ->
                Review(doc.id, doc.getString("entrantID").orEmpty(), campsiteID,
                    doc.getLong("rating")?.toInt() ?: 0, doc.getString("review").orEmpty(),
                    doc.getString("authorName").orEmpty()).apply {
                    @Suppress("UNCHECKED_CAST")
                    upvotedEntrants = (doc.get("upvotedEntrants") as? List<String>).orEmpty().toMutableList()
                    @Suppress("UNCHECKED_CAST")
                    downvotedEntrants = (doc.get("downvotedEntrants") as? List<String>).orEmpty().toMutableList()
                }
            })
        }

    /** Recomputes the campsite's rating (out of 5) as the mean of all its reviews and saves it.
     *  Reviews store their rating out of 10, so each one is halved. */
    internal fun refreshCampsiteRating(campsiteID: String): Task<Void> =
        reviews(campsiteID).get().continueWithTask { task ->
            val ratings = task.result.documents.mapNotNull { it.getLong("rating") }.filter { it > 0 }
            val average = if (ratings.isEmpty()) null
                else Math.round(ratings.average() / 2.0 * 10.0) / 10f // one decimal place
            _campsites.firstOrNull { it.campsiteID == campsiteID }?.setRating(average)
            campsitesRef.document(campsiteID).update("rating", average)
        }

    /** Sets this entrant's vote on a review: 1 = upvote, -1 = downvote, 0 = remove their vote.
     *  An entrant can only be in one of the two lists at a time. */
    internal fun voteOnReview(review: Review, entrantID: String, vote: Int): Task<Void> {
        val ref = reviews(review.campsiteID).document(review.reviewID)
        return ref.update(mapOf(
            "upvotedEntrants" to if (vote == 1) FieldValue.arrayUnion(entrantID) else FieldValue.arrayRemove(entrantID),
            "downvotedEntrants" to if (vote == -1) FieldValue.arrayUnion(entrantID) else FieldValue.arrayRemove(entrantID)
        ))
    }

    internal fun postReview(review: Review): Task<Boolean> {
        val ref = reviews(review.campsiteID).document(review.entrantID)
        return db.runTransaction { transaction ->
            if (transaction.get(ref).exists()) return@runTransaction false
            transaction.set(ref, mapOf(
                "entrantID" to review.entrantID, "authorName" to review.authorName,
                "rating" to review.rating, "review" to review.review,
                "upvotedEntrants" to emptyList<String>(), "downvotedEntrants" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp()
            ))
            true
        }.continueWithTask { posted ->
            // Once the review is saved, update the campsite's aggregate rating to include it
            if (posted.isSuccessful && posted.result == true) {
                refreshCampsiteRating(review.campsiteID).continueWith { true }
            } else posted
        }
    }

    //claude.ai used to create remove campsite ratings and reviews and it's following functions
    //in the main methods.
    //chat here: https://claude.ai/share/cdf52d32-f73c-4fc2-b19e-b7015e307051

    internal fun removeReviewRating(review: Review): Task<Void> =
        reviews(review.campsiteID).document(review.reviewID).update("rating", 0)

    internal fun deleteReview(review: Review): Task<Void>{
        val reviewRef = reviews(review.campsiteID).document(review.reviewID)
        return reviewRef.collection("replies").get().continueWithTask{ task ->
                val batch = db.batch()
                task.result?.documents?.forEach{batch.delete(it.reference) }
                batch.delete(reviewRef)
                batch.commit()
        }
    }


    internal fun observeReviewReplies(review: Review, onResult: (List<ReviewReply>?) -> Unit) =
        reviews(review.campsiteID).document(review.reviewID).collection("replies").orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                onResult(if (error != null) null else snapshot?.documents?.map { doc ->
                    ReviewReply(doc.id, review.reviewID, doc.getString("entrantID").orEmpty(),
                        review.campsiteID, doc.getString("contents").orEmpty(), doc.getString("authorName").orEmpty())
                })
            }

    internal fun postReviewReply(reply: ReviewReply): Task<Boolean> {
        val review = reviews(reply.campsiteID).document(reply.parentReviewID)
        val ref = review.collection("replies").document()
        return db.runTransaction { transaction ->
            if (!transaction.get(review).exists()) return@runTransaction false
            transaction.set(ref, mapOf(
                "entrantID" to reply.entrantID, "authorName" to reply.authorName,
                "contents" to reply.contents, "createdAt" to FieldValue.serverTimestamp()
            ))
            true
        }
    }

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

package com.example.code.classes

/** Class for Storing Review
 * @param entrantID ID of Entrant (Should be one per campsite)
 * @param campsiteID ID of Campsite the review belongs to
 * @param rating (out of ten, but will be shifted to of 5 for review) integer rating
 * @param review Contents of the string review
 */
class Review(
    val reviewID: String,
    val entrantID: String,
    val campsiteID: String,
    var rating: Int = 0,
    var review: String = "",
    val authorName: String = "",
) {
    /**
     * isEdited: Track if review was edited
     * replies: List of Replies under a Review
     * upvotedEntrants: Entrants (IDs as proxies) approve review (Cache in their IDs to avoid duplication)
     * downvotedEntrants: Entrants (IDs as proxies) disapproved (Cache in their IDs to avoid duplication)
     */
    var isEdited: Boolean = false;
    var replies = mutableListOf<ReviewReply>()
    var upvotedEntrants = mutableListOf<String>()
    var downvotedEntrants = mutableListOf<String>()

    fun calculateTotalUpvote(): Int {
        return upvotedEntrants.size - downvotedEntrants.size
    }

}
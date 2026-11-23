package com.example.code.classes

/**
 * ReviewReply is the class that stores a reply to a review (Only one review.)
 * @param replyID Cache in Reply ID
 * @param parentReviewID Tie Reply to Review (They can't be on their own)
 * @param entrantID Cache in which Entrant replied
 * @param campsiteID Cache in which Campsite they replied it to
 * @param contents Actual Reply
 */
class ReviewReply(
    val replyID: String,
    val parentReviewID: String,
    val entrantID: String,
    val campsiteID: String,
    var contents: String = ""
) {

}
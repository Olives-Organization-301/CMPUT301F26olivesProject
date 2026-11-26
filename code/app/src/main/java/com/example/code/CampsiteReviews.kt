package com.example.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.code.classes.Campsite
import com.example.code.classes.Entrant
import com.example.code.classes.Review
import com.example.code.classes.ReviewReply

@Composable
internal fun CampsiteReviewsTab(
    campsite: Campsite?,
    entrant: Entrant?,
    database: DatabaseCampsites,
    textColor: Color
) {
    val campsiteID = campsite?.campsiteID ?: return
    var reviews by remember(campsiteID) { mutableStateOf<List<Review>?>(null) }
    var loading by remember(campsiteID) { mutableStateOf(true) }
    DisposableEffect(database, campsiteID) {
        val listener = database.observeReviews(campsiteID) {
            reviews = it
            loading = false
        }
        onDispose { listener.remove() }
    }

    val loaded = reviews
    if (loaded == null) {
        Text(if (loading) "Loading reviews…" else "Could not load reviews.", color = textColor)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (loaded.isEmpty()) Text("No reviews yet.", color = textColor)
        if (entrant != null && loaded.none { it.entrantID == entrant.getUserID() }) {
            var rating by remember(campsiteID, entrant.getUserID()) { mutableStateOf(0) }
            Text("Rate this campsite", color = textColor)
            Row {
                (1..5).forEach { stars ->
                    TextButton(modifier = Modifier.weight(1f), onClick = { rating = stars }) {
                        Text(if (stars <= rating) "★" else "☆", color = textColor)
                    }
                }
            }
            ReviewComposer("Write a review", "Post review", rating > 0, textColor) { text, finish ->
                database.postReview(Review(
                    entrant.getUserID(), entrant.getUserID(), campsiteID, rating * 2, text,
                    entrant.name.ifBlank { entrant.username }
                )).addOnCompleteListener { finish(it.isSuccessful && it.result == true) }
            }
        }

        loaded.forEach { review ->
            androidx.compose.runtime.key(review.reviewID) {
                ReviewCard(review, entrant, database, textColor)
            }
        }
    }
}

@Composable
private fun ReviewCard(review: Review, entrant: Entrant?, database: DatabaseCampsites, textColor: Color) {
    var replies by remember { mutableStateOf<List<ReviewReply>?>(emptyList()) }
    var replying by remember { mutableStateOf(false) }
    DisposableEffect(database, review.campsiteID, review.reviewID) {
        val listener = database.observeReviewReplies(review) { replies = it }
        onDispose { listener.remove() }
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(textColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("${review.authorName.ifBlank { "Entrant" }} · ${review.rating / 2f}/5", color = textColor, fontWeight = FontWeight.Bold)
        Text(review.review, color = textColor)
        if (replies == null) Text("Could not load replies.", color = textColor)
        replies.orEmpty().forEach { reply ->
            Text("${reply.authorName.ifBlank { "Entrant" }}: ${reply.contents}", color = textColor, modifier = Modifier.padding(start = 12.dp))
        }
        if (entrant != null) {
            TextButton(onClick = { replying = !replying }) {
                Text(if (replying) "Cancel reply" else "Reply", color = textColor)
            }
            if (replying) {
                ReviewComposer("Write a reply", "Post reply", true, textColor) { text, finish ->
                    database.postReviewReply(ReviewReply(
                        "", review.reviewID, entrant.getUserID(), review.campsiteID, text,
                        entrant.name.ifBlank { entrant.username }
                    )).addOnCompleteListener {
                        val saved = it.isSuccessful && it.result == true
                        finish(saved)
                        if (saved) replying = false
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewComposer(
    label: String,
    action: String,
    ready: Boolean,
    textColor: Color,
    onPost: (String, (Boolean) -> Unit) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text,
        onValueChange = { if (it.length <= 2_000) text = it },
        label = { Text(label) },
        enabled = !saving,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Button(enabled = ready && text.isNotBlank() && !saving, onClick = {
        saving = true
        failed = false
        onPost(text.trim()) { saved ->
            saving = false
            failed = !saved
            if (saved) text = ""
        }
    }) { Text(action) }
    if (failed) Text("Could not post. Try again.", color = textColor)
}

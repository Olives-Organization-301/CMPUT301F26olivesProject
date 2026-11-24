package com.example.code

import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore

/** # Database Chat
 *  Implementation of US 02.12.01: Campsite owner messaging a user who is reserving their campsite.
 *
 *  Prototype was gained from a Conversation with Android Studio's agent
 *  Prompt: "Create a Firestore database class for real-time chat message synchronization"
 *
 */
class DatabaseChat {
    private val db = Firebase.firestore
    private val messagesRef = db.collection("messages")
    private val _messages = mutableStateListOf<ChatMessage>()

    val messages: List<ChatMessage> get() = _messages

    init {
        // Real-time listener syncing all chat messages from Firestore
        messagesRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                android.util.Log.e("DB", "chat listener failed", error)
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener
            val list = snapshots.documents.mapNotNull { doc -> parseMessage(doc) }
            _messages.clear()
            _messages.addAll(list)
        }
    }

    /**
     * Builds a ChatMessage from a Firestore document, field by field.
     * toObject() throws if any field has the wrong type (e.g. sentAt saved as a Timestamp or
     * String, or fromOwner saved as "true" from the Firebase console), and that message would
     * silently vanish. Reading each field leniently keeps those messages.
     * Prompt: https://claude.ai/share/a85f01c0-dfd7-4bfa-9315-8d607e4416f0
     */
    private fun parseMessage(doc: DocumentSnapshot): ChatMessage? {
        return try {
            ChatMessage(
                messageID = doc.getString("messageID")?.ifBlank { null } ?: doc.id,
                campsiteID = doc.get("campsiteID")?.toString().orEmpty().trim(),
                campsiteName = doc.get("campsiteName")?.toString().orEmpty(),
                sender = doc.get("sender")?.toString().orEmpty(),
                timestamp = doc.get("timestamp")?.toString().orEmpty(),
                text = doc.get("text")?.toString().orEmpty(),
                fromOwner = asBoolean(doc.get("fromOwner")) ?: true,
                entrantID = doc.get("entrantID")?.toString().orEmpty().trim(),
                ownerID = doc.get("ownerID")?.toString().orEmpty().trim(),
                automated = asBoolean(doc.get("automated")) ?: false,
                type = doc.get("type")?.toString()?.ifBlank { null } ?: MESSAGE_TEXT,
                locationIndex = asLong(doc.get("locationIndex"))?.toInt() ?: -1,
                sentAt = asLong(doc.get("sentAt")) ?: 0L
            )
        } catch (e: Exception) {
            android.util.Log.e("DB", "could not read message ${doc.id}", e)
            null
        }
    }

    private fun asBoolean(value: Any?): Boolean? = when (value) {
        is Boolean -> value
        is String -> value.trim().lowercase().toBooleanStrictOrNull()
        else -> null
    }

    /** Millis from a Long/Double, a Firestore Timestamp, or a numeric String */
    private fun asLong(value: Any?): Long? = when (value) {
        is Number -> value.toLong()
        is Timestamp -> value.toDate().time
        is String -> value.trim().toLongOrNull()
        else -> null
    }

    /**
     * Persists a new ChatMessage object into the Firestore database collection.
     */
    fun addMessage(message: ChatMessage) {
        val id = if (message.messageID.isBlank()) java.util.UUID.randomUUID().toString() else message.messageID
        message.messageID = id
        if (!_messages.any { it.messageID == id }) {
            _messages.add(message)
        }
        messagesRef.document(id).set(message)
    }

    /** Every message between [entrantID] and [campsiteID], oldest first
     * Filter Messages to capture correspondence between an Entrant and its Campsite,
     * Using its strings; Return it as a list
     */
    fun conversation(campsiteID: String, entrantID: String): List<ChatMessage> =
        _messages.filter { it.campsiteID == campsiteID && it.entrantID == entrantID }
            .sortedBy { it.sentAt }

    /** [entrantID]'s conversations: campsiteID -> its messages (oldest first),
     *  with the most recently active conversation first
     *  From an Entrant, get the campsites it has messaged and return
     *  the campsites it has messaged from
     */
    fun conversationsFor(entrantID: String): Map<String, List<ChatMessage>> =
        _messages.filter { it.entrantID == entrantID && it.campsiteID.isNotBlank() }
            .groupBy { it.campsiteID }
            .mapValues { (_, msgs) -> msgs.sortedBy { it.sentAt } }
            .entries
            .sortedByDescending { (_, msgs) -> msgs.last().sentAt }
            .associate { it.key to it.value }

    /**
     * Deletes a ChatMessage document from Firestore by message ID.
     */
    fun deleteMessage(messageID: String) {
        _messages.removeAll { it.messageID == messageID }
        messagesRef.document(messageID).delete()
    }
}

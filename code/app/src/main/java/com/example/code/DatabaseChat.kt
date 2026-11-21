package com.example.code

import androidx.compose.runtime.mutableStateListOf
import com.example.code.classes.ChatMessage
import com.google.firebase.Firebase
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
            val list = snapshots.documents.mapNotNull { doc ->
                try {
                    doc.toObject(ChatMessage::class.java)?.apply {
                        if (messageID.isBlank()) messageID = doc.id
                    }
                } catch (e: Exception) {
                    null
                }
            }
            _messages.clear()
            _messages.addAll(list)
        }
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

    /**
     * Deletes a ChatMessage document from Firestore by message ID.
     */
    fun deleteMessage(messageID: String) {
        _messages.removeAll { it.messageID == messageID }
        messagesRef.document(messageID).delete()
    }
}

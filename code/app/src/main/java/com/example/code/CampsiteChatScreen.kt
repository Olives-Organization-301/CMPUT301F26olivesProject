package com.example.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** # Campsite Chat Screen
 *  Implementation of US 02.12.01: Campsite owner messaging a user who is reserving their campsite.
 *
 *  Prototype was gained from a Conversation with Android Studio's agent
 *  Prompt: "Create an interactive chat screen with real-time text messaging and input field"
 *
 */

private val ChatBackground = Color(0xFFD4D0CF)
private val BubbleColor = Color(0xFF2B2B2B)
private val SubtitleColor = Color(0xFF666666)

@Composable
fun CampsiteChatScreen(
    navController: NavController,
    appState: AppState? = null,
    campsiteName: String = "Campsite Name",
    ownerName: String = "Owner Name",
    entrantName: String = "Entrant Name",
    messages: List<ChatMessage> = emptyList()
) {
    var messageText by remember { mutableStateOf("") }
    val dbMessages = appState?.chat?.messages ?: emptyList()
    val displayMessages = if (dbMessages.isNotEmpty()) {
        dbMessages
    } else if (messages.isNotEmpty()) {
        messages
    } else {
        listOf(
            ChatMessage(sender = "Entrant Name", timestamp = "Date, Time", text = "[Message Txt]", fromOwner = false),
            ChatMessage(sender = "Owner Name", timestamp = "Date, Time", text = "[Message Txt]", fromOwner = true),
            ChatMessage(sender = "Entrant Name", timestamp = "Date, Time", text = "[Message Txt]", fromOwner = false)
        )
    }

    val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackground)
    ) {
        // To avoid clipping into top status bar
        Spacer(Modifier.height(48.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(end = 14.dp)
            ) {
                Text("Back")
            }

            Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 14.dp)) {
                Text(
                    text = campsiteName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = "Owner: $ownerName - Entrant: $entrantName",
                    fontSize = 12.sp,
                    color = SubtitleColor
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(displayMessages) { message ->
                MessageRow(message)
            }
        }

        // Chat Input Bar for Campsite Owner to message user reserving campsite
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val newMsg = ChatMessage(
                                messageID = java.util.UUID.randomUUID().toString(),
                                campsiteName = campsiteName,
                                sender = ownerName,
                                timestamp = timeFormatter.format(Date()),
                                text = messageText.trim(),
                                fromOwner = true,
                                entrantID = entrantName,
                                ownerID = ownerName
                            )
                            appState?.chat?.addMessage(newMsg)
                            messageText = ""
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun MessageRow(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.fromOwner) Alignment.End else Alignment.Start
    ) {
        Text(
            text = "${message.sender}-${message.timestamp}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = message.text,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BubbleColor)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}

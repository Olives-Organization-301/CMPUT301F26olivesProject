package com.example.code

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
 *  Codex prompt summary: Open Messages to list entrants with reservations at the owner's
 *  campsites, then verify the flow on the emulator.
 *
 */

private val ChatBackground = Color(0xFFD4D0CF)
private val BubbleColor = Color(0xFF2B2B2B)
private val SubtitleColor = Color(0xFF666666)

@Composable
fun CampsiteChatScreen(
    navController: NavController,
    appState: AppState
) {
    val ownerID = appState.userID?.takeIf { appState.role == "owner" }
    var campsiteID by rememberSaveable(ownerID) { mutableStateOf<String?>(null) }
    var entrantID by rememberSaveable(ownerID) { mutableStateOf<String?>(null) }

    // Reserved Campsites is the confirmed list. The same entrant can have a thread at each site.
    val entrantsByCampsite = appState.entrants.entrants.flatMap { entrant ->
        entrant.getCampsiteLists()[RESERVED_CAMPSITES].orEmpty().distinct().map { it to entrant }
    }.groupBy({ it.first }, { it.second })
    val reservations = appState.campsites.campsites
        .filter { ownerID != null && it.getOwnerID() == ownerID }
        .sortedBy { it.getName() }
        .flatMap { campsite ->
            entrantsByCampsite[campsite.campsiteID].orEmpty()
                .sortedBy { it.name.ifBlank { it.username } }
                .map { entrant -> campsite to entrant }
        }
    val selection = reservations.firstOrNull { (campsite, entrant) ->
        campsite.campsiteID == campsiteID && entrant.getUserID() == entrantID
    }
    val closeThread = {
        campsiteID = null
        entrantID = null
    }
    BackHandler(enabled = selection != null, onBack = closeThread)
    if (selection != null && ownerID != null) {
        val (campsite, entrant) = selection
        val owner = appState.owners.campsiteOwners.firstOrNull { it.getUserID() == ownerID }
        OwnerChatThread(
            campsiteName = campsite.getName(),
            ownerName = owner?.name?.ifBlank { owner.username }?.ifBlank { null } ?: "Campsite Owner",
            entrantName = entrant.name.ifBlank { entrant.username },
            messages = appState.chat.conversation(campsite.campsiteID, entrant.getUserID()),
            onBack = closeThread,
            onSend = { message ->
                appState.chat.addMessage(message.copy(
                    campsiteID = campsite.campsiteID,
                    entrantID = entrant.getUserID(),
                    ownerID = ownerID
                ))
            }
        )
        return
    }

    Column(Modifier.fillMaxSize().background(ChatBackground).navigationBarsPadding().padding(14.dp)) {
        Spacer(Modifier.height(34.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
            Text("Messages", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }
        if (reservations.isEmpty()) {
            Text("No entrants with confirmed reservations.", modifier = Modifier.padding(top = 12.dp), color = Color.Black)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reservations) { (campsite, entrant) ->
                Surface(
                    onClick = {
                        campsiteID = campsite.campsiteID
                        entrantID = entrant.getUserID()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(entrant.name.ifBlank { entrant.username }, fontWeight = FontWeight.Medium)
                        Text(campsite.getName(), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerChatThread(
    campsiteName: String,
    ownerName: String,
    entrantName: String,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSend: (ChatMessage) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }
    val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackground)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // To avoid clipping into top status bar
        Spacer(Modifier.height(48.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = onBack,
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
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages) { message ->
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
                    enabled = messageText.isNotBlank(),
                    onClick = {
                        onSend(ChatMessage(
                            campsiteName = campsiteName,
                            sender = ownerName,
                            timestamp = timeFormatter.format(Date()),
                            text = messageText.trim(),
                            fromOwner = true
                        ))
                        messageText = ""
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

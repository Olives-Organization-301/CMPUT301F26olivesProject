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
import com.example.code.classes.Campsite
import com.example.code.classes.Entrant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** # Campsite Chat Screen
 *  Implementation of US 02.12.01: Campsite owner messaging a user who is reserving their campsite.
 *
 *  Prototype was gained from a Conversation with Android Studio's agent
 *  Prompt: "Create an interactive chat screen with real-time text messaging and input field"
 *
 *  Codex prompts: "so when entrant registers, for campsite owner messages no entrant messages
 *  appear, fix that"; "make test cases for this in any case"; and "can owner B see messages
 *  from the entrants on that campsite collab". Registration shows an entrant on every
 *  member campsite's owner inbox, while each campsite retains its own message thread.
 *
 */

private val ChatBackground = Color(0xFFD4D0CF)
private val BubbleColor = Color(0xFF2B2B2B)
private val SubtitleColor = Color(0xFF666666)

/**
 * Builds one thread per entrant and owned campsite from registrations, bookings and shared
 * lottery entrants. Deduplication keeps an entrant registered through two lists in one row.
 * The owner filter prevents a collaborator from seeing another owner's campsite messages.
 */
internal fun ownerMessageThreads(
    ownerID: String?,
    campsites: List<Campsite>,
    entrants: List<Entrant>,
    lotteries: List<SharedLottery>,
): List<Pair<Campsite, Entrant>> {
    if (ownerID == null) return emptyList()
    val entrantsByID = entrants.associateBy { it.getUserID() }
    val listedBySite = entrants.flatMap { entrant ->
        listOf(WAITING_LIST, ACTIVE_CAMPSITES, RESERVED_CAMPSITES)
            .flatMap { entrant.getCampsiteLists()[it].orEmpty() }
            .distinct().map { it to entrant.getUserID() }
    }.groupBy({ it.first }, { it.second })
    val lotteriesByID = lotteries.associateBy { it.id }
    return campsites.asSequence()
        .filter { it.getOwnerID() == ownerID }
        .sortedBy { it.getName() }
        .flatMap { site ->
            (listedBySite[site.campsiteID].orEmpty() +
                (site.sharedLotteryID?.let(lotteriesByID::get)?.entrants.orEmpty()))
                .distinct().mapNotNull(entrantsByID::get)
                .sortedBy { it.name.ifBlank { it.username } }
                .asSequence().map { site to it }
        }.toList()
}

@Composable
fun CampsiteChatScreen(
    navController: NavController,
    appState: AppState
) {
    val ownerID = appState.userID?.takeIf { appState.role == "owner" }
    var campsiteID by rememberSaveable(ownerID) { mutableStateOf<String?>(null) }
    var entrantID by rememberSaveable(ownerID) { mutableStateOf<String?>(null) }

    // Registration can place an entrant on the waiting or active list before they reserve.
    // Keep one conversation per entrant and campsite across all three stages.
    val conversations = ownerMessageThreads(
        ownerID, appState.campsites.campsites, appState.entrants.entrants,
        appState.campsites.sharedLotteries.all()
    )
    val selection = conversations.firstOrNull { (campsite, entrant) ->
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
        if (conversations.isEmpty()) {
            Text("No registered entrants yet.", modifier = Modifier.padding(top = 12.dp), color = Color.Black)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(conversations) { (campsite, entrant) ->
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

        // Chat Input Bar for the campsite owner to message a registered entrant
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

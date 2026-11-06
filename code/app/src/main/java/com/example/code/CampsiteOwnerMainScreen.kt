package com.example.code

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.window.DialogProperties
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.Coordinates
import com.example.code.classes.PreviewCampsite
import com.example.code.classes.WaitlistEntrant
import java.time.LocalDateTime
/** # Search Screen
 *  Implementation of the Entrants' Main Screen as seen in the Figma.
 *
 *  Prototype was gained from this conversation with Claude Sonnet 5.0:
 *  https://claude.ai/chat/fa52c57a-03b0-486b-b988-87d2f36ff18a
 *  Prompt: "How do I Correctly pad this Main screen? How do I make the Cards?"
 *
 */
//https://claude.ai/share/a95788a4-8990-40f1-95c7-b5a744a1e94d: Prompt:I need to make a button to be able to go to campsitechatscreen
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteOwnerMainScreen(navController: NavController, appState: AppState) {
    var searchString by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var ownerName by remember { mutableStateOf("Campsite Owner") }
    var showEdit by remember { mutableStateOf<PreviewCampsite?>(null) }
    var showLottery by remember { mutableStateOf<PreviewCampsite?>(null) }
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val previewCampsites = remember {
        mutableStateListOf<PreviewCampsite>()
    }

    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    // Load real campsites and owner name using viewCampsites function
    LaunchedEffect(appState.userID) {
        val ownerID = appState.userID
        if (!ownerID.isNullOrBlank()) {
            try {
                appState.owners.searchCampsiteOwner(ownerID) { owner ->
                    mainHandler.post {
                        if (owner != null) {
                            ownerName = owner.username.ifBlank { owner.email }
                            owner.viewCampsites(appState.campsites) { campsites ->
                                mainHandler.post {
                                    previewCampsites.clear()
                                    for (c in campsites) {
                                        val cName = try { c.getName().ifBlank { "Campsite" } } catch (e: Exception) { "Campsite" }
                                        val cLoc = try { c.getCoordinates()?.getFirstSubdivision()?.ifBlank { "Location" } ?: "Location" } catch (e: Exception) { "Location" }
                                        previewCampsites.add(
                                            PreviewCampsite(
                                                name = cName,
                                                location = cLoc,
                                                startMillis = null,
                                                endMillis = null,
                                                waitingList = emptyList()
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            appState.campsites.getCampsitesForOwner(ownerID) { campsites ->
                                mainHandler.post {
                                    previewCampsites.clear()
                                    for (c in campsites) {
                                        val cName = try { c.getName().ifBlank { "Campsite" } } catch (e: Exception) { "Campsite" }
                                        val cLoc = try { c.getCoordinates()?.getFirstSubdivision()?.ifBlank { "Location" } ?: "Location" } catch (e: Exception) { "Location" }
                                        previewCampsites.add(
                                            PreviewCampsite(
                                                name = cName,
                                                location = cLoc,
                                                startMillis = null,
                                                endMillis = null,
                                                waitingList = emptyList()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Prevent crash
            }
        }
    }

    val filteredCampsites = if (searchString.isBlank()) {
        previewCampsites
    } else {
        previewCampsites.filter { it.name.contains(searchString, ignoreCase = true) }
    }

    // Column Wrapping the Search/Card Screens and Menu bar
    Column(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.weight(1f)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // To avoid clipping it into the top of the screen
                Spacer(Modifier.height(48.dp))

                // Signed in owner verification banner
                Text(
                    text = "Signed in as: $ownerName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF274690),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Search Bar
                TextField(
                    value = searchString,
                    onValueChange = { searchString = it },
                    label = { Text("Search for your campsite") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )

                // The Cards of Campsites
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(filteredCampsites) { preview ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB0B0B0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .background(Color.DarkGray, RoundedCornerShape(16.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(preview.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                    Text(preview.location, color = Color.White)
                                    Text("Entry: " + (preview.startMillis?.let { format.format(Date(it)) } ?: "Not set"), color = Color.White)
                                    Text("End: " + (preview.endMillis?.let { format.format(Date(it)) } ?: "Not set"), color = Color.White)
                                }

                                Spacer(Modifier.width(8.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Button(onClick = { showEdit = preview }) { Text("  Edit  ", maxLines = 1) }
                                    Button(onClick = { showLottery = preview }) { Text("Lottery", maxLines = 1) }
                                }
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { showAdd = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "Add campsite") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray)
                .navigationBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("List", maxLines = 1) }
            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("QR Code", maxLines = 1) }
            Button(onClick = { navController.navigate("loginScreen") }, modifier = Modifier.weight(1f)) { Text("Account", maxLines = 1) }
            Button(onClick = { navController.navigate("campsiteChat") }, modifier = Modifier.weight(1f)) { Text("Message Entrants", maxLines = 1) }
        }
    }

    if (showAdd) {
        AddEditCampsite(
            title = "Add Campsite",
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { newCampsite ->
                previewCampsites.add(newCampsite)
                appState.campsites.generateUniqueCampsiteID { uniqueId ->
                    val coords = Coordinates("Canada", newCampsite.location, -113.49, 53.54)
                    val realCampsite = Campsite(
                        name = newCampsite.name,
                        ownerID = appState.userID ?: "unknown_owner",
                        campsiteID = uniqueId,
                        maxOccupancy = 4,
                        price = 50.0f,
                        rating = 5.0f,
                        capacity = 4,
                        coordinates = coords,
                        startDate = LocalDateTime.now(),
                        endDate = LocalDateTime.now().plusDays(7)
                    )
                    appState.campsites.addCampsite(realCampsite)
                }
                showAdd = false
            }
        )
    }

    showEdit?.let { current ->
        AddEditCampsite(
            title = "Edit Campsite",
            initial = current,
            onDismiss = { showEdit = null },
            onSave = { updated ->
                val i = previewCampsites.indexOfFirst { it === current }
                if (i >= 0) previewCampsites[i] = updated
                showEdit = null
            }
        )
    }

    showLottery?.let { current ->
        LotteryDialog(
            campsite = current,
            onDismiss = { showLottery = null },
            onSave = { updated ->
                val i = previewCampsites.indexOfFirst { it === current }
                if (i >= 0) previewCampsites[i] = updated
                showLottery = null
            }
        )
    }
}

@Composable
fun AddEditCampsite(
    title: String,
    initial: PreviewCampsite?,
    onDismiss: () -> Unit,
    onSave: (PreviewCampsite) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var startMillis by remember { mutableStateOf(initial?.startMillis) }
    var endMillis by remember { mutableStateOf(initial?.endMillis) }
    val context = LocalContext.current
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val startText = startMillis?.let { format.format(Date(it)) } ?: "Not set"
    val endText = endMillis?.let { format.format(Date(it)) } ?: "Not set"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            onSave(
                                PreviewCampsite(
                                    name = name,
                                    location = location,
                                    startMillis = startMillis,
                                    endMillis = endMillis,
                                    waitingList = initial?.waitingList ?: emptyList()
                                )
                            )
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Campsite name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Location")
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Address or place name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Lottery dates")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickDate(context, null) { startMillis = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Entry: $startText", maxLines = 1) }

                        OutlinedButton(
                            onClick = { pickDate(context, startMillis) { endMillis = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("End: $endText", maxLines = 1) }
                    }
                }
            }
        }
    }
}

/*
Implementation of owner screen as outlined in the figma. Search screen was used as reference.
AddCampsite, pickdate, and parts of campsiteOwnerMainScreen UI were developed using claude 3.5 in
this chat: https://claude.ai/share/140477f9-42aa-4b16-8189-dad57b7d8afb
Prompt: "How do I change this so that the delete and edit buttons are on top of each other at the end of each card"
 */
@Composable
fun LotteryDialog(
    campsite: PreviewCampsite,
    onDismiss: () -> Unit,
    onSave: (PreviewCampsite) -> Unit,
) {
    val context = LocalContext.current
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    var startMillis by remember { mutableStateOf(campsite.startMillis) }
    var endMillis by remember { mutableStateOf(campsite.endMillis) }
    var waitingList by remember { mutableStateOf(campsite.waitingList) }

    var newEntrantName by remember { mutableStateOf("") }
    var newEntrantEmail by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val isLotteryConfigured = (startMillis != null) && (endMillis != null)
    val endMillisVal = endMillis ?: now
    val isLotteryActive = isLotteryConfigured && now <= endMillisVal
    val isExpired = isLotteryConfigured && now > endMillisVal
    var numLotteryWinners by remember { mutableStateOf(5) }
    var showSetWinnersDialog by remember { mutableStateOf(false) }
    var inputWinnersText by remember { mutableStateOf(numLotteryWinners.toString()) }

    fun startLottery() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sMillis = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val eMillis = calendar.timeInMillis

        startMillis = sMillis
        endMillis = eMillis
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Lottery: ${campsite.name}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            onSave(
                                PreviewCampsite(
                                    name = campsite.name,
                                    location = campsite.location,
                                    startMillis = startMillis,
                                    endMillis = endMillis,
                                    waitingList = waitingList
                                )
                            )
                        }
                    ) {
                        Text("Save")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Information for this lottery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val startText = startMillis?.let { format.format(Date(it)) } ?: "Not set"
                        val endText = endMillis?.let { format.format(Date(it)) } ?: "Not set"

                        Text("Lottery started on: $startText", fontSize = 14.sp)
                        Text("Lottery will expire on: $endText", fontSize = 14.sp)
                        Text("Lottery will have $numLotteryWinners winners", fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(6.dp))

                        val statusText = when {
                            !isLotteryConfigured -> "Status: Lottery not placed yet"
                            isLotteryActive -> {
                                val daysLeft = endMillis?.let { ((it - now) / (1000 * 60 * 60 * 24)).coerceAtLeast(0) } ?: 0
                                "Status: Lottery in progress ($daysLeft days until draw)"
                            }
                            else -> "Status: Lottery has expired"
                        }

                        Text(
                            text = statusText,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLotteryActive) Color(0xFF2E7D32) else if (isExpired) Color.Red else Color.Unspecified
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { startLottery() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Start from today", maxLines = 1, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    pickDate(context, null) { s ->
                                        startMillis = s
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = s
                                            add(Calendar.DAY_OF_YEAR, 7)
                                        }
                                        endMillis = cal.timeInMillis
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Start from custom date", maxLines = 1, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    inputWinnersText = numLotteryWinners.toString()
                                    showSetWinnersDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Set # of selected", maxLines = 1, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (showSetWinnersDialog) {
                    AlertDialog(
                        onDismissRequest = { showSetWinnersDialog = false },
                        title = { Text("Set number of lottery winners") },
                        text = {
                            Column {
                                Text("Enter number of entrants to be drawn for this lottery:")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = inputWinnersText,
                                    onValueChange = { inputWinnersText = it },
                                    label = { Text("Number of winners") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val parsed = inputWinnersText.toIntOrNull()
                                    if (parsed != null && parsed >= 0) {
                                        numLotteryWinners = parsed
                                    }
                                    showSetWinnersDialog = false
                                }
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSetWinnersDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Waiting list (${waitingList.size} entrants)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = if (isExpired) "Lottery expired, viewing final waiting list"
                           else "Waiting list currently in progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (waitingList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No entrants yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(waitingList) { entrant ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = entrant.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(text = entrant.email, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                        Text(
                                            text = "Joined: ${format.format(Date(entrant.joinedMillis))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            waitingList = waitingList.filter { it.id != entrant.id }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove entrant", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Add entrant to waiting list (TESTING PURPOSES ONLY)", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newEntrantName,
                                onValueChange = { newEntrantName = it },
                                label = { Text("Name", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newEntrantEmail,
                                onValueChange = { newEntrantEmail = it },
                                label = { Text("Email", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (newEntrantName.isNotBlank()) {
                                        val newEntrant = WaitlistEntrant(
                                            id = java.util.UUID.randomUUID().toString(),
                                            name = newEntrantName,
                                            email = newEntrantEmail.ifBlank { "entrant@example.com" },
                                            joinedMillis = System.currentTimeMillis()
                                        )
                                        waitingList = waitingList + newEntrant
                                        newEntrantName = ""
                                        newEntrantEmail = ""
                                    }
                                }
                            ) {
                                Text("Add", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
/*
WAITLIST implemented in order to facilitate the lottery function.
Users/entrants will be allowed to join this waitlist for a period of
one week until it expires (IMPLEMENTED), at which point 5 (or less,
depending on waitlist size) random users on the list will be selected,
on a first-come first-serve basis, to reserve their spots.

They will be notified by the system and are free to turn down these
requests at will.

The owner can spectate the waiting list as it fills up (or not), right
until the deadline for waiting list closure. (IMPLEMENTED) Optionally
they may also be able to see the status of each selected member of the
waiting list - whether they have signed on or not.

THIS CODE WAS IMPLEMENTED BY GOOGLE GEMINI 11-06-2026, WITH THE PROMPT
"This button allows a campsite owner to place their campsite into a
lottery system for a certain period of time. Such a lottery will allow
for customers/entrants to join a "waiting list" for one week. The owner
can spectate this waiting list until its expiry."

The following code has been commented and slightly altered from the
version it provided for the sake of bringing it into line stylistically
with the rest of our code, and removing any extraneous/redundant features
it added.
 */
/*
                AGENT USED: GEMINI
                DATE OF PROMPT: 11-06-2026
                PROMPT USED:
                "Upon click, basic pop-up window with "ok" button
                where the owner can reset the value of numLotteryWinners"

                Some modifications have been performed to the original LLM output.
                 */

/*
This function was also implemented using the above prompt. It is useful
for picking a date, as with the "custom" lottery date feature.
 */
private fun pickDate(context: Context, minDateMillis: Long?, onSelected: (Long) -> Unit) {
    val cal = Calendar.getInstance()
    val dialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            cal.set(year, month, day)
            onSelected(cal.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )
    if (minDateMillis != null) {
        dialog.datePicker.minDate = minDateMillis
    }
    dialog.show()
}

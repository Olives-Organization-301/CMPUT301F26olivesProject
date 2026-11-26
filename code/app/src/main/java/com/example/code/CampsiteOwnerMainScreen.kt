package com.example.code

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.window.DialogProperties
import com.example.code.classes.Alert
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.Coordinates
import com.example.code.classes.PreviewCampsite
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
/** # Search Screen
 *  Implementation of the Entrants' Main Screen as seen in the Figma.
 *
 *  Prototype was gained from this conversation with Claude Sonnet 5.0:
 *  https://claude.ai/chat/fa52c57a-03b0-486b-b988-87d2f36ff18a
 *  Prompt: "How do I Correctly pad this Main screen? How do I make the Cards?"
 *
 */
//https://claude.ai/share/a95788a4-8990-40f1-95c7-b5a744a1e94d: Prompt:I need to make a button to be able to go to campsitechatscreen

// From Gabe; I edited Campsite Details in order to change the dates of the campsites,
// so it requires this API for date parsing
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteOwnerMainScreen(navController: NavController, appState: AppState) {
    val context = LocalContext.current
    var searchString by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showQRCodeDialog by remember { mutableStateOf(false) }
    var selectedQRCampsite by remember { mutableStateOf<PreviewCampsite?>(null) }
    var ownerName by remember { mutableStateOf("Campsite Owner") }
    var currentOwnerObj by remember { mutableStateOf<CampsiteOwner?>(null) }
    var showEdit by remember { mutableStateOf<PreviewCampsite?>(null) }
    var showLottery by remember { mutableStateOf<PreviewCampsite?>(null) }
    var showAlert by remember { mutableStateOf<PreviewCampsite?>(null) }
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val previewCampsites = remember {
        mutableStateListOf<PreviewCampsite>()
    }

    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    LaunchedEffect(appState.userID, appState.campsites.campsites.toList()) {
        val ownerID = appState.userID ?: return@LaunchedEffect
        val attempted = mutableSetOf<String>()
        while (true) {
            val now = LocalDateTime.now()
            appState.campsites.campsites
                .filter { it.getOwnerID() == ownerID && it.isReadyToDraw(now) }
                .forEach { campsite ->
                    if (attempted.add(campsite.campsiteID)) {
                        appState.campsites.drawClosedLottery(campsite.campsiteID, ownerID)
                            .addOnFailureListener { error ->
                                attempted.remove(campsite.campsiteID)
                                android.util.Log.e("DB", "Closing lottery failed for ${campsite.campsiteID}", error)
                            }
                    }
                }
            delay(1_000)
        }
    }

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
                                        val cLoc = try { c.getMainCoordinates()?.getCity()?.ifBlank { "Location" } ?: "Location" } catch (e: Exception) { "Location" }
                                        val cImage = c.thumbnailPhoto().orEmpty()
                                        previewCampsites.add(
                                            PreviewCampsite(
                                                name = cName,
                                                location = cLoc,
                                                startMillis = c.getStartDate()?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                                                endMillis = c.getEndDate()?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                                                startMillisRegis = null,
                                                endMillisRegis = null,
                                                waitingList = emptyList(),
                                                campsiteImage = cImage,
                                                campsiteID = c.campsiteID
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
                                        val cLoc = try { c.getMainCoordinates()?.getCity()?.ifBlank { "Location" } ?: "Location" } catch (e: Exception) { "Location" }
                                        val cImage = c.thumbnailPhoto().orEmpty()
                                        previewCampsites.add(
                                            PreviewCampsite(
                                                name = cName,
                                                location = cLoc,
                                                startMillis = c.getStartDate()?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                                                endMillis = c.getEndDate()?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                                                startMillisRegis = null,
                                                endMillisRegis = null,
                                                waitingList = emptyList(),
                                                campsiteImage = cImage,
                                                campsiteID = c.campsiteID
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
                                .heightIn(120.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB0B0B0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfilePicture(
                                    profileImage = preview.campsiteImage,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(16.dp))
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
                                    Button(onClick = { showAlert = preview }) { Text(" Alert ", maxLines = 1)}
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
/**
 * Displays Account details for campsite owner and allows user to log out
 * **/
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray)
                .navigationBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("List", maxLines = 1) }
            Button(onClick = { showQRCodeDialog = true }, modifier = Modifier.weight(1f)) { Text("QR Code", maxLines = 1) }
            Button(onClick = { navController.navigate("campsiteOwnerProfileScreen") }, modifier = Modifier.weight(1f)) { Text("Account", maxLines = 1) }
            Button(onClick = { navController.navigate("campsiteChat") }, modifier = Modifier.weight(1f)) { Text("Message Entrants", maxLines = 1) }
        }
    }

    if (showQRCodeDialog) {
        AlertDialog(
            onDismissRequest = { showQRCodeDialog = false },
            title = {
                Text(
                    text = "Generate QR Code",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (previewCampsites.isEmpty()) {
                        Text(
                            text = "No campsites available to generate QR code.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(previewCampsites) { campsite ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0E0D0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(campsite.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(campsite.location, fontSize = 12.sp, color = Color.DarkGray)
                                        }
                                        Button(
                                            onClick = {
                                                selectedQRCampsite = campsite
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF274690))
                                        ) {
                                            Text("Generate QR code", fontSize = 11.sp, color = Color.White, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQRCodeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    selectedQRCampsite?.let { campsite ->
        AlertDialog(
            onDismissRequest = { selectedQRCampsite = null },
            title = {
                Text(
                    text = "QR Code: ${campsite.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scanning this QR code links entrants to Campsite Details.", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))

                    QRCodeImage(
                        data = campsite.campsiteID,
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Location: ${campsite.location}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val matchedCampsite = appState.campsites.campsites.firstOrNull { it.campsiteID == campsite.campsiteID }
                        if (matchedCampsite != null) {
                            appState.viewingCampsiteID = matchedCampsite.campsiteID
                            selectedQRCampsite = null
                            showQRCodeDialog = false
                            navController.navigate("campsiteDetails")
                        } else {
                            selectedQRCampsite = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF274690))
                ) {
                    Text("Test Scan (View Details)", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedQRCampsite = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Text(
                    text = "Campsite Owner Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Name: $ownerName", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Owner ID: ${appState.userID ?: "Unknown"}", fontSize = 13.sp, color = Color.Gray)
                    Text("Role: Campsite Owner", fontSize = 13.sp, color = Color(0xFF274690), fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountDialog = false
                        appState.userID = null
                        appState.role = null
                        DeviceIdentity.clearEntrantId(context)
                        navController.navigate("loginScreen") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAdd) {
        AddEditCampsite(
            title = "Add Campsite",
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { newCampsite ->
                appState.campsites.generateUniqueCampsiteID { uniqueId ->
                    val coords = Coordinates(
                        country = "Canada",
                        region = "",
                        city = newCampsite.location,
                        address = ""
                    )
                    val realCampsite = Campsite(
                        name = newCampsite.name,
                        ownerID = appState.userID ?: "unknown_owner",
                        campsiteID = uniqueId,
                        maxOccupancy = mutableListOf(4),
                        price = 50.0f,
                        rating = 5.0f,
                        coordinates = mutableListOf(coords),
                        detailsImage = newCampsite.campsiteImage.ifBlank { null },
                        thumbnailImage = newCampsite.campsiteImage.ifBlank { null },
                        lotteryStartDate = LocalDateTime.now(),
                        lotteryEndDate = LocalDateTime.now().plusDays(7)
                    )
                    appState.campsites.addCampsite(realCampsite)
                        .addOnSuccessListener {
                            previewCampsites.add(
                                PreviewCampsite(
                                    name = newCampsite.name,
                                    location = newCampsite.location,
                                    startMillis = newCampsite.startMillis,
                                    endMillis = newCampsite.endMillis,
                                    startMillisRegis = newCampsite.startMillisRegis,
                                    endMillisRegis = newCampsite.endMillisRegis,
                                    campsiteImage = newCampsite.campsiteImage,
                                    campsiteID = uniqueId
                                )
                            )
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Could not save campsite", Toast.LENGTH_LONG).show()
                        }
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
                val changedImage = updated.campsiteImage.takeIf { it != current.campsiteImage }
                appState.campsites.updateCampsiteListing(current.campsiteID, updated.name, changedImage)
                    .addOnSuccessListener {
                        val index = previewCampsites.indexOfFirst { it.campsiteID == current.campsiteID }
                        if (index >= 0) previewCampsites[index] = updated
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Could not save campsite changes", Toast.LENGTH_LONG).show()
                    }
                showEdit = null
            }
        )
    }

    showLottery?.let { current ->
        LotteryDialog(
            campsite = current,
            liveCampsite = appState.campsites.campsites.firstOrNull { it.campsiteID == current.campsiteID },
            entrantNames = appState.entrants.entrants.associate { it.getUserID() to it.name.ifBlank { it.username } },
            onDismiss = { showLottery = null },
            onSave = { startMillis, endMillis ->
                val ownerID = appState.userID
                val start = startMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                val end = endMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                if (ownerID != null && start != null && end != null && !end.isBefore(start)) {
                    if (startMillis == current.startMillis && endMillis == current.endMillis) {
                        showLottery = null
                    } else {
                        appState.campsites.updateLotteryDates(current.campsiteID, ownerID, start.atStartOfDay(), end.atTime(23, 59, 59))
                            .addOnCompleteListener { result ->
                                if (result.isSuccessful && result.result == true) {
                                    val index = previewCampsites.indexOfFirst { it.campsiteID == current.campsiteID }
                                    if (index >= 0) previewCampsites[index] = PreviewCampsite(
                                        current.name, current.location, startMillis, endMillis,
                                        current.startMillisRegis, current.endMillisRegis, current.waitingList,
                                        current.campsiteImage, current.campsiteID
                                    )
                                    showLottery = null
                                } else {
                                    Toast.makeText(context, "Could not save lottery dates", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(context, "Choose a valid start and end date", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    showAlert?.let { current ->
        NotificationDialog(
            campsite = current,
            onDismiss = {showAlert = null},
            ownerName = ownerName,
            appState = appState
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
    var startMillisRegis by remember { mutableStateOf(initial?.startMillisRegis) }
    var endMillisRegis by remember { mutableStateOf(initial?.endMillisRegis) }
    var campsiteImage by remember { mutableStateOf(initial?.campsiteImage ?: "") }

    val context = LocalContext.current
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val (processingPhoto, takePhoto, openPhotoPicker) = rememberPhotoPicker { campsiteImage = it }

    val startText = startMillis?.let { format.format(Date(it)) } ?: "Not set"
    val endText = endMillis?.let { format.format(Date(it)) } ?: "Not set"
    val startTextRegis = startMillisRegis?.let { format.format(Date(it)) } ?: "Not set"
    val endTextRegis = endMillisRegis?.let { format.format(Date(it)) } ?: "Not set"

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
                                    startMillisRegis = startMillisRegis,
                                    endMillisRegis = endMillisRegis,
                                    waitingList = initial?.waitingList ?: emptyList(),
                                    campsiteImage = campsiteImage,
                                    campsiteID = initial?.campsiteID.orEmpty()
                                )
                            )
                        },
                        enabled = name.isNotBlank() && !processingPhoto
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

                    Text("Campsite Picture", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfilePicture(
                            profileImage = campsiteImage,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = takePhoto,
                                enabled = !processingPhoto,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Take a Picture", fontSize = 11.sp, maxLines = 1)
                            }

                            Button(
                                onClick = openPhotoPicker,
                                enabled = !processingPhoto,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Choose from Gallery", fontSize = 11.sp, maxLines = 1)
                            }

                            if (campsiteImage.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { campsiteImage = "" },
                                    enabled = !processingPhoto,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Remove", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    Text("Booking dates")
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

                    Text("Registration period")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickDate(context, null) { startMillisRegis = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Entry: $startTextRegis", maxLines = 1) }

                        OutlinedButton(
                            onClick = { pickDate(context, startMillisRegis) { endMillisRegis = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("End: $endTextRegis", maxLines = 1) }
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
/** Codex prompt: Show the real waiting list and per-location draw results in the owner dialog. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun LotteryDialog(
    campsite: PreviewCampsite,
    liveCampsite: Campsite?,
    entrantNames: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (Long?, Long?) -> Unit,
) {
    val context = LocalContext.current
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    var start by remember(campsite.campsiteID) { mutableStateOf(campsite.startMillis) }
    var end by remember(campsite.campsiteID) { mutableStateOf(campsite.endMillis) }
    val drawn = liveCampsite?.isLotteryDrawn() == true
    val entrants = liveCampsite?.getWaitingList().orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lottery: ${campsite.name}") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Starts: " + (start?.let { format.format(Date(it)) } ?: "Not set"))
                    Text("Closes: " + (end?.let { format.format(Date(it)) } ?: "Not set"))
                    if (!drawn) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickDate(context, null) { start = it } }) {
                                Text("Start date")
                            }
                            OutlinedButton(onClick = { pickDate(context, null) { end = it } }) {
                                Text("End date")
                            }
                        }
                    }
                    if (liveCampsite == null) {
                        Text("Loading lottery?")
                    } else if (drawn) {
                        Text("Draw complete ? ${liveCampsite.getFinalList().size} selected")
                    } else if (liveCampsite.isReadyToDraw(LocalDateTime.now())) {
                        Text("Lottery closed ? drawing results")
                    }
                    Text("Waiting list (${entrants.size} entrants)", fontWeight = FontWeight.Bold)
                }
                items(entrants, key = { it }) { entrantID ->
                    Text(entrantNames[entrantID]?.ifBlank { null } ?: entrantID)
                    if (drawn) {
                        val locations = liveCampsite?.getLocationsWon(entrantID).orEmpty()
                        Text(if (locations.isEmpty()) "Not selected"
                            else locations.joinToString { "Location ${it + 1}" })
                    }
                }
            }
        },
        confirmButton = {
            if (!drawn) {
                TextButton(enabled = start != null && end != null, onClick = {
                    onSave(start, end)
                }) { Text("Save dates") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/** Codex prompt: Generate a scannable campsite QR code from its Firestore document ID. */
internal fun createCampsiteQrBitmap(campsiteID: String): Bitmap {
    val matrix = QRCodeWriter().encode(campsiteID, BarcodeFormat.QR_CODE, 384, 384)
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK
        else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
}

@Composable
fun QRCodeImage(data: String, modifier: Modifier = Modifier) {
    val image = remember(data) { createCampsiteQrBitmap(data).asImageBitmap() }
    Image(bitmap = image, contentDescription = "Campsite QR code", modifier = modifier)
}
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

@Composable
fun NotificationDialog(
    campsite: PreviewCampsite,
    onDismiss: () -> Unit,
    ownerName: String,
    appState: AppState
){
    var alertTitle by remember { mutableStateOf("") }
    var alertBody by remember { mutableStateOf("") }

    fun CreateAlert(campsite: PreviewCampsite, owner: String, title: String, body: String){
        for(entrant in campsite.waitingList){
            val alertID = java.util.UUID.randomUUID()
            val newAlert = Alert(alertID.toString(), title, body, entrant.id, owner )
            appState.alerts.addAlert(newAlert)
        }

    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ){
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(15.dp)
            ) {
                Spacer(Modifier.height(48.dp))
                Row(){
                    Text(
                        text = "Send alert to entrants of ${campsite.name}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "✕",
                        fontSize = 24.sp,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                OutlinedTextField(
                    value = alertTitle,
                    onValueChange = { alertTitle = it },
                    label = { Text("Title", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = alertBody,
                    onValueChange = { alertBody = it },
                    label = { Text("Message", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(5.dp),
                    singleLine = true
                )
                Button(
                    onClick = {
                        CreateAlert(campsite, ownerName, alertTitle, alertBody)
                        alertTitle = ""
                        alertBody = ""
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().padding(5.dp)
                ){
                    Text("Send Alert")
                }
                Spacer(modifier = Modifier.weight(1f))


            }
        }

    }


}

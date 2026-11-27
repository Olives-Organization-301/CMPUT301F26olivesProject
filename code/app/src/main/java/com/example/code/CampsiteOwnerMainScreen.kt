package com.example.code

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.DialogProperties
import com.example.code.classes.Alert
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.ChatMessage
import com.example.code.classes.Coordinates
import com.example.code.classes.PreviewCampsite
import com.example.code.classes.WaitlistEntrant
import kotlinx.coroutines.delay
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

data class OwnerThemeColors(
    val gradient: List<Color>,
    val title: Color,
    val subtitle: Color,
    val footer: Color,
    val primaryButton: Color,
    val primaryText: Color,
    val secondaryButton: Color,
    val secondaryText: Color
)

private val OwnerLightColors = OwnerThemeColors(
    gradient = listOf(Color(0xFFFFF8EE), Color(0xFFFDE8CB)),
    title = Color(0xFF2B1B17),
    subtitle = Color(0xFF7A6A62),
    footer = Color(0xFF1A1A1A),
    primaryButton = Color(0xFF45238A),
    primaryText = Color.White,
    secondaryButton = Color(0xFF6B49B0),
    secondaryText = Color.White
)

private val OwnerDarkColors = OwnerThemeColors(
    gradient = listOf(Color(0xFF101828), Color(0xFF1F2B45)),
    title = Color.White,
    subtitle = Color(0xFF9AA5BD),
    footer = Color.White,
    primaryButton = Color(0xFFFFB84D),
    primaryText = Color(0xFF1A1A1A),
    secondaryButton = Color(0xFFFFE7BF),
    secondaryText = Color(0xFF1A1A1A)
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteOwnerMainScreen(
    navController: NavController,
    appState: AppState,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
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
    var showReservations by remember { mutableStateOf<PreviewCampsite?>(null) }
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val colors = if (darkTheme) OwnerDarkColors else OwnerLightColors

    val previewCampsites = remember {
        mutableStateListOf<PreviewCampsite>()
    }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

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
                                                campsiteID = c.campsiteID,
                                                description = c.getDescription(),
                                                maxOccupancy = c.getPeakOccupancy(),
                                                tags = c.getTags(),
                                                latitude = c.getMainCoordinates()?.getLatitude(),
                                                longitude = c.getMainCoordinates()?.getLongitude()
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
                                                campsiteID = c.campsiteID,
                                                description = c.getDescription(),
                                                maxOccupancy = c.getPeakOccupancy(),
                                                tags = c.getTags(),
                                                latitude = c.getMainCoordinates()?.getLatitude(),
                                                longitude = c.getMainCoordinates()?.getLongitude()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(colors.gradient))
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.weight(1f)) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Signed in owner verification banner
                    Text(
                        text = "Signed in as: $ownerName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.title,
                        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                    )

                    // Search Bar
                    TextField(
                        value = searchString,
                        onValueChange = { searchString = it },
                        placeholder = { Text("Search for your campsite", color = colors.subtitle) },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.title.copy(alpha = 0.06f),
                            unfocusedContainerColor = colors.title.copy(alpha = 0.04f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = colors.title,
                            unfocusedTextColor = colors.title
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // The Cards of Campsites
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(filteredCampsites) { preview ->
                            val matchedCampsite = appState.campsites.campsites.firstOrNull {
                                it.campsiteID == preview.campsiteID
                            }
                            val thumbnail = matchedCampsite.thumbnailPhoto() ?: preview.campsiteImage.takeIf { it.isNotBlank() }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(135.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFB0B0B0))
                            ) {
                                CampsiteImageBackground(
                                    image = thumbnail,
                                    defaultImage = R.drawable.campsite_thumbnail,
                                    fallback = Color(0xFFB0B0B0),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    CompositionLocalProvider(
                                        LocalContentColor provides Color.White
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 20.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(preview.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                                if (preview.location.isNotBlank()) {
                                                    Text(preview.location, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                                                }
                                                Text("Entry: " + (preview.startMillis?.let { format.format(Date(it)) } ?: "Not set"), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                                Text("End: " + (preview.endMillis?.let { format.format(Date(it)) } ?: "Not set"), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            var showMenu by remember { mutableStateOf(false) }

                                            Button(
                                                onClick = { showMenu = true },
                                                shape = RoundedCornerShape(20.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryButton, contentColor = colors.primaryText),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text("Menu", maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (showMenu) {
                                                AlertDialog(
                                                    onDismissRequest = { showMenu = false },
                                                    title = { Text(preview.name) },
                                                    confirmButton = {
                                                        TextButton(onClick = { showMenu = false }) {
                                                            Text("Close")
                                                        }
                                                    },
                                                    text = {
                                                        Column(Modifier.verticalScroll(rememberScrollState())) {
                                                            TextButton(onClick = {
                                                                showMenu = false
                                                                showEdit = preview
                                                            }) { Text("Edit") }
                                                            TextButton(onClick = {
                                                                showMenu = false
                                                                showLottery = preview
                                                            }) { Text("Lottery") }
                                                            TextButton(onClick = {
                                                                showMenu = false
                                                                showAlert = preview
                                                            }) { Text("Alert") }
                                                            TextButton(onClick = {
                                                                showMenu = false
                                                                showReservations = preview
                                                            }) { Text("Reservations") }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = colors.primaryButton,
                    contentColor = colors.primaryText,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) { Icon(Icons.Filled.Add, contentDescription = "Add campsite") }
            }

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.title.copy(alpha = 0.05f))
                    .navigationBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryButton.copy(alpha = 0.4f), contentColor = colors.title),
                    modifier = Modifier.weight(1f)
                ) { Text("List", maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { showQRCodeDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryButton.copy(alpha = 0.4f), contentColor = colors.title),
                    modifier = Modifier.weight(1f)
                ) { Text("QR Code", maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { showAccountDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryButton.copy(alpha = 0.4f), contentColor = colors.title),
                    modifier = Modifier.weight(1f)
                ) { Text("Account", maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { navController.navigate("campsiteChat") },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryButton.copy(alpha = 0.4f), contentColor = colors.title),
                    modifier = Modifier.weight(1f)
                ) { Text("Messages", maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
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
                        data = campsite.campsiteID.ifBlank { campsite.name },
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
                        val matchedCampsite = appState.campsites.campsites.firstOrNull {
                            it.campsiteID == campsite.campsiteID
                        }
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
                    val realCampsite = Campsite(
                        name = newCampsite.name,
                        ownerID = appState.userID ?: "unknown_owner",
                        campsiteID = uniqueId,
                        maxOccupancy = mutableListOf(4),
                        price = 50.0f,
                        rating = 5.0f,
                        coordinates = mutableListOf(Coordinates(
                            country = "Canada",
                            region = "",
                            city = newCampsite.location,
                            address = newCampsite.location,
                            latitude = newCampsite.latitude,
                            longitude = newCampsite.longitude
                        )),
                        detailsImage = newCampsite.campsiteImage.ifBlank { null },
                        thumbnailImage = newCampsite.campsiteImage.ifBlank { null },
                        lotteryStartDate = LocalDateTime.now(),
                        lotteryEndDate = LocalDateTime.now().plusDays(7)
                    )
                    realCampsite.setDescription(newCampsite.description)
                    realCampsite.setTags(newCampsite.tags)
                    realCampsite.setMaxOccupancy(listOf(newCampsite.maxOccupancy))
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
                                    campsiteID = uniqueId,
                                    description = newCampsite.description,
                                    maxOccupancy = newCampsite.maxOccupancy,
                                    tags = newCampsite.tags,
                                    latitude = newCampsite.latitude,
                                    longitude = newCampsite.longitude
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
                appState.campsites.updateCampsiteDetails(
                    current.campsiteID,
                    updated.name,
                    updated.location,
                    updated.description,
                    updated.maxOccupancy,
                    updated.latitude,
                    updated.longitude,
                    updated.tags
                ).addOnSuccessListener {
                    val changedImage = updated.campsiteImage.takeIf { it != current.campsiteImage }
                    appState.campsites.updateCampsiteListing(current.campsiteID, updated.name, changedImage)
                        .addOnSuccessListener {
                            val index = previewCampsites.indexOfFirst { it.campsiteID == current.campsiteID }
                            if (index >= 0) previewCampsites[index] = updated
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Could not save campsite image", Toast.LENGTH_LONG).show()
                        }
                }.addOnFailureListener {
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
            onDismiss = { showAlert = null },
            ownerName = ownerName,
            appState = appState
        )
    }

    showReservations?.let { current ->
        val siteMessages = appState.chat.messages.filter {
            it.campsiteID == current.campsiteID && it.ownerID == appState.userID
        }
        val decisions = ownerReservationDecisions(siteMessages)
        val entrantsById = appState.entrants.entrants.associateBy { it.getUserID() }
        val confirmed = decisions.filter { it.type == RESERVATION_CONFIRMED }
        val declined = decisions.filter { it.type == RESERVATION_DECLINED }
        AlertDialog(
            onDismissRequest = { showReservations = null },
            title = { Text("Reservations: ${current.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Booked dates: ${ownerBookedDates(siteMessages).joinToString().ifBlank { "None" }}")
                    Text("Active reservations", fontWeight = FontWeight.Bold)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 170.dp)) {
                        items(confirmed) { decision ->
                            val entrant = entrantsById[decision.entrantID]
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(entrant?.name?.ifBlank { entrant.username } ?: decision.entrantID)
                                Text("Location ${decision.locationIndex + 1}: ${decision.stayDates.joinToString()}")
                                TextButton(onClick = {
                                    val now = System.currentTimeMillis()
                                    appState.chat.addMessage(ChatMessage(
                                        campsiteID = current.campsiteID, campsiteName = current.name,
                                        sender = ownerName, timestamp = Date(now).toString(),
                                        text = "The owner rejected your reservation for Location ${decision.locationIndex + 1}.",
                                        fromOwner = true, entrantID = decision.entrantID,
                                        ownerID = appState.userID.orEmpty(), automated = true,
                                        type = RESERVATION_DECLINED, locationIndex = decision.locationIndex, sentAt = now
                                    ))
                                    entrantsById[decision.entrantID]?.let { entrant ->
                                        val hasOtherConfirmed = ownerReservationDecisions(
                                            appState.chat.conversation(current.campsiteID, decision.entrantID) +
                                                appState.chat.messages.filter {
                                                    it.campsiteID == current.campsiteID && it.entrantID == decision.entrantID
                                                }
                                        ).any { it.type == RESERVATION_CONFIRMED && it.locationIndex != decision.locationIndex }
                                        if (!hasOtherConfirmed) appState.entrants.removeEntrantCampsite(
                                            entrant, RESERVED_CAMPSITES, current.campsiteID
                                        )
                                    }
                                }) { Text("Reject reservation") }
                            }
                        }
                    }
                    Text("Cancelled reservations", fontWeight = FontWeight.Bold)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                        items(declined) { decision ->
                            Text("${entrantsById[decision.entrantID]?.name ?: decision.entrantID} · Location ${decision.locationIndex + 1}")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showReservations = null }) { Text("Close") } }
        )
    }
}

/** Popup form for campsite owners to add or edit campsite details.
 *  Captures name, location, description and amenities, maximum occupancy, map coordinates,
 *  optional image URL, tags, booking dates, registration dates, and rules and policies.
 *
 *  Prompt: "US 02.01.02 As a campsite owner, I want to provide campsite details including
 *  name, location, description (amenities), maximum occupancy, map location, optional image, and tags."
 *  Implemented with Android Studio AI Agent.
 */
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
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var occupancy by remember { mutableStateOf(initial?.maxOccupancy?.toString() ?: "4") }
    var tagsText by remember { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var latitude by remember { mutableStateOf(initial?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(initial?.longitude?.toString() ?: "") }

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
                                    campsiteID = initial?.campsiteID.orEmpty(),
                                    description = description.trim(),
                                    maxOccupancy = occupancy.toIntOrNull() ?: 4,
                                    tags = tagsText.split(",").map(String::trim).filter(String::isNotEmpty),
                                    latitude = latitude.toDoubleOrNull(),
                                    longitude = longitude.toDoubleOrNull()
                                )
                            )
                        },
                        enabled = name.isNotBlank() &&
                            occupancy.toIntOrNull()?.let { it > 0 } == true &&
                            ((latitude.isBlank() && longitude.isBlank()) ||
                                (latitude.toDoubleOrNull()?.let { lat ->
                                    longitude.toDoubleOrNull()?.let { lon -> lat in -90.0..90.0 && lon in -180.0..180.0 }
                                } == true)) &&
                            !processingPhoto
                    ) {
                        Text("Save")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
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

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description and amenities") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    OutlinedTextField(
                        value = occupancy,
                        onValueChange = { occupancy = it.filter(Char::isDigit) },
                        label = { Text("Maximum occupancy") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Map latitude (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Map longitude (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        label = { Text("Tags (comma separated)") },
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickDate(context, null) { start = it } }) {
                                Text("Start date", fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { pickDate(context, start) { end = it } }) {
                                Text("End date", fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Waiting list (${entrants.size} entrants)", fontWeight = FontWeight.Bold)
                }
                items(entrants, key = { it }) { entrantID ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entrantNames[entrantID]?.ifBlank { null } ?: entrantID, fontSize = 14.sp)
                        if (drawn) {
                            val locations = liveCampsite?.getLocationsWon(entrantID).orEmpty()
                            Text(
                                if (locations.isEmpty()) "Not selected"
                                else locations.joinToString { "Location ${it + 1}" },
                                fontSize = 13.sp,
                                color = if (locations.isNotEmpty()) Color(0xFF2E7D32) else Color.Gray
                            )
                        }
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
    var showSentAlerts by remember { mutableStateOf(false) }
    var sentAlerts by remember { mutableStateOf<List<Alert>>(emptyList()) }

    fun CreateAlert(campsite: PreviewCampsite, owner: String, title: String, body: String){
        for(entrant in campsite.waitingList){
            val alertID = UUID.randomUUID()
            val newAlert = Alert(alertID.toString(), title, body, entrant.id, owner)
            appState.alerts.addAlert(newAlert)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send alert to entrants of ${campsite.name}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = {
                    appState.alerts.fetchSentBy(ownerName) { sentAlerts = it }
                    showSentAlerts = true
                }) { Text("View sent notifications") }
                OutlinedTextField(
                    value = alertTitle,
                    onValueChange = { alertTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = alertBody,
                    onValueChange = { alertBody = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    CreateAlert(campsite, ownerName, alertTitle, alertBody)
                    alertTitle = ""
                    alertBody = ""
                    onDismiss()
                }
            ) {
                Text("Send Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showSentAlerts) {
        AlertDialog(
            onDismissRequest = { showSentAlerts = false },
            title = { Text("Notifications sent") },
            text = {
                if (sentAlerts.isEmpty()) Text("No notifications have been sent.")
                else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(sentAlerts) { alert ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(alert.title, fontWeight = FontWeight.Bold)
                            Text(alert.body)
                            Text("To: ${alert.recipient}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSentAlerts = false }) { Text("Close") }
            }
        )
    }
}

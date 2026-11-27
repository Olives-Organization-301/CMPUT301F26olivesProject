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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.key
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.time.Instant
import java.time.LocalDate
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
    var newCampsiteRules by remember(appState.userID) { mutableStateOf<List<String>?>(null) }
    var loadingDefaultRules by remember(appState.userID) { mutableStateOf(false) }
    var creatingCampsite by remember(appState.userID) { mutableStateOf(false) }
    var showQRCodeDialog by remember { mutableStateOf(false) }
    var selectedQRCampsite by remember { mutableStateOf<PreviewCampsite?>(null) }
    var ownerName by remember { mutableStateOf("Campsite Owner") }
    var currentOwnerObj by remember { mutableStateOf<CampsiteOwner?>(null) }
    var showEdit by remember { mutableStateOf<PreviewCampsite?>(null) }
    var showLottery by remember { mutableStateOf<PreviewCampsite?>(null) }
    var savingLottery by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf<PreviewCampsite?>(null) }
    var showReservations by remember { mutableStateOf<PreviewCampsite?>(null) }
    var showReservationList by remember(appState.userID) { mutableStateOf(false) }
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val colors = if (darkTheme) OwnerDarkColors else OwnerLightColors

    val previewCampsites = remember {
        mutableStateListOf<PreviewCampsite>()
    }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

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
                                                longitude = c.getMainCoordinates()?.getLongitude(),
                                                rules = c.getRules(),
                                                waitlistCapacity = c.waitlistCapacity,
                                                sharedLotteryID = c.sharedLotteryID,
                                                stayStartMillis = c.getVacationStartTimes().singleOrNull()?.first?.pickerMillis(),
                                                stayEndMillis = c.getVacationStartTimes().singleOrNull()?.second?.pickerMillis()
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
                                                longitude = c.getMainCoordinates()?.getLongitude(),
                                                rules = c.getRules(),
                                                waitlistCapacity = c.waitlistCapacity,
                                                sharedLotteryID = c.sharedLotteryID,
                                                stayStartMillis = c.getVacationStartTimes().singleOrNull()?.first?.pickerMillis(),
                                                stayEndMillis = c.getVacationStartTimes().singleOrNull()?.second?.pickerMillis()
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
                            val shared = matchedCampsite?.sharedLotteryID?.let { appState.campsites.sharedLotteries.find(it) }
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
                                                Text("Entry: " + ((shared?.opens?.pickerMillis() ?: preview.startMillis)?.let { format.format(Date(it)) } ?: "Not set"), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                                Text("End: " + ((shared?.closes?.pickerMillis() ?: preview.endMillis)?.let { format.format(Date(it)) } ?: "Not set"), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
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

                                            // Codex prompts: "for when menu is clicked on a campsite for campsite owner, i can't scroll down fix that"
                                            // "check the stash changes, there was a menu re-work so apply the logic of the change on that"
                                            // "remove the older menu code"
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
                    onClick = {
                        val ownerID = appState.userID
                        if (ownerID != null && !loadingDefaultRules && newCampsiteRules == null) {
                            loadingDefaultRules = true
                            appState.owners.getDefaultCampsiteRules(ownerID)
                                .addOnCompleteListener { result ->
                                    loadingDefaultRules = false
                                    if (appState.userID == ownerID) {
                                        if (result.isSuccessful) {
                                            newCampsiteRules = result.result
                                        } else {
                                            Toast.makeText(context, "Could not load default rules and policies", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                        }
                    },
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
                    onClick = { showReservationList = true },
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
                    // Codex prompt: "is it possible to have that version of account back"
                    onClick = { navController.navigate("campsiteOwnerProfileScreen") },
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

    newCampsiteRules?.let { defaultRules ->
        AddEditCampsite(
            title = "Add Campsite",
            initial = null,
            defaultRules = defaultRules,
            saving = creatingCampsite,
            onDismiss = { if (!creatingCampsite) newCampsiteRules = null },
            onSave = save@{ newCampsite ->
                val ownerID = appState.userID ?: return@save
                if (creatingCampsite) return@save
                creatingCampsite = true
                appState.campsites.generateUniqueCampsiteID { uniqueId ->
                    val opens = lotteryNow().withNano(0)
                    val closes = opens.plusDays(7)
                    val realCampsite = Campsite(
                        name = newCampsite.name,
                        ownerID = ownerID,
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
                        lotteryStartDate = opens,
                        lotteryEndDate = closes
                    )
                    realCampsite.setDescription(newCampsite.description)
                    realCampsite.setTags(newCampsite.tags)
                    realCampsite.setRules(newCampsite.rules)
                    newCampsite.stayRange()?.let { realCampsite.setVacationStartTimes(listOf(it)) }
                    realCampsite.setMaxOccupancy(listOf(newCampsite.maxOccupancy))
                    appState.campsites.addCampsite(realCampsite, saveRulesAsOwnerDefault = true)
                        .addOnSuccessListener {
                            if (appState.userID == ownerID) {
                                previewCampsites.add(newCampsite.copy(
                                    campsiteID = uniqueId, startMillis = opens.pickerMillis(), endMillis = closes.pickerMillis()
                                ))
                                newCampsiteRules = null
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Could not save campsite", Toast.LENGTH_LONG).show()
                        }
                        .addOnCompleteListener { creatingCampsite = false }
                }
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
                    updated.tags,
                    updated.rules,
                    updated.stayRange().takeIf {
                        updated.stayStartMillis != current.stayStartMillis || updated.stayEndMillis != current.stayEndMillis
                    }
                ).addOnSuccessListener {
                    val index = previewCampsites.indexOfFirst { it.campsiteID == current.campsiteID }
                    if (index >= 0) previewCampsites[index] = updated.copy(campsiteImage = current.campsiteImage)
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
        val live = appState.campsites.campsites.firstOrNull { it.campsiteID == current.campsiteID }
        val groupID = if (live == null) current.sharedLotteryID else live.sharedLotteryID
        val group = groupID?.let { appState.campsites.sharedLotteries.find(it) }
        val eligibleIDs = appState.campsites.campsites.filter {
            it.getOwnerID() == appState.userID && it.sharedLotteryID == null &&
                !it.isLotteryDrawn() && it.getWaitingList().isEmpty()
        }.map { it.campsiteID }.toSet()
        val options = previewCampsites.filter { it.campsiteID in eligibleIDs || it.campsiteID in group?.campsiteIDs.orEmpty() }
        LotteryDialog(
            campsite = current,
            liveCampsite = live,
            group = group,
            groupLoading = groupID != null && group == null,
            campsiteOptions = options,
            entrantNames = appState.entrants.entrants.associate { it.getUserID() to it.name.ifBlank { it.username } },
            saving = savingLottery,
            onDismiss = { if (!savingLottery) showLottery = null },
            onSave = save@{ startMillis, endMillis, capacity, selected, winnerCount ->
                if (savingLottery) return@save
                val ownerID = appState.userID
                val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val end = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                if (ownerID == null) return@save
                if (end.isBefore(start) || end.isBefore(lotteryNow().toLocalDate())) {
                    Toast.makeText(context, "Choose valid lottery dates", Toast.LENGTH_LONG).show()
                    return@save
                }
                savingLottery = true
                val savedStart = if (startMillis == (live?.getStartDate()?.pickerMillis() ?: current.startMillis)) {
                    live?.getStartDate() ?: Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
                } else start.atStartOfDay()
                val savedEnd = if (endMillis == (live?.getEndDate()?.pickerMillis() ?: current.endMillis)) {
                    live?.getEndDate() ?: Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
                } else end.atTime(23, 59, 59)
                val saveTask = if (group != null || selected.size > 1) {
                    appState.campsites.sharedLotteries.save(
                        groupID, ownerID, selected,
                        if (group?.opens?.pickerMillis() == startMillis) group.opens else start.atStartOfDay(),
                        if (group?.closes?.pickerMillis() == endMillis) group.closes else end.atTime(23, 59, 59),
                        capacity, winnerCount
                    )
                } else {
                    appState.campsites.updateLotteryListing(current.campsiteID, ownerID, savedStart, savedEnd, capacity)
                }
                saveTask.addOnCompleteListener { result ->
                    savingLottery = false
                    if (result.isSuccessful && result.result == true) {
                        val index = previewCampsites.indexOfFirst { it.campsiteID == current.campsiteID }
                        if (index >= 0) previewCampsites[index] = previewCampsites[index].copy(
                            startMillis = startMillis, endMillis = endMillis, waitlistCapacity = capacity
                        )
                        showLottery = null
                    } else {
                        Toast.makeText(context, "Could not save. Check the registration dates, campsite selection, and waitlist size.", Toast.LENGTH_LONG).show()
                    }
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

    val ownerID = appState.userID
    if (showReservationList && ownerID != null) {
        OwnerReservationListDialog(
            messages = appState.chat.messages,
            ownerID = ownerID,
            campsiteNames = previewCampsites.associate { it.campsiteID to it.name },
            entrantNames = appState.entrants.entrants.associate {
                it.getUserID() to it.name.ifBlank { it.username }
            },
            onDismiss = { showReservationList = false }
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
                                if (decision.rulesAcknowledged) Text("Campsite rules acknowledged")
                                TextButton(onClick = reject@{
                                    if (decision.lotteryID.isNotBlank()) {
                                        val ownerID = appState.userID ?: return@reject
                                        appState.campsites.sharedLotteries.reject(decision, ownerID)
                                            .addOnCompleteListener {
                                                if (!it.isSuccessful || it.result != true) {
                                                    Toast.makeText(context, "Could not reject reservation", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        return@reject
                                    }
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

/** Codex prompt: "so for campsite owner, when list is clicked, I want there to be options for past reservations and active reservations" */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun OwnerReservationListDialog(
    messages: List<ChatMessage>,
    ownerID: String,
    campsiteNames: Map<String, String>,
    entrantNames: Map<String, String>,
    onDismiss: () -> Unit,
) {
    var past by rememberSaveable { mutableStateOf(false) }
    val reservations = ownerReservationList(messages, ownerID, LocalDate.now(), past)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reservations") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !past,
                    onClick = { past = false },
                    label = { Text("Active reservations") }
                )
                FilterChip(
                    selected = past,
                    onClick = { past = true },
                    label = { Text("Past reservations") }
                )
                if (reservations.isEmpty()) {
                    Text(if (past) "No past reservations" else "No active reservations")
                } else {
                    key(past) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(reservations) { reservation ->
                                Column {
                                    Text(
                                        campsiteNames[reservation.campsiteID]
                                            ?: reservation.campsiteName.ifBlank { "Campsite unavailable" },
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(entrantNames[reservation.entrantID]?.ifBlank { null } ?: reservation.entrantID)
                                    Text("Location ${reservation.locationIndex + 1}")
                                    Text(reservation.reservationDates().distinct().sorted().joinToString()
                                        .ifBlank { "Booked stay dates unavailable" })
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/** Popup form for campsite owners to add or edit campsite details.
 *  Captures name, location, description and amenities, maximum occupancy, map coordinates,
 *  optional image URL, tags, booking dates, registration dates, and rules and policies.
 *
 *  A new listing starts with the owner's [defaultRules] and requires nonblank rules.
 *  An existing listing starts with its own published rules. Closing the rules editor
 *  keeps the draft in this form. Saving the campsite publishes the nonblank lines.
 *
 *  Codex prompt: "for campsite being added in campsite owner let there be an option at the
 *  bottom that requires to be filled and it be about rules and policies, can be exited but its
 *  automatically saved, and then if there is content inside let there be an edit option,
 *  when campsite is made, for that certain owner let there rules and policies be the default
 *  for a new campsite that can be edited"
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
    defaultRules: List<String> = emptyList(),
    saving: Boolean = false,
    onSave: (PreviewCampsite) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var stayStartMillis by remember { mutableStateOf(initial?.stayStartMillis) }
    var stayEndMillis by remember { mutableStateOf(initial?.stayEndMillis) }
    var campsiteImage by remember { mutableStateOf(initial?.campsiteImage ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var occupancy by remember { mutableStateOf(initial?.maxOccupancy?.toString() ?: "4") }
    var tagsText by remember { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var latitude by remember { mutableStateOf(initial?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(initial?.longitude?.toString() ?: "") }
    var rulesText by rememberSaveable(initial?.campsiteID) {
        mutableStateOf((initial?.rules ?: defaultRules).joinToString("\n"))
    }
    var showRulesEditor by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val (processingPhoto, takePhoto, openPhotoPicker) = rememberPhotoPicker { campsiteImage = it }

    val startText = stayStartMillis?.let { format.format(Date(it)) } ?: "Not set"
    val endText = stayEndMillis?.let { format.format(Date(it)) } ?: "Not set"

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
                                    startMillis = initial?.startMillis,
                                    endMillis = initial?.endMillis,
                                    startMillisRegis = null,
                                    endMillisRegis = null,
                                    waitingList = initial?.waitingList ?: emptyList(),
                                    campsiteImage = campsiteImage,
                                    campsiteID = initial?.campsiteID.orEmpty(),
                                    description = description.trim(),
                                    maxOccupancy = occupancy.toIntOrNull() ?: 4,
                                    tags = tagsText.split(",").map(String::trim).filter(String::isNotEmpty),
                                    latitude = latitude.toDoubleOrNull(),
                                    longitude = longitude.toDoubleOrNull(),
                                    rules = rulesText.lineSequence().map(String::trim).filter(String::isNotEmpty).toList(),
                                    waitlistCapacity = initial?.waitlistCapacity,
                                    sharedLotteryID = initial?.sharedLotteryID,
                                    stayStartMillis = stayStartMillis,
                                    stayEndMillis = stayEndMillis
                                )
                            )
                        },
                        enabled = !saving && name.isNotBlank() &&
                            ((stayStartMillis == null && stayEndMillis == null) ||
                                (stayStartMillis != null && stayEndMillis != null &&
                                    !stayEndMillis!!.pickerDate().isBefore(stayStartMillis!!.pickerDate()))) &&
                            (initial != null || rulesText.isNotBlank()) &&
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

                    Text("Available stay dates")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickDate(context, null) { stayStartMillis = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Entry: $startText", maxLines = 1) }

                        OutlinedButton(
                            onClick = { pickDate(context, stayStartMillis) { stayEndMillis = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("End: $endText", maxLines = 1) }
                    }

                    Text(
                        if (initial == null) "Rules and policies (required)" else "Rules and policies",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedButton(
                        enabled = !saving,
                        onClick = { showRulesEditor = true }
                    ) {
                        Text(if (rulesText.isBlank()) "Add rules and policies" else "Edit rules and policies")
                    }
                }
            }
        }
    }

    if (showRulesEditor) {
        AlertDialog(
            onDismissRequest = { showRulesEditor = false },
            title = { Text("Rules and policies") },
            text = {
                OutlinedTextField(
                    value = rulesText,
                    onValueChange = { rulesText = it },
                    label = { Text("Campsite rules and policies") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(onClick = { showRulesEditor = false }) { Text("Done") }
            }
        )
    }
}

/**
 * US 02.04.06 and 02.04.07. Save dates and an optional waitlist cap from one editor.
 * Codex prompts: "work on user stories 2.04.07, 2.04.06, 2.05.01, how would the idea be implemented first"
 * and "work on it then, and make the test cases, and then document the code with the necessary prompts"
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun LotteryDialog(
    campsite: PreviewCampsite,
    liveCampsite: Campsite?,
    group: SharedLottery?,
    groupLoading: Boolean,
    campsiteOptions: List<PreviewCampsite>,
    entrantNames: Map<String, String>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Int?, List<String>, Int) -> Unit,
) {
    val context = LocalContext.current
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    var start by remember(campsite.campsiteID, group?.id) {
        mutableStateOf(group?.opens?.pickerMillis() ?: liveCampsite?.getStartDate()?.pickerMillis() ?: campsite.startMillis)
    }
    var end by remember(campsite.campsiteID, group?.id) {
        mutableStateOf(group?.closes?.pickerMillis() ?: liveCampsite?.getEndDate()?.pickerMillis() ?: campsite.endMillis)
    }
    val drawn = group?.drawn ?: (liveCampsite?.isLotteryDrawn() == true)
    val entrants = group?.entrants ?: liveCampsite?.getWaitingList().orEmpty()
    val currentCapacity = when {
        group != null -> group.capacity
        liveCampsite != null -> liveCampsite.waitlistCapacity
        else -> campsite.waitlistCapacity
    }
    var capacityText by rememberSaveable(campsite.campsiteID, group?.id) { mutableStateOf(currentCapacity?.toString().orEmpty()) }
    val anchorID = group?.anchorID ?: campsite.campsiteID
    var selected by remember(campsite.campsiteID, group?.id) {
        mutableStateOf(group?.campsiteIDs ?: listOf(anchorID))
    }
    val shared = group != null || selected.size > 1
    var winnersText by rememberSaveable(campsite.campsiteID, group?.id) { mutableStateOf((group?.winnerCount ?: 5).toString()) }
    val winners = winnersText.trim().toIntOrNull()
    val capacity = capacityText.trim().toIntOrNull()
    val capacityValid = capacityText.isBlank() || capacity != null && capacity > 0 && capacity >= entrants.size
    val now by registrationClock()
    val openingLocked = if (group != null) !now.isBefore(group.opens) else entrants.isNotEmpty()
    val editable = !groupLoading && !drawn && (group?.closes ?: liveCampsite?.getEndDate())?.let { now.isBefore(it) } != false
    val memberSelectionEnabled = editable && !openingLocked && !saving &&
        (group != null || campsiteOptions.any { it.campsiteID == anchorID })
    val selectionChanged = selected.toSet() != group?.campsiteIDs?.toSet()
    val membershipValid = !shared || selected.size >= 2 &&
        (!selectionChanged || start?.pickerDate()?.atStartOfDay()?.let { now.isBefore(it) } == true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lottery: ${campsite.name}") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Registration opens: " + (start?.let { format.format(Date(it)) } ?: "Not set"))
                    Text("Registration closes: " + (end?.let { format.format(Date(it)) } ?: "Not set"))
                    if (groupLoading) Text("Loading shared lottery…")
                    if (editable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = !openingLocked && !saving, onClick = { pickDate(context, null) { start = it } }) {
                                Text("Set opening", fontSize = 12.sp)
                            }
                            OutlinedButton(enabled = !saving, onClick = { pickDate(context, start) { end = it } }) {
                                Text("Set closing", fontSize = 12.sp)
                            }
                        }
                        OutlinedTextField(
                            value = capacityText,
                            onValueChange = { capacityText = it },
                            label = { Text("Waitlist capacity") },
                            supportingText = {
                                Text(if (capacityValid) "Leave blank for unlimited" else "Enter a positive limit of at least ${entrants.size}")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !saving
                        )
                    }
                }
                if (shared || memberSelectionEnabled && campsiteOptions.size > 1) {
                    item { Text("Campsites in this lottery", fontWeight = FontWeight.Bold) }
                    items(campsiteOptions.filter { memberSelectionEnabled || it.campsiteID in selected }, key = { "site-${it.campsiteID}" }) { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = option.campsiteID in selected,
                                modifier = Modifier.testTag("lotterySite_${option.campsiteID}"),
                                enabled = memberSelectionEnabled && option.campsiteID != anchorID,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + option.campsiteID else selected - option.campsiteID
                                }
                            )
                            Text(option.name)
                        }
                    }
                }
                if (shared && editable) {
                    item {
                        if (!membershipValid) Text("Select at least two campsites and an opening date in the future to change the campsite selection.")
                        OutlinedTextField(
                            value = winnersText,
                            onValueChange = { winnersText = it },
                            label = { Text("Number of winners") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !saving,
                            singleLine = true
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    val count = currentCapacity?.let { "${entrants.size}/$it" } ?: "${entrants.size}"
                    Text("Waiting list ($count entrants)", fontWeight = FontWeight.Bold)
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
                                if (group != null) {
                                    if (entrantID in group.winners) "Selected" else "Not selected"
                                } else if (locations.isEmpty()) "Not selected"
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
            if (editable) {
                val selectedStart = start
                val selectedEnd = end
                TextButton(
                    enabled = !saving && capacityValid && membershipValid &&
                        (!shared || (winners ?: 0) > 0) && selectedStart != null && selectedEnd != null,
                    onClick = {
                        if (selectedStart != null && selectedEnd != null) {
                            onSave(selectedStart, selectedEnd, capacity, selected, winners ?: 5)
                        }
                    }
                ) { Text(if (saving) "Saving…" else "Save lottery") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun LocalDateTime.pickerMillis() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
private fun Long.pickerDate() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun PreviewCampsite.stayRange(): Pair<LocalDateTime, LocalDateTime>? {
    val start = stayStartMillis?.pickerDate() ?: return null
    val end = stayEndMillis?.pickerDate() ?: return null
    return start.atStartOfDay() to end.atTime(23, 59, 59)
}

/**
 * Codex prompt: "just have one version of the UI, the one that has sign up and login and each
 * of entrant, owner and admin, and include the QR bitmap helper again"
 */
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

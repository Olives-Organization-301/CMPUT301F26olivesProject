package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.Entrant
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.staticCompositionLocalOf
import android.graphics.Bitmap
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

/** Main Profile Screen
 * I Used Sonnet 5.0 in order to finish the GUI and then integrate the backend
 * Prompt: "Match The screenshot with the GUI"
 * Conversation: https://claude.ai/share/b857df9c-812a-4adb-b696-38a5266149f7
 * Prompt: "Can you integrate the profile with the backend?"
 * Conversation: https://claude.ai/share/b0990579-fe55-4946-828d-1830efc58118
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProfileScreen(
    navController: NavController,
    appState: AppState
) {
    val context = LocalContext.current

    val entrant by produceState<Entrant?>(
        initialValue = null,
        key1 = appState.userID
    ) {
        val id = appState.userID
        if (id == null) {
            value = null
        } else {
            appState.entrants.searchEntrants(id) { found ->
                value = found
            }
        }
    }

    val recentCampsites = entrant?.getCampsiteLists()?.get("Past_Campsites") ?: emptyList()
    var showEditDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }
    val tick = refreshTick   // read so the screen recomposes after a save

    val outline = MaterialTheme.colorScheme.onSurface

    // Organize Entire UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {

        // To avoid clipping it into the top of the screen
        Spacer(Modifier.height(48.dp))

        // Profile Basic Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Profile Picture (blank circle if the entrant has none, see ProfilePicture below)
            ProfilePicture(
                profileImage = entrant?.profileImage.orEmpty(),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(entrant?.name?.ifBlank { "No name set" } ?: "Loading…")
                Text("@${entrant?.username ?: ""}")
                Text(entrant?.dateOfBirth?.ifBlank { "No date of birth" } ?: "")
                Text(entrant?.email ?: "")
                Text(entrant?.phoneNumber?.ifBlank { "No phone number" } ?: "")
            }

            // Scan QR Code Box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(3.dp, outline, CircleShape)
                    .clickable { showQrDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text("QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(Modifier.width(8.dp))

            // Cancel Box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(3.dp, outline, CircleShape)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("About Me", fontWeight = FontWeight.Bold)
        Text(entrant?.description?.ifBlank { "No description yet." } ?: "")

        Spacer(Modifier.height(24.dp))

        Text("Recent Campsites", fontSize = 22.sp, fontWeight = FontWeight.Medium)

        // Previous Campsites
        // weight(1f) lets the list take the leftover space so the buttons stay pinned at the bottom
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(recentCampsites) { campsite ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            appState.viewingCampsiteID = campsite
                            navController.navigate("campsiteDetails")
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image of Campsite
                    // Placeholder box; replace with Image(painterResource(...)) or AsyncImage(...)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Text(
                        text = campsite,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )

                    // Details icon ("click me for more info!"), emoji so no dependencies
                    Text("ℹ️", fontSize = 18.sp)

                    // Chevron
                    Text(
                        text = "›",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showPolicyDialog = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text("Policy", fontSize = 12.sp, maxLines = 1) }

            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text("Edit Profile", fontSize = 12.sp, maxLines = 1) }

            Button(
                onClick = {
                    appState.userID = null
                    appState.role = null
                    DeviceIdentity.clearEntrantId(context)
                    navController.navigate("loginScreen") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text("Log Out", fontSize = 12.sp, maxLines = 1, color = Color.White) }
        }
    }

    // Edit User popup (Delete Profile lives here now)
    val current = entrant
    if (showEditDialog && current != null) {
        EditProfileDialog(
            entrant = current,
            onDismiss = { showEditDialog = false },
            onSave = { name, username, dob, email, phone, description, profileImage, wantsNotifications ->
                current.name = name
                current.username = username
                current.dateOfBirth = dob
                current.email = email
                current.phoneNumber = phone
                current.description = description
                if (profileImage != current.profileImage) {
                    if (profileImage.isBlank()) {
                        deleteEntrantImagesFromFirestore(current.getUserID())
                    } else {
                        saveImageRecordToFirestore(context, current.getUserID(), username, email, profileImage)
                    }
                }
                current.profileImage = profileImage
                current.wantsNotifications = wantsNotifications
                appState.entrants.updateEntrant(current)
                // If notifications were just turned on, show any lottery results waiting
                if (wantsNotifications) notifyLotteryResults(context, appState)
                refreshTick++
                showEditDialog = false
            },
            onDeleteProfile = {
                val id = current.getUserID()
                deleteEntrantImagesFromFirestore(id)
                // Take the entrant off every campsite's waiting list before deleting the profile
                appState.campsites.removeEntrantFromAllCampsites(id)
                appState.entrants.deleteEntrant(current)
                showEditDialog = false
                appState.userID = null
                appState.role = null
                // Forget this ID on the device so it's never reused for another account
                DeviceIdentity.clearEntrantId(context)
                navController.navigate("loginScreen") {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    if (showPolicyDialog) {
        PolicyDialog(onDismiss = { showPolicyDialog = false })
    }

    // Scan a campsite's QR code, then open that campsite's details
    if (showQrDialog) {
        QrCodeScanDialog(
            appState = appState,
            onCampsiteFound = { campsiteID ->
                showQrDialog = false
                appState.viewingCampsiteID = campsiteID
                navController.navigate("campsiteDetails")
            },
            onDismiss = { showQrDialog = false }
        )
    }
}

/** Use Policy Page in order to see the policy from the profile
 * I Used Sonnet 5.0 in Order to make a policy dialogue
 * Prompt: "Can you make a separate policy dialogue?"
 * Conversation: https://claude.ai/share/b0990579-fe55-4946-828d-1830efc58118
 */
@Composable
fun PolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Policy") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PolicySection(
                    title = "1. Accounts",
                    body = "You must provide accurate information when creating your profile. " +
                            "You are responsible for all activity under your account and for " +
                            "keeping your login details private."
                )
                PolicySection(
                    title = "2. Reservations",
                    body = "Campsite reservations are held for you once confirmed. If you cannot " +
                            "attend, cancel at least 48 hours in advance so the spot can be " +
                            "offered to someone on the waiting list. Repeated no-shows may " +
                            "limit your ability to book."
                )
                PolicySection(
                    title = "3. Respect the Campsite",
                    body = "Leave no trace: pack out all garbage, follow posted fire restrictions, " +
                            "and keep noise down between 10 PM and 7 AM. Damage to campsite " +
                            "property may result in fees or account suspension."
                )
                PolicySection(
                    title = "4. Conduct",
                    body = "Harassment, discrimination, or unsafe behaviour toward other campers " +
                            "or staff is not tolerated and may result in removal from the platform."
                )
                PolicySection(
                    title = "5. Your Data",
                    body = "We store your name, contact details, and campsite history to run the " +
                            "service. We do not sell your personal information. You can edit or " +
                            "delete your profile at any time from your profile page."
                )
                PolicySection(
                    title = "6. Changes",
                    body = "This policy may be updated from time to time. Continued use of the " +
                            "app means you accept the current version."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

/** Quick Formatting in order to format headers and text
 * I Used Sonnet 5.0 in Order to make a policy dialogue
 * Prompt: "Can you make a separate policy dialogue?"
 * Conversation: https://claude.ai/share/b0990579-fe55-4946-828d-1830efc58118
 */
@Composable
private fun PolicySection(title: String, body: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Use separate popup to Edit the File
 * I Used Sonnet 5.0 in Order to separate the filter settings
 * Prompt: "Can you make ?"
 * Conversation: https://claude.ai/share/b0990579-fe55-4946-828d-1830efc58118
 */
@Composable
fun EditProfileDialog(
    entrant: Entrant,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        username: String,
        dateOfBirth: String,
        email: String,
        phoneNumber: String,
        description: String,
        profileImage: String,
        wantsNotifications: Boolean
    ) -> Unit,
    onDeleteProfile: () -> Unit
) {
    // Draft state: edits only get committed when Save is pressed
    var name by remember { mutableStateOf(entrant.name) }
    var username by remember { mutableStateOf(entrant.username) }
    var dateOfBirth by remember { mutableStateOf(entrant.dateOfBirth) }
    var email by remember { mutableStateOf(entrant.email) }
    var phoneNumber by remember { mutableStateOf(entrant.phoneNumber) }
    var description by remember { mutableStateOf(entrant.description) }
    var profileImage by remember { mutableStateOf(entrant.profileImage) }
    var wantsNotifications by remember { mutableStateOf(entrant.wantsNotifications) }

    // Photo Picker: lets the user pick an image from their gallery/files (no permissions needed)
    val context = LocalContext.current

    // Whether the phone lets the app notify. The app only asks from here, never on its own.
    var canNotify by remember { mutableStateOf(hasNotificationPermission(context)) }
    var askedForPermission by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        canNotify = granted
        askedForPermission = true
    }
    // Opting in asks the phone for permission if it doesn't have it yet
    fun setWantsNotifications(wants: Boolean) {
        wantsNotifications = wants
        if (wants && !canNotify && !askedForPermission) {
            requestNotificationPermission(context, notificationPermission)
        }
    }
    val (processingPhoto, takePhoto, openPhotoPicker) = rememberPhotoPicker { profileImage = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Date of Birth") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("About Me") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Profile Picture Row
                Text("Profile Picture", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfilePicture(
                        profileImage = profileImage,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )

                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                        if (profileImage.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    profileImage = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Remove", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Selectable Preset Avatar Thumbnails
                Text("Preset Avatars (Select to Preview):", fontSize = 12.sp, color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Avatar 1: Default Profile
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (profileImage == TEST_PFP_PROFILE) 3.dp else 1.dp,
                                color = if (profileImage == TEST_PFP_PROFILE) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                profileImage = TEST_PFP_PROFILE
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.default_profile_image),
                            contentDescription = "Test Profile Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Avatar 2: Default Gift
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (profileImage == TEST_PFP_GIFT) 3.dp else 1.dp,
                                color = if (profileImage == TEST_PFP_GIFT) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                profileImage = TEST_PFP_GIFT
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.default_gift_image),
                            contentDescription = "Test Gift Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Notifications opt in / out (tapping the text toggles it too)
                Text("Notifications", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { setWantsNotifications(!wantsNotifications) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = wantsNotifications,
                        onCheckedChange = { setWantsNotifications(it) }
                    )
                    Text("Opt in to notifications")
                }
                // Opted in, but the phone is blocking notifications for this app
                if (wantsNotifications && !canNotify) {
                    Text(
                        "Notifications are turned off for this app on your phone.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = {
                        requestNotificationPermission(context, notificationPermission, askedForPermission)
                    }) { Text("Allow Notifications") }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onDeleteProfile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete Profile") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !processingPhoto,
                onClick = {
                    onSave(
                        name,
                        username,
                        dateOfBirth,
                        email,
                        phoneNumber,
                        description,
                        profileImage,
                        wantsNotifications
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ----- Profile picture (moved from EntrantImages.kt) -----

/** Entrant images
 * Entrant.profileImage can hold:
 *  - ""                 : no profile picture, so a blank circle is shown
 *  - a content:// URI   : an image picked with the Photo Picker (only readable on this device)
 *  - TEST_PFP_* values  : TEMPORARY hardcoded images for testing
 * TODO: Upload picked images to Firebase Storage and store the download URL instead, so the
 *  picture shows up on other devices too.
 * Image Locations: code\app\src\main\res\drawable
 */

// Constants to test Test Profile Pictures
// TODO: Remove later once images start persisting
const val TEST_PFP_PROFILE = "test:default_profile_image"
const val TEST_PFP_GIFT = "test:default_gift_image"

// TEMPORARY: maps the test values to their drawables (null if it isn't a test value)
// TODO: Remove this later
@DrawableRes
fun testProfileImageRes(profileImage: String): Int? = when (profileImage) {
    TEST_PFP_PROFILE -> R.drawable.default_profile_image
    TEST_PFP_GIFT -> R.drawable.default_gift_image
    else -> null
}

fun deleteEntrantImagesFromFirestore(entrantID: String) {
    if (entrantID.isBlank()) return
    try {
        val db = com.google.firebase.Firebase.firestore
        db.collection("images")
            .whereEqualTo("entrantID", entrantID)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    db.collection("images").document(doc.id).delete()
                }
            }
    } catch (e: Exception) {
        android.util.Log.e("ImageDelete", "Failed deleting images for entrant $entrantID from Firestore", e)
    }
}

fun uploadImageToFirestore(
    context: Context,
    uri: Uri,
    entrantID: String,
    onSuccess: (base64: String) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val db = com.google.firebase.Firebase.firestore
            val docId = java.util.UUID.randomUUID().toString()
            val imageMap = mapOf(
                "entrantID" to entrantID,
                "imageData" to base64String,
                "uri" to uri.toString(),
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("images").document(docId).set(imageMap)
                .addOnSuccessListener {
                    onSuccess(base64String)
                }
                .addOnFailureListener {
                    onSuccess(base64String)
                }
        }
    } catch (e: Exception) {
        android.util.Log.e("ImageUpload", "Error uploading image to Firestore images collection", e)
    }
}

fun decodeBase64ToImageBitmap(base64Str: String): ImageBitmap? = try {
    val cleanBase64 = base64Str.substringAfter("base64,")
    val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
} catch (e: Exception) {
    null
}

fun saveImageRecordToFirestore(
    context: Context,
    entrantID: String,
    username: String,
    email: String,
    profileImage: String
) {
    if (profileImage.isBlank() || entrantID.isBlank()) return
    try {
        val deviceId = DeviceIdentity.getOrCreate(context)
        val db = com.google.firebase.Firebase.firestore
        db.collection("images")
            .whereEqualTo("entrantID", entrantID)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    db.collection("images").document(doc.id).delete()
                }
                val docId = java.util.UUID.randomUUID().toString()
                val imageDoc = mapOf(
                    "entrantID" to entrantID,
                    "deviceIdentity" to deviceId,
                    "username" to username,
                    "email" to email,
                    "profileImage" to profileImage,
                    "imageData" to profileImage,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("images").document(docId).set(imageDoc)
            }
            .addOnFailureListener {
                val docId = java.util.UUID.randomUUID().toString()
                val imageDoc = mapOf(
                    "entrantID" to entrantID,
                    "deviceIdentity" to deviceId,
                    "username" to username,
                    "email" to email,
                    "profileImage" to profileImage,
                    "imageData" to profileImage,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("images").document(docId).set(imageDoc)
            }
    } catch (e: Exception) {
        android.util.Log.e("ImageRecord", "Failed saving image to images collection", e)
    }
}

/** Draws an entrant's profile picture. Draws a blank placeholder when there is no picture
 *  or the picked image can't be read.
 */
@Composable
fun ProfilePicture(profileImage: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val testRes = testProfileImageRes(profileImage)

    // Load picked images off the main thread
    val bitmap = key(context, profileImage) {
        produceState<ImageBitmap?>(initialValue = null) {
            value = if (profileImage.isBlank() || testRes != null) null
            else withContext(Dispatchers.IO) {
                if (profileImage.startsWith("content://") || profileImage.startsWith("file://")) {
                    loadImage(context, Uri.parse(profileImage))
                } else {
                    decodeBase64ToImageBitmap(profileImage)
                }
            }
        }.value
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = "Profile picture",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (testRes != null) {
        // TEMPORARY: hardcoded test image
        Image(
            painter = painterResource(testRes),
            contentDescription = "Profile picture",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Blank placeholder (no picture yet)
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

// Reads an image from a content URI, shrunk so its smaller side is roughly maxSize pixels
internal fun loadImage(context: Context, uri: Uri, maxSize: Int = 512): ImageBitmap? = try {
    val resolver = context.contentResolver
    // First pass: only read the image's size
    // Use Bitmap Object in order to decode it with a circle
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    // Halve the resolution until it's close to maxSize, so big photos don't use too much memory
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSize && bounds.outHeight / (sample * 2) >= maxSize) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
} catch (e: Exception) {
    android.util.Log.e("ProfilePicture", "Couldn't load profile picture $uri", e)
    null
}


/* ---------------------------------------------------------------------------------------------
 * QR CODE SCANNING (entrants only)
 * The entrant picks an image of a campsite's QR code; ZXing reads the text in it, the text is
 * matched to a campsite (by ID, or by name for older codes), and the entrant is taken to that
 * campsite's details.
 * Conversation (All 4 functions underneath: https://claude.ai/share/6448aa90-1673-4617-9624-eac3afb21ef4)
 * ------------------------------------------------------------------------------------------- */

/**
 * For tests only: when set, tapping the square uses this image instead of opening the system
 * photo picker (tests can't tap inside the picker, it's another app). Null in the real app.
 */
val LocalQrTestImage = staticCompositionLocalOf<(() -> Bitmap?)?> { null }

/** Pop-up with a square input field: tap it to choose a QR code image from the phone. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun QrCodeScanDialog(
    appState: AppState,
    onCampsiteFound: (campsiteID: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // What was picked: a Uri from the photo picker, or a Bitmap from LocalQrTestImage
    var picked by remember { mutableStateOf<Any?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var foundCampsite by remember { mutableStateOf<Campsite?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }

    // System photo picker (no storage permission needed)
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) picked = uri }
    val testImage = LocalQrTestImage.current
    val openPicker: () -> Unit = {
        if (testImage != null) {
            picked = testImage()
        } else {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    // Load + decode off the main thread whenever a new image is picked
    LaunchedEffect(picked) {
        val choice = picked ?: return@LaunchedEffect
        scanning = true
        error = null
        foundCampsite = null
        val (bmp, text) = withContext(Dispatchers.Default) {
            val loaded = when (choice) {
                is Uri -> loadScaledBitmap(context, choice)
                is Bitmap -> choice
                else -> null
            }
            loaded to loaded?.let { decodeQrCode(it) }
        }
        bitmap = bmp
        when {
            bmp == null -> error = "Couldn't open that image."
            text == null -> error = "No QR code found in that image."
            else -> {
                foundCampsite = findCampsiteForQr(appState, text)
                if (foundCampsite == null) error = "That QR code isn't for a campsite in this app."
            }
        }
        scanning = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan QR Code") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Square input field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp))
                        .clickable { openPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected QR code",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    } else {
                        Text(
                            if (scanning) "Reading image…" else "Tap to choose a QR code image",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                when {
                    scanning -> Text("Scanning…")
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    foundCampsite != null -> Text("Found: ${foundCampsite!!.getName()}", fontWeight = FontWeight.Bold)
                }
                if (bitmap != null) {
                    TextButton(onClick = { openPicker() }) { Text("Choose a different image") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = foundCampsite != null && !scanning,
                onClick = { foundCampsite?.let { onCampsiteFound(it.campsiteID) } }
            ) { Text("View Campsite") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Loads the picked image, shrunk so its longest side is about [maxSide] px (keeps decoding fast). */
private fun loadScaledBitmap(context: Context, uri: Uri, maxSide: Int = 1024): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}.getOrNull()

/** Reads the text stored in a QR code image with ZXing. Returns null if there's no readable QR code. */
fun decodeQrCode(bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val binary = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
    val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true
    )
    return try {
        MultiFormatReader().decode(binary, hints).text
    } catch (e: NotFoundException) {
        null
    } catch (e: Exception) {
        null
    }
}

/** Matches QR text to a campsite: by campsite ID first, then by exact name. */
@RequiresApi(Build.VERSION_CODES.O)
fun findCampsiteForQr(appState: AppState, qrText: String): Campsite? {
    val text = qrText.trim()
    val campsites = appState.campsites.campsites
    return campsites.firstOrNull { it.campsiteID == text }
        ?: campsites.firstOrNull { it.getName() == text }
}

package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showPolicyDialog = true },
                modifier = Modifier.weight(1f)
            ) { Text("View User Policy") }

            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier.weight(1f)
            ) { Text("Edit User Profile") }
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
                current.profileImage = profileImage
                current.wantsNotifications = wantsNotifications
                appState.entrants.updateEntrant(current)
                refreshTick++
                showEditDialog = false
            },
            onDeleteProfile = {
                val id = current.getUserID()
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
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Keep read access to the image after the app restarts
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                android.util.Log.w("EditProfile", "No persistable permission for $uri", e)
            }
            profileImage = uri.toString()
        }
    }
    val openPhotoPicker = {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

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

                // Profile Picture: "Add" if there's none, otherwise "Update" and "Remove"
                Text("Profile Picture", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfilePicture(
                        profileImage = profileImage,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    if (profileImage.isBlank()) {
                        Button(
                            onClick = openPhotoPicker,
                            modifier = Modifier.weight(1f)
                        ) { Text("Add") }
                    } else {
                        Button(
                            onClick = openPhotoPicker,
                            modifier = Modifier.weight(1f)
                        ) { Text("Update") }
                        OutlinedButton(
                            onClick = { profileImage = "" },
                            modifier = Modifier.weight(1f)
                        ) { Text("Remove") }
                    }
                }
                // TEMPORARY (testing): swap between the two hardcoded images without the picker
                TextButton(
                    onClick = {
                        profileImage =
                            if (profileImage == TEST_PFP_PROFILE) TEST_PFP_GIFT else TEST_PFP_PROFILE
                    }
                ) { Text("Swap test image") }

                // Notifications opt in / out (tapping the text toggles it too)
                Text("Notifications", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { wantsNotifications = !wantsNotifications },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = wantsNotifications,
                        onCheckedChange = { wantsNotifications = it }
                    )
                    Text("Opt in to notifications")
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
                onClick = { onSave(
                    name,
                    username,
                    dateOfBirth,
                    email,
                    phoneNumber,
                    description,
                    profileImage,
                    wantsNotifications
                ) }
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

/** Draws an entrant's profile picture. Draws a blank placeholder when there is no picture
 *  or the picked image can't be read.
 */
@Composable
fun ProfilePicture(profileImage: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val testRes = testProfileImageRes(profileImage)

    // Load picked images off the main thread
    val bitmap by produceState<ImageBitmap?>(initialValue = null, profileImage) {
        value = if (profileImage.isBlank() || testRes != null) null
        else withContext(Dispatchers.IO) { loadImage(context, Uri.parse(profileImage)) }
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
private fun loadImage(context: Context, uri: Uri, maxSize: Int = 512): ImageBitmap? = try {
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
